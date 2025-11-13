package Service;

import java.util.List;
import Dao.GenericDAO;
import Models.Envio;

/**
 * Implementación del servicio de negocio para la entidad Domicilio.
 * Capa intermedia entre la UI y el DAO que aplica validaciones de negocio.
 *
 * Responsabilidades:
 * - Validar que los datos del domicilio sean correctos ANTES de persistir
 * - Aplicar reglas de negocio (RN-023: calle y número obligatorios)
 * - Delegar operaciones de BD al DAO
 * - Transformar excepciones técnicas en errores de negocio comprensibles
 *
 * Patrón: Service Layer con inyección de dependencias
 */
public class EnvioServiceImpl implements GenericService<Envio> {
    /**
     * DAO para acceso a datos de domicilios.
     * Inyectado en el constructor (Dependency Injection).
     * Usa GenericDAO para permitir testing con mocks.
     */
    private final GenericDAO<Envio> envioDAO;

    /**
     * Constructor con inyección de dependencias.
     * Valida que el DAO no sea null (fail-fast).
     *
     * @param envioDAO DAO de domicilios (normalmente DomicilioDAO)
     * @throws IllegalArgumentException si envioDAO es null
     */
    public EnvioServiceImpl(GenericDAO<Envio> envioDAO) {
        if (envioDAO == null) {
            throw new IllegalArgumentException("DomicilioDAO no puede ser null");
        }
        this.envioDAO = envioDAO;
    }

    /**
     * Inserta un nuevo domicilio en la base de datos.
     *
     * Flujo:
     * 1. Valida que calle y número no estén vacíos
     * 2. Delega al DAO para insertar
     * 3. El DAO asigna el ID autogenerado al objeto domicilio
     *
     * @param envio Domicilio a insertar (id será ignorado y regenerado)
     * @throws Exception Si la validación falla o hay error de BD
     */
    @Override
    public void insertar(Envio envio) throws Exception {
        validateEnvio(envio);
        envioDAO.insertar(envio);
    }

    /**
     * Actualiza un domicilio existente en la base de datos.
     *
     * Validaciones:
     * - El domicilio debe tener datos válidos (calle, número)
     * - El ID debe ser > 0 (debe ser un domicilio ya persistido)
     *
     * IMPORTANTE: Si varias personas comparten este domicilio,
     * la actualización los afectará a TODAS (RN-040).
     *
     * @param envio Domicilio con los datos actualizados
     * @throws Exception Si la validación falla o el domicilio no existe
     */
    @Override
    public void actualizar(Envio envio) throws Exception {
        validateEnvio(envio);
        if (envio.getId() <= 0) {
            throw new IllegalArgumentException("El ID del envio debe ser mayor a 0 para actualizar");
        }
        envioDAO.actualizar(envio);
    }

    /**
     * Elimina lógicamente un domicilio (soft delete).
     * Marca el domicilio como eliminado=TRUE sin borrarlo físicamente.
     *
     * ⚠️ ADVERTENCIA: Este método NO verifica si hay personas asociadas.
     * Puede dejar referencias huérfanas en personas.domicilio_id (RN-029).
     *
     * ALTERNATIVA SEGURA: Usar PersonaServiceImpl.eliminarDomicilioDePersona()
     * que actualiza la FK antes de eliminar (opción 10 del menú).
     *
     * @param id ID del domicilio a eliminar
     * @throws Exception Si id <= 0 o no existe el domicilio
     */
    @Override
    public void eliminar(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        envioDAO.eliminar(id);
    }

    /**
     * Obtiene un domicilio por su ID.
     *
     * @param id ID del domicilio a buscar
     * @return Domicilio encontrado, o null si no existe o está eliminado
     * @throws Exception Si id <= 0 o hay error de BD
     */
    @Override
    public Envio getById(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        return envioDAO.getById(id);
    }

    /**
     * Obtiene todos los domicilios activos (eliminado=FALSE).
     *
     * @return Lista de domicilios activos (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    @Override
    public List<Envio> getAll() throws Exception {
        return envioDAO.getAll();
    }

    /**
     * Valida que un domicilio tenga datos correctos.
     *
     * Reglas de negocio aplicadas:
     * - RN-023: Calle y número son obligatorios
     * - RN-024: Se verifica trim() para evitar strings solo con espacios
     *
     * @param envio Domicilio a validar
     * @throws IllegalArgumentException Si alguna validación falla
     */
    private void validateEnvio(Envio envio) {
        if (envio == null) {
            throw new IllegalArgumentException("El envio no puede ser null");
        }
        if (envio.getTracking() == null || envio.getTracking().trim().isEmpty()) {
            throw new IllegalArgumentException("El número no puede estar vacío");
        }
    }
}