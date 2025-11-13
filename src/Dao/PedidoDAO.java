package Dao;

import Models.Pedido;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import Config.DatabaseConnection;
import Models.Envio;
import java.time.LocalDate;

/**
 * Data Access Object para la entidad Pedido.
 * Gestiona todas las operaciones de persistencia de pedidos en la base de datos.
 *
 * Características:
 * - Implementa GenericDAO<Pedido> para operaciones CRUD estándar
 * - Usa PreparedStatements en TODAS las consultas (protección contra SQL injection)
 * - Maneja LEFT JOIN con envíos para cargar la relación de forma eager
 * - Implementa soft delete (eliminado=TRUE, no DELETE físico)
 * - Proporciona búsquedas especializadas (por número exacto, por nombre de cliente con LIKE)
 * - Soporta transacciones mediante insertTx() (recibe Connection externa)
 *
 * Patrón: DAO con try-with-resources para manejo automático de recursos JDBC
 */
public class PedidoDAO implements GenericDAO<Pedido> {
    /**
     * Query de inserción de pedido.
     * Inserta numero, fecha, clienteNombre, total y estado.
     * El id es AUTO_INCREMENT y se obtiene con RETURN_GENERATED_KEYS.
     */
    private static final String INSERT_SQL = """
        INSERT INTO
            pedido
            (
             numero,
             fecha,
             clienteNombre,
             total,
             estado)
        VALUES
            (?, ?, ?, ?, ?)
        """;

    /**
     * Query de actualización de pedido.
     * Actualiza numero, fecha, clienteNombre, total y estado por id.
     * NO actualiza el flag eliminado (solo se modifica en soft delete).
     */
    private static final String UPDATE_SQL = """
        UPDATE
            pedido
        SET
            numero = ?,
            fecha = ?,
            clienteNombre = ?,
            total = ?,
            estado = ?
        WHERE id = ?
        """;

    /**
     * Query de soft delete.
     * Marca eliminado=TRUE sin borrar físicamente la fila.
     * Preserva integridad referencial y datos históricos.
     */
    private static final String DELETE_SQL = """
            UPDATE
                pedido
            SET
                eliminado = TRUE
            WHERE id = ?
    """;
    
      private static final String DELETE_STATUS_SQL = """
            SELECT
                eliminado
            FROM
                pedido    
            WHERE id = ?
    """;

    /**
     * Query para obtener pedido por ID.
     * LEFT JOIN con envío para cargar la relación de forma eager.
     * Solo retorna pedidos activos (eliminado=FALSE).
     *
     * Columnas relevantes del ResultSet:
     * - Pedido: id, numero, fecha, clienteNombre, estado, total
     * - Envío (puede ser NULL): envio_id, tracking, empresa, tipo, costo, fechaEstimada, fechaDespacho, estado_envio
     */
    private static final String SELECT_BY_ID_SQL = """
        SELECT
            p.eliminado,
            p.id,
            p.numero,
            p.fecha,
            p.clienteNombre,
            p.estado,
            p.total,
            e.id AS envio_id,
            e.tracking,
            e.empresa,
            e.tipo,
            e.costo,
            e.fechaEstimada,
            e.fechaDespacho,
            e.estado as estado_envio
            FROM pedido p LEFT JOIN envio e ON e.pedidoId = p.id 
            WHERE p.id = ? AND p.eliminado = FALSE;
        """;

    /**
     * Query para obtener todos los pedidos activos.
     * LEFT JOIN con envíos para cargar relaciones.
     * Filtra por eliminado=FALSE (solo pedidos activos).
     */
    private static final String SELECT_ALL_SQL =  """
        SELECT
            p.id,
            p.eliminado,
            p.numero,
            p.fecha,
            p.clienteNombre,
            p.estado,
            p.total,
            e.id AS envio_id,
            e.tracking,
            e.empresa,
            e.tipo,
            e.costo,
            e.fechaEstimada,
            e.fechaDespacho,
            e.estado as estado_envio
            FROM pedido p LEFT JOIN envio e ON e.pedidoId = p.id AND p.eliminado = FALSE
            WHERE p.eliminado = FALSE;
        """;

    /**
     * Query de búsqueda por número de pedido con LIKE.
     * Permite búsqueda flexible: el usuario ingresa "100" y encuentra "100", "1001", etc.
     * Usa % antes y después del filtro: LIKE '%filtro%'
     * Solo pedidos activos (eliminado=FALSE).
     */
    private static final String SEARCH_BY_NUMBER_SQL = """
        SELECT
            p.id,
            p.eliminado,
            p.numero,
            p.fecha,
            p.clienteNombre,
            p.estado,
            p.total,
            e.id AS envio_id,
            e.tracking,
            e.empresa,
            e.tipo,
            e.costo,
            e.fechaEstimada,
            e.fechaDespacho,
            e.estado as estado_envio
            FROM pedido p LEFT JOIN envio e ON e.pedidoId = p.id AND p.eliminado = FALSE
            WHERE p.eliminado = FALSE AND (p.numero LIKE ?);
        """;

    /**
     * Query de búsqueda exacta por número de pedido.
     * Usa comparación exacta (=) porque el número es único (RN-001).
     * Usado por PedidosServiceImpl.validateNumeroUnique() para verificar unicidad.
     * Solo pedidos activos (eliminado=FALSE).
     */
    private static final String SEARCH_BY_NOMBRE_CLIENTE = """
        SELECT
            p.id,
            p.eliminado,
            p.numero,
            p.fecha,
            p.clienteNombre,
            p.estado,
            p.total,
            e.id AS envio_id,
            e.tracking,
            e.empresa,
            e.tipo,
            e.costo,
            e.fechaEstimada,
            e.fechaDespacho,
            e.estado as estado_envio
            FROM pedido p LEFT JOIN envio e ON e.pedidoId = p.id AND p.eliminado = FALSE
            WHERE p.eliminado = FALSE AND (p.clienteNombre LIKE ?);
        """;

    /**
     * DAO de envíos (disponible para operaciones coordinadas).
     * Inyectado en el constructor por si se necesita coordinar operaciones.
     */
    private final EnvioDAO envioDAO;

    /**
     * Constructor con inyección de EnvioDAO.
     * Valida que la dependencia no sea null (fail-fast).
     *
     * @param envioDAO DAO de envíos
     * @throws IllegalArgumentException si envioDAO es null
     */
    public PedidoDAO(EnvioDAO envioDAO) {
        if (envioDAO == null) {
            throw new IllegalArgumentException("EnvioDAO no puede ser null");
        }
        this.envioDAO = envioDAO;
    }

    /**
     * Inserta un pedido en la base de datos (versión sin transacción).
     * Crea su propia conexión y la cierra automáticamente.
     *
     * Flujo:
     * 1. Abre conexión con DatabaseConnection.getConnection()
     * 2. Crea PreparedStatement con INSERT_SQL y RETURN_GENERATED_KEYS
     * 3. Setea parámetros del pedido (número, fecha, clienteNombre, total, estado)
     * 4. Ejecuta INSERT
     * 5. Obtiene el ID autogenerado y lo asigna a pedido.id
     * 6. Cierra recursos automáticamente (try-with-resources)
     *
     * @param pedido Pedido a insertar (id será ignorado y regenerado)
     * @throws Exception Si falla la inserción o no se obtiene ID generado
     */
    @Override
    public void insertar(Pedido pedido) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            setearParametrosPedido(stmt, pedido);
            stmt.executeUpdate();
            setGeneratedId(stmt, pedido);
        }
    }

    /**
     * Inserta un pedido dentro de una transacción existente.
     * NO crea nueva conexión, recibe una Connection externa.
     * NO cierra la conexión (responsabilidad del caller con TransactionManager).
     *
     * Usado por: (Actualmente no usado, pero disponible para transacciones futuras)
     * - Operaciones que requieren múltiples inserts coordinados
     * - Rollback automático si alguna operación falla
     *
     * @param pedido Pedido a insertar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @throws Exception Si falla la inserción
     */
    @Override
    public void insertTx(Pedido pedido, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            setearParametrosPedido(stmt, pedido);
            stmt.executeUpdate();
            setGeneratedId(stmt, pedido);
        }
    }

    /**
     * Actualiza una persona existente en la base de datos.
     * Actualiza nombre, apellido, dni y FK domicilio_id.
     *
     * Validaciones:
     * - Si rowsAffected == 0 → La persona no existe o ya está eliminada
     *
     * IMPORTANTE: Este método puede cambiar la FK domicilio_id:
     * - Si persona.domicilio == null → domicilio_id = NULL (desasociar)
     * - Si persona.domicilio.id > 0 → domicilio_id = domicilio.id (asociar/cambiar)
     *
     * @param pedido Persona con los datos actualizados (id debe ser > 0)
     * @throws SQLException Si la persona no existe o hay error de BD
     */
    @Override
    public void actualizar(Pedido pedido) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            setearParametrosPedido(stmt, pedido);
            stmt.setInt(6, pedido.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el pedido con ID: " + pedido.getId());
            }
        }
    }

    /**
     * Elimina lógicamente una persona (soft delete).
     * Marca eliminado=TRUE sin borrar físicamente la fila.
     *
     * Validaciones:
     * - Si rowsAffected == 0 → La persona no existe o ya está eliminada
     *
     * IMPORTANTE: NO elimina el domicilio asociado (correcto según RN-037).
     * Múltiples personas pueden compartir un domicilio.
     *
     * @param id ID de la persona a eliminar
     * @throws SQLException Si la persona no existe o hay error de BD
     */
    @Override
    public void eliminar(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL); 
             PreparedStatement stmt2 = conn.prepareStatement(DELETE_STATUS_SQL)) {

            stmt.setInt(1, id);
            stmt2.setInt(1, id);

            try (ResultSet rs = stmt2.executeQuery()) {
                if (rs.next()) {
                    if (rs.getBoolean("eliminado") == true) {
                        System.out.println("El pedido ya ha sido eliminado anteriormente");
                    } else {
                        int rowsAffected = stmt.executeUpdate();

                        if (rowsAffected == 0) {
                            throw new SQLException("No se encontró pedido con ID: " + id);
                        }
                        else{
                            System.out.println("Pedido eliminado exitosamente.");
                        }
                    }
                }
            } catch (SQLException e) {
                throw new Exception("Error al eliminar pedido: " + e.getMessage(), e);
            }

        }
    }
    

    /**
     * Obtiene una persona por su ID.
     * Incluye su domicilio asociado mediante LEFT JOIN.
     *
     * @param id ID de la persona a buscar
     * @return Persona encontrada con su domicilio, o null si no existe o está eliminada
     * @throws Exception Si hay error de BD (captura SQLException y re-lanza con mensaje descriptivo)
     */
    @Override
    public Pedido getById(int id) throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPedido(rs);
                   
                }
            }
        } catch (SQLException e) {
            throw new Exception("Error al obtener pedido por ID: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Obtiene todos los pedidos activos (eliminado=FALSE).
     * Incluye su envío mediante LEFT JOIN (si existe).
     *
     * Nota: Usa Statement (no PreparedStatement) porque no hay parámetros.
     *
     * @return Lista de pedidos activos con su envío (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    @Override
    public List<Pedido> getAll() throws Exception {
        List<Pedido> pedidos = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_SQL)) {

            while (rs.next()) {
                pedidos.add(mapResultSetToPedido(rs));
            }
        } catch (SQLException e) {
            throw new Exception("Error al obtener todos los pedidos: " + e.getMessage(), e);
        }
        return pedidos;
    }

    /**
     * Busca pedidos por nombre de cliente con búsqueda flexible (LIKE).
     * Permite búsqueda parcial: "ana" encuentra "Ana", "Anastasia", "Mariana", etc.
     *
     * Patrón de búsqueda: LIKE '%filtro%' en clienteNombre
     * Sensibilidad a mayúsculas: depende de la collation de la BD.
     *
     * @param filtro Texto a buscar (no puede estar vacío)
     * @return Lista de pedidos que coinciden con el filtro (puede estar vacía)
     * @throws IllegalArgumentException Si el filtro está vacío
     * @throws SQLException Si hay error de BD
     */
    public List<Pedido> buscarPorNombreCliente(String filtro) throws SQLException {
        if (filtro == null || filtro.trim().isEmpty()) {
            throw new IllegalArgumentException("El filtro de búsqueda no puede estar vacío");
        }

        List<Pedido> pedidos = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_NOMBRE_CLIENTE)) {

            // Construye el patrón LIKE: %filtro%
            String searchPattern = "%" + filtro + "%";
            stmt.setString(1, searchPattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    pedidos.add(mapResultSetToPedido(rs));
                }
            }
        }
        return pedidos;
    }

    /**
     * Busca un pedido por número exacto.
     * Usa comparación exacta (=) porque el número de pedido es único (RN-001).
     *
     * Uso típico:
     * - Validar unicidad del número de pedido (PedidosServiceImpl.validateNumeroUnique)
     * - Buscar un pedido específico desde el menú
     *
     * @param numeroPedido Número de pedido exacto a buscar (se aplica trim automáticamente)
     * @return Pedido con ese número, o null si no existe o está eliminado
     * @throws IllegalArgumentException Si el número de pedido está vacío
     * @throws SQLException Si hay error de BD
     */
    public Pedido buscarPorNumeroDePedido(String numeroPedido) throws SQLException {
        if (numeroPedido == null || numeroPedido.trim().isEmpty()) {
            throw new IllegalArgumentException("El número de pedido no puede estar vacío");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SEARCH_BY_NUMBER_SQL)) {

            stmt.setString(1, numeroPedido.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPedido(rs);
                }
            }
        }
        return null;
    }

    /**
     * Setea los parámetros del pedido en un PreparedStatement.
     * Método auxiliar usado por insertar() e insertTx().
     *
     * Parámetros seteados (en orden):
     * 1. numero (String)
     * 2. fecha (Date)
     * 3. clienteNombre (String)
     * 4. total (Double)
     * 5. estado (Enum en String)
     *
     * @param stmt PreparedStatement con INSERT_SQL
     * @param pedido Pedido con los datos a insertar
     * @throws SQLException Si hay error al setear parámetros
     */
    private void setearParametrosPedido(PreparedStatement stmt, Pedido pedido) throws SQLException {
        stmt.setString(1, pedido.getNumero());
        stmt.setDate(2, java.sql.Date.valueOf(pedido.getFecha()));
        stmt.setString(3, pedido.getClienteNombre());
        stmt.setDouble(4, pedido.getTotal());
        stmt.setString(5, pedido.getEstado().toString());
    }


    /**
     * Obtiene el ID autogenerado por la BD después de un INSERT.
     * Asigna el ID generado al objeto pedido.
     *
     * IMPORTANTE: Este método es crítico para mantener la consistencia:
     * - Después de insertar, el objeto pedido debe tener su ID real de la BD
     * - Permite usar pedido.getId() inmediatamente después de insertar
     * - Necesario para operaciones transaccionales que requieren el ID generado
     *
     * @param stmt PreparedStatement que ejecutó el INSERT con RETURN_GENERATED_KEYS
     * @param pedido Objeto pedido a actualizar con el ID generado
     * @throws SQLException Si no se pudo obtener el ID generado (indica problema grave)
     */
    private void setGeneratedId(PreparedStatement stmt, Pedido pedido) throws SQLException {
        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                pedido.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("La inserción del pedido falló, no se obtuvo ID generado");
            }
        }
    }

    /**
     * Mapea un ResultSet a un objeto Pedido.
     * Reconstruye la relación con Envío usando LEFT JOIN.
     *
     * Mapeo de columnas:
     * Pedido:
     * - id → p.id
     * - numero → p.numero
     * - fecha → p.fecha
     * - clienteNombre → p.clienteNombre
     * - total → p.total
     * - estado → p.estado
     *
     * Envío (puede ser NULL si el pedido no tiene envío):
     * - id → e.id AS envio_id
     * - tracking → e.tracking
     * - empresa → e.empresa
     * - tipo → e.tipo
     * - costo → e.costo
     * - fechaEstimada → e.fechaEstimada
     * - fechaDespacho → e.fechaDespacho
     * - estado → e.estado as estado_envio
     *
     * Lógica de NULL en LEFT JOIN:
     * - Si envio_id es NULL → pedido.envio = null (correcto)
     * - Si envio_id > 0 → Se obtiene el Envío por ID y se asigna al pedido
     *
     * @param rs ResultSet posicionado en una fila con datos de pedido y envío
     * @return Pedido reconstruido con su envío (si tiene)
     * @throws SQLException Si hay error al leer columnas del ResultSet
     */
    private Pedido mapResultSetToPedido(ResultSet rs) throws SQLException {
        // Manejo correcto de LEFT JOIN: verificar si envio_id es NULL
        int envioId = rs.getInt("envio_id");
        Envio envio = null;
        if (envioId > 0 && !rs.wasNull()) {
            envio = envioDAO.getById(envioId);
        }
        Pedido.Estado estado = Pedido.Estado.valueOf(rs.getString("estado"));
        Date fecha = rs.getDate("fecha");
        LocalDate pedidoFecha = fecha.toLocalDate();
        Pedido pedido = new Pedido(
                rs.getInt("id"),
                rs.getBoolean("eliminado"),
                rs.getString("numero"),
                pedidoFecha,
                rs.getString("clienteNombre"),
                rs.getDouble("total"),
                estado,
                envio);



        return pedido;
    }
}