package Service;

import java.util.List;
import Dao.GenericDAO;
import Models.Envio;

/**
 * Implementación del servicio de negocio para la entidad Envío.
 * Capa intermedia entre la UI y el DAO que aplica validaciones de negocio.
 *
 * Responsabilidades:
 * - Validar que los datos del envío sean correctos ANTES de persistir
 * - Aplicar reglas de negocio (por ejemplo: tracking obligatorio)
 * - Delegar operaciones de BD al DAO
 * - Transformar excepciones técnicas en errores de negocio comprensibles
 *
 * Patrón: Service Layer con inyección de dependencias
 */
public class EnvioServiceImpl implements GenericService<Envio> {
    /**
     * DAO para acceso a datos de envíos.
     * Inyectado en el constructor (Dependency Injection).
     * Usa GenericDAO para permitir testing con mocks.
     */
    private final GenericDAO<Envio> envioDAO;

    /**
     * Constructor con inyección de dependencias.
     * Valida que el DAO no sea null (fail-fast).
     *
     * @param envioDAO DAO de envíos
     * @throws IllegalArgumentException si envioDAO es null
     */
    public EnvioServiceImpl(GenericDAO<Envio> envioDAO) {
        if (envioDAO == null) {
            throw new IllegalArgumentException("EnvioDAO no puede ser null");
        }
        this.envioDAO = envioDAO;
    }

    /**
     * Inserta un nuevo envío en la base de datos.
     *
     * Flujo:
     * 1. Valida que el tracking no esté vacío
     * 2. Delega al DAO para insertar
     * 3. El DAO asigna el ID autogenerado al objeto envío
     *
     * @param envio Envío a insertar (id será ignorado y regenerado)
     * @throws Exception Si la validación falla o hay error de BD
     */
    @Override
    public void insertar(Envio envio) throws Exception {
        validateEnvio(envio);
        envioDAO.insertar(envio);
    }

    /**
     * Actualiza un envío existente en la base de datos.
     *
     * Validaciones:
     * - El envío debe tener datos válidos (tracking)
     * - El ID debe ser > 0 (debe ser un envío ya persistido)
     *
     * IMPORTANTE: Si varios pedidos compartieran este envío,
     * la actualización los afectaría a TODOS (RN-040).
     *
     * @param envio Envío con los datos actualizados
     * @throws Exception Si la validación falla o el envío no existe
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
     * Elimina lógicamente un envío (soft delete).
     * Marca el envío como eliminado=TRUE sin borrarlo físicamente.
     *
     * Advertencia: Este método NO verifica si hay pedidos asociados.
     * Puede dejar referencias a envíos marcados como eliminados en pedidos.
     *
     * Alternativa sugerida: desasociar el envío desde el servicio de pedidos
     * antes de eliminarlo para evitar referencias inconsistentes.
     *
     * @param id ID del envío a eliminar
     * @throws Exception Si id <= 0 o no existe el envío
     */
    @Override
    public void eliminar(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        envioDAO.eliminar(id);
    }

    /**
     * Obtiene un envío por su ID.
     *
     * @param id ID del envío a buscar
     * @return Envío encontrado, o null si no existe o está eliminado
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
     * Obtiene todos los envíos activos (eliminado=FALSE).
     *
     * @return Lista de envíos activos (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    @Override
    public List<Envio> getAll() throws Exception {
        return envioDAO.getAll();
    }

    /**
     * Valida que un envío tenga datos correctos.
     *
     * Reglas de negocio aplicadas (simplificadas):
     * - Tracking obligatorio (no vacío)
     * - Se verifica trim() para evitar strings solo con espacios
     *
     * @param envio Envío a validar
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