package Dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import Config.DatabaseConnection;
import Models.Envio;
import java.time.LocalDate;

/**
 * Data Access Object para la entidad Envio.
 * Gestiona todas las operaciones de persistencia de domicilios en la base de datos.
 *
 * Características:
 * - Implementa GenericDAO<Envio> para operaciones CRUD estándar
 * - Usa PreparedStatements en TODAS las consultas (protección contra SQL injection)
 * - Implementa soft delete (eliminado=TRUE, no DELETE físico)
 * - NO maneja relaciones (Envio es entidad independiente)
 * - Soporta transacciones mediante insertTx() (recibe Connection externa)
 *
 * Diferencias con EnvioDAO:
 * - Más simple: NO tiene LEFT JOINs (Domicilio no tiene relaciones cargadas)
 * - NO tiene búsquedas especializadas (solo CRUD básico)
 * - Todas las queries filtran por eliminado=FALSE (soft delete)
 *
 * Patrón: DAO con try-with-resources para manejo automático de recursos JDBC
 */
public class EnvioDAO implements GenericDAO<Envio> {
    /**
     * Query de inserción de domicilio.
     * Inserta calle y número.
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
     * Query de actualización de domicilio.
     * Actualiza calle y número por id.
     * NO actualiza el flag eliminado (solo se modifica en soft delete).
     *
     * ⚠️ IMPORTANTE: Si varias personas comparten este domicilio,
     * la actualización los afectará a TODAS (RN-040).
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
     * ⚠️ PELIGRO: Este método NO verifica si hay personas asociadas.
     * Puede dejar FKs huérfanas (personas.domicilio_id apuntando a domicilio eliminado).
     * ALTERNATIVA SEGURA: PersonaServiceImpl.eliminarDomicilioDePersona()
     */
    private static final String DELETE_SQL = """
        UPDATE
            envio
        SET eliminado = TRUE
        WHERE id = ?
    """;

    /**
     * Query para obtener domicilio por ID.
     * Solo retorna domicilios activos (eliminado=FALSE).
     * SELECT * es aceptable aquí porque Domicilio tiene solo 4 columnas.
     */
    private static final String SELECT_BY_ID_SQL = """
        SELECT
            *
        FROM
            envio 
        WHERE id = ? AND eliminado = FALSE
        """;

    /**
     * Query para obtener todos los domicilios activos.
     * Filtra por eliminado=FALSE (solo domicilios activos).
     * SELECT * es aceptable aquí porque Domicilio tiene solo 4 columnas.
     */
    private static final String SELECT_ALL_SQL = """
        SELECT
            *
        FROM
            envio
        WHERE eliminado = FALSE
        """;

    /**
     * Inserta un domicilio en la base de datos (versión sin transacción).
     * Crea su propia conexión y la cierra automáticamente.
     *
     * Flujo:
     * 1. Abre conexión con DatabaseConnection.getConnection()
     * 2. Crea PreparedStatement con INSERT_SQL y RETURN_GENERATED_KEYS
     * 3. Setea parámetros (calle, numero)
     * 4. Ejecuta INSERT
     * 5. Obtiene el ID autogenerado y lo asigna a domicilio.id
     * 6. Cierra recursos automáticamente (try-with-resources)
     *
     * IMPORTANTE: El ID generado se asigna al objeto domicilio.
     * Esto permite que PersonaServiceImpl.insertar() use domicilio.getId()
     * inmediatamente después de insertar.
     *
     * @param envio Domicilio a insertar (id será ignorado y regenerado)
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
     * Inserta un domicilio dentro de una transacción existente.
     * NO crea nueva conexión, recibe una Connection externa.
     * NO cierra la conexión (responsabilidad del caller con TransactionManager).
     *
     * Usado por: (Actualmente no usado, pero disponible para transacciones futuras)
     * - Operaciones que requieren múltiples inserts coordinados
     * - Rollback automático si alguna operación falla
     *
     * @param envio Domicilio a insertar
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
     * Actualiza un domicilio existente en la base de datos.
     * Actualiza calle y número.
     *
     * Validaciones:
     * - Si rowsAffected == 0 → El domicilio no existe o ya está eliminado
     *
     * ⚠️ IMPORTANTE: Si varias personas comparten este domicilio,
     * la actualización los afectará a TODAS (RN-040).
     * Ejemplo:
     * - Domicilio ID=1 "Av. Siempreviva 742" tiene 3 personas asociadas
     * - actualizar(domicilio con calle="Calle Nueva") cambia la dirección de las 3 personas
     *
     * Esto es CORRECTO: permite que familias compartan la misma dirección
     * y se actualice en un solo lugar.
     *
     * @param envio Domicilio con los datos actualizados (id debe ser > 0)
     * @throws SQLException Si el domicilio no existe o hay error de BD
     */
    @Override
    public void actualizar(Envio envio) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {
            setearParametrosEnvio(stmt, envio);
            stmt.setInt(7, envio.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("No se pudo actualizar el domicilio con ID: " + envio.getId());
            }
        }
    }

    /**
     * Elimina lógicamente un domicilio (soft delete).
     * Marca eliminado=TRUE sin borrar físicamente la fila.
     *
     * Validaciones:
     * - Si rowsAffected == 0 → El domicilio no existe o ya está eliminado
     *
     * ⚠️ PELIGRO: Este método NO verifica si hay personas asociadas (RN-029).
     * Si hay personas con personas.domicilio_id apuntando a este domicilio,
     * quedarán con FK huérfana (apuntando a un domicilio eliminado).
     *
     * Esto puede causar:
     * - Datos inconsistentes (persona asociada a domicilio "eliminado")
     * - Errores en LEFT JOINs que esperan domicilios activos
     *
     * ALTERNATIVA SEGURA: PersonaServiceImpl.eliminarDomicilioDePersona()
     * - Primero actualiza persona.domicilio_id = NULL
     * - Luego elimina el domicilio
     * - Garantiza que no queden FKs huérfanas
     *
     * Este método se mantiene para casos donde:
     * - Se está seguro de que el domicilio NO tiene personas asociadas
     * - Se quiere eliminar domicilios en lote (administración)
     *
     * @param id ID del domicilio a eliminar
     * @throws SQLException Si el domicilio no existe o hay error de BD
     */
    @Override
    public void eliminar(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("No se encontró domicilio con ID: " + id);
            }
        }
    }

    /**
     * Obtiene un domicilio por su ID.
     * Solo retorna domicilios activos (eliminado=FALSE).
     *
     * @param id ID del domicilio a buscar
     * @return Domicilio encontrado, o null si no existe o está eliminado
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
     * Obtiene todos los domicilios activos (eliminado=FALSE).
     *
     * Nota: Usa Statement (no PreparedStatement) porque no hay parámetros.
     *
     * Uso típico:
     * - MenuHandler opción 7: Listar domicilios existentes para asignar a persona
     *
     * @return Lista de domicilios activos (puede estar vacía)
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
     * Setea los parámetros de domicilio en un PreparedStatement.
     * Método auxiliar usado por insertar() e insertTx().
     * <p>
     * Parámetros seteados:
     * 1. calle (String)
     * 2. numero (String)
     *
     * @param stmt  PreparedStatement con INSERT_SQL
     * @param envio Domicilio con los datos a insertar
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
     * Asigna el ID generado al objeto domicilio.
     *
     * IMPORTANTE: Este método es crítico para mantener la consistencia:
     * - Después de insertar, el objeto domicilio debe tener su ID real de la BD
     * - PersonaServiceImpl.insertar() depende de esto para setear la FK:
     *   1. domicilioService.insertar(domicilio) → domicilio.id se setea aquí
     *   2. personaDAO.insertar(persona) → usa persona.getDomicilio().getId() para la FK
     * - Necesario para operaciones transaccionales que requieren el ID generado
     *
     * @param stmt PreparedStatement que ejecutó el INSERT con RETURN_GENERATED_KEYS
     * @param envio Objeto domicilio a actualizar con el ID generado
     * @throws SQLException Si no se pudo obtener el ID generado (indica problema grave)
     */
    private void setGeneratedId(PreparedStatement stmt, Envio envio) throws SQLException {
        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                envio.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("La inserción del domicilio falló, no se obtuvo ID generado");
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