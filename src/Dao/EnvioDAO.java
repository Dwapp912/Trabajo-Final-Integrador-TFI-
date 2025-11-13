package Dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import Config.DatabaseConnection;
import Models.Envio;
import java.time.LocalDate;

/**
 * Data Access Object para la entidad Envío.
 * Gestiona todas las operaciones de persistencia de envíos en la base de datos.
 *
 * Características:
 * - Implementa GenericDAO<Envio> para operaciones CRUD estándar
 * - Usa PreparedStatements en TODAS las consultas (protección contra SQL injection)
 * - Implementa soft delete (eliminado=TRUE, no DELETE físico)
 * - NO maneja relaciones (Envio es entidad independiente)
 * - Soporta transacciones mediante insertTx() (recibe Connection externa)
 *
 * Diferencias con otros DAO:
 * - Más simple: NO tiene LEFT JOINs (Envío no tiene relaciones cargadas aquí)
 * - NO tiene búsquedas especializadas (solo CRUD básico)
 * - Todas las queries filtran por eliminado=FALSE (soft delete)
 *
 * Patrón: DAO con try-with-resources para manejo automático de recursos JDBC
 */
public class EnvioDAO implements GenericDAO<Envio> {
    /**
     * Query de inserción de envío.
     * Inserta tracking, empresa, tipo, estado, costo, fechas y FK pedidoId.
     * El id es AUTO_INCREMENT y se obtiene con RETURN_GENERATED_KEYS.
     * El campo eliminado tiene DEFAULT FALSE en la BD.
     */
    private static final String INSERT_SQL = """
        INSERT INTO envio
            (
             tracking,
             costo,
             fechaDespacho,
             fechaEstimada,
             tipo,
             empresa,
             estado,
             pedidoId
             )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

    /**
     * Query de actualización de envío.
     * Actualiza los campos principales por id (tracking, empresa, tipo, estado, costo, fechas).
     * NO actualiza el flag eliminado (solo se modifica en soft delete).
     *
     * Nota: si hubiera varios pedidos vinculados a un mismo envío, la actualización impactará a todos.
     */
    
    
     private static final String UPDATE_SQL = """
        UPDATE
            envio
        SET
           tracking = ?,
           empresa = ?,  
           tipo = ?, 
           estado = ? , 
           costo = ?,
           fechaDespacho = ?                                  
                                                                                     
        WHERE id = ?
    """;
       
      /**
              **/
    
    /**
     * Query de soft delete.
     * Marca eliminado=TRUE sin borrar físicamente la fila.
     * Preserva integridad referencial y datos históricos.
     *
     * Nota: Este método no verifica si hay pedidos asociados al envío.
     * Considere desasociar el envío de los pedidos antes de eliminarlo desde el servicio correspondiente.
     */
    private static final String DELETE_SQL = """
        UPDATE
            envio
        SET eliminado = TRUE
        WHERE id = ?
    """;

    /**
     * Query para obtener envío por ID.
     * Solo retorna envíos activos (eliminado=FALSE).
     */
    private static final String SELECT_BY_ID_SQL = """
        SELECT
            *
        FROM
            envio 
        WHERE id = ? AND eliminado = FALSE
        """;

    /**
     * Query para obtener todos los envíos activos.
     * Filtra por eliminado=FALSE (solo envíos activos).
     */
    private static final String SELECT_ALL_SQL = """
        SELECT
            *
        FROM
            envio
        WHERE eliminado = FALSE
        """;

    /**
     * Inserta un envío en la base de datos (versión sin transacción).
     * Crea su propia conexión y la cierra automáticamente.
     *
     * Flujo:
     * 1. Abre conexión con DatabaseConnection.getConnection()
     * 2. Crea PreparedStatement con INSERT_SQL y RETURN_GENERATED_KEYS
     * 3. Setea parámetros del envío (tracking, empresa, tipo, estado, costo, fechas)
     * 4. Ejecuta INSERT
     * 5. Obtiene el ID autogenerado y lo asigna a envio.id
     * 6. Cierra recursos automáticamente (try-with-resources)
     *
     * IMPORTANTE: El ID generado se asigna al objeto envío.
     * Esto permite asociarlo inmediatamente a un Pedido si corresponde.
     *
     * @param envio Envío a insertar (id será ignorado y regenerado)
     * @throws SQLException Si falla la inserción o no se obtiene ID generado
     */
    @Override
    public void insertar(Envio envio) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            setearParametrosEnvio(stmt, envio);
            stmt.executeUpdate();
            setGeneratedId(stmt, envio);
        }
    }

    /**
     * Inserta un envío dentro de una transacción existente.
     * NO crea nueva conexión, recibe una Connection externa.
     * NO cierra la conexión (responsabilidad del caller con TransactionManager).
     *
     * Usado por: (Actualmente no usado, pero disponible para transacciones futuras)
     * - Operaciones que requieren múltiples inserts coordinados
     * - Rollback automático si alguna operación falla
     *
     * @param envio Envío a insertar
     * @param conn Conexión transaccional (NO se cierra en este método)
     * @throws Exception Si falla la inserción
     */
    @Override
    public void insertTx(Envio envio, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            setearParametrosEnvio(stmt, envio);
            stmt.executeUpdate();
            setGeneratedId(stmt, envio);
        }
    }

    /**
     * Actualiza un envío existente en la base de datos.
     * Actualiza sus campos principales.
     *
     * Validaciones:
     * - Si rowsAffected == 0 → El envío no existe o ya está eliminado
     *
     * Nota: Si existieran varios pedidos vinculados a este envío,
     * la actualización los afectará a todos.
     *
     * @param envio Envío con los datos actualizados (id debe ser > 0)
     * @throws SQLException Si el envío no existe o hay error de BD
     */
    @Override
    public void actualizar(Envio envio) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {
            setearParametrosEnvio(stmt, envio);
            stmt.setInt(7, envio.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el envío con ID: " + envio.getId());
            }
        }
    }

    /**
     * Elimina lógicamente un envío (soft delete).
     * Marca eliminado=TRUE sin borrar físicamente la fila.
     *
     * Validaciones:
     * - Si rowsAffected == 0 → El envío no existe o ya está eliminado
     *
     * ⚠️ PELIGRO: Este método NO verifica si hay pedidos asociados (RN-029).
     * Si hay pedidos con envío asociado a este registro,
     * quedarán con referencia a un envío marcado como eliminado.
     *
     * Esto puede causar:
     * - Datos inconsistentes (pedido asociado a envío "eliminado")
     *
     * Alternativa recomendada: gestionar la desasociación del envío desde el servicio de pedidos
     * antes de eliminarlo para evitar referencias inconsistentes.
     *
     * Este método se mantiene para casos donde:
     * - Se está seguro de que el envío NO tiene pedidos asociados
     * - Se quiere eliminar envíos en lote (administración)
     *
     * @param id ID del envío a eliminar
     * @throws SQLException Si el envío no existe o hay error de BD
     */
    @Override
    public void eliminar(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("No se encontró envío con ID: " + id);
            }
        }
    }

    /**
     * Obtiene un envío por su ID.
     * Solo retorna envíos activos (eliminado=FALSE).
     *
     * @param id ID del envío a buscar
     * @return Envío encontrado, o null si no existe o está eliminado
     * @throws SQLException Si hay error de BD
     */
    @Override
    public Envio getById(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEnvio(rs);
                }
            }
        }
        return null;
    }

    /**
     * Obtiene todos los envíos activos (eliminado=FALSE).
     *
     * Nota: Usa Statement (no PreparedStatement) porque no hay parámetros.
     *
     * Uso típico:
     * - MenuHandler opción 6: Listar envíos existentes
     *
     * @return Lista de envíos activos (puede estar vacía)
     * @throws SQLException Si hay error de BD
     */
    @Override
    public List<Envio> getAll() throws SQLException {
        List<Envio> envios = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_SQL)) {

            while (rs.next()) {
                envios.add(mapResultSetToEnvio(rs));
            }
        }

        return envios;
    }

    /**
     * Setea los parámetros del envío en un PreparedStatement.
     * Método auxiliar usado por insertar() e insertTx().
     *
     * Parámetros seteados (en orden):
     * 1. tracking (String)
     * 2. empresa (String - enum name)
     * 3. tipo (String - enum name)
     * 4. estado (String - enum name)
     * 5. costo (Double)
     * 6. fechaDespacho (Date)
     *
     * @param stmt  PreparedStatement con INSERT_SQL
     * @param envio Envío con los datos a insertar
     * @throws SQLException Si hay error al setear parámetros
     */
    private void setearParametrosEnvio(PreparedStatement stmt, Envio envio) throws SQLException {
        stmt.setString(1, envio.getTracking());
        stmt.setDouble(5, envio.getCosto());
        stmt.setDate(6, java.sql.Date.valueOf(envio.getFechaDespacho()));        
        stmt.setString(3, envio.getTipo().toString());
        stmt.setString(2, envio.getEmpresa().toString());
        stmt.setString(4, envio.getEstado().toString());
       

    }

    /**
     * Obtiene el ID autogenerado por la BD después de un INSERT.
     * Asigna el ID generado al objeto envío.
     *
     * IMPORTANTE: Este método es crítico para mantener la consistencia:
     * - Después de insertar, el objeto envío debe tener su ID real de la BD
     * - Permite usar envio.getId() inmediatamente después de insertar
     * - Necesario para operaciones transaccionales que requieren el ID generado
     *
     * @param stmt PreparedStatement que ejecutó el INSERT con RETURN_GENERATED_KEYS
     * @param envio Objeto envío a actualizar con el ID generado
     * @throws SQLException Si no se pudo obtener el ID generado (indica problema grave)
     */
    private void setGeneratedId(PreparedStatement stmt, Envio envio) throws SQLException {
        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                envio.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("La inserción del envío falló, no se obtuvo ID generado");
            }
        }
    }

    /**
     * Mapea un ResultSet a un objeto Envio.
     * Reconstruye el objeto usando el constructor completo.
     *
     * @param rs ResultSet posicionado en una fila con datos de envio
     * @return Envio reconstruido
     * @throws SQLException Si hay error al leer columnas del ResultSet
     */
    private Envio mapResultSetToEnvio(ResultSet rs) throws SQLException {
        Date fecha = rs.getDate("fechaDespacho");
        LocalDate fechaDespacho = fecha.toLocalDate();
         Date fechaE = rs.getDate("fechaEstimada");
        LocalDate fechaEstimada = fechaE.toLocalDate();
        return new Envio(
            rs.getInt("id"),
            rs.getBoolean("eliminado"),
            rs.getString("tracking"),
            Envio.Empresa.valueOf(rs.getString("empresa")),
            Envio.Tipo.valueOf(rs.getString("tipo")),
            rs.getDouble("costo"),
            fechaDespacho,
            fechaEstimada,
            Envio.Estado.valueOf(rs.getString("estado")),
            null
        );
    }
}