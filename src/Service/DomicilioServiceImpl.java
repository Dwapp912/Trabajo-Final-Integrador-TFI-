package Service;

import java.util.List;
import Dao.GenericDAO;
import Models.Domicilio;

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
public class DomicilioServiceImpl implements GenericService<Domicilio> {
    /**
     * DAO para acceso a datos de domicilios.
     * Inyectado en el constructor (Dependency Injection).
     * Usa GenericDAO para permitir testing con mocks.
     */
    private final GenericDAO<Domicilio> domicilioDAO;

    /**
     * Constructor con inyección de dependencias.
     * Valida que el DAO no sea null (fail-fast).
     *
     * @param domicilioDAO DAO de domicilios (normalmente DomicilioDAO)
     * @throws IllegalArgumentException si domicilioDAO es null
     */
    public DomicilioServiceImpl(GenericDAO<Domicilio> domicilioDAO) {
        if (domicilioDAO == null) {
            throw new IllegalArgumentException("DomicilioDAO no puede ser null");
        }
        this.domicilioDAO = domicilioDAO;
    }

    /**
     * Inserta un nuevo domicilio en la base de datos.
     *
     * Flujo:
     * 1. Valida que calle y número no estén vacíos
     * 2. Delega al DAO para insertar
     * 3. El DAO asigna el ID autogenerado al objeto domicilio
     *
     * @param domicilio Domicilio a insertar (id será ignorado y regenerado)
     * @throws Exception Si la validación falla o hay error de BD
     */
    @Override
    public void insertar(Domicilio domicilio) throws Exception {
        validateDomicilio(domicilio);
        domicilioDAO.insertar(domicilio);
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
     * @param domicilio Domicilio con los datos actualizados
     * @throws Exception Si la validación falla o el domicilio no existe
     */
    @Override
    public void actualizar(Domicilio domicilio) throws Exception {
        validateDomicilio(domicilio);
        if (domicilio.getId() <= 0) {
            throw new IllegalArgumentException("El ID del domicilio debe ser mayor a 0 para actualizar");
        }
        domicilioDAO.actualizar(domicilio);
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
        domicilioDAO.eliminar(id);
    }

    /**
     * Obtiene un domicilio por su ID.
     *
     * @param id ID del domicilio a buscar
     * @return Domicilio encontrado, o null si no existe o está eliminado
     * @throws Exception Si id <= 0 o hay error de BD
     */
    @Override
    public Domicilio getById(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        return domicilioDAO.getById(id);
    }

    /**
     * Obtiene todos los domicilios activos (eliminado=FALSE).
     *
     * @return Lista de domicilios activos (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    @Override
    public List<Domicilio> getAll() throws Exception {
        return domicilioDAO.getAll();
    }

    /**
     * Valida que un domicilio tenga datos correctos.
     *
     * Reglas de negocio aplicadas:
     * - RN-023: Calle y número son obligatorios
     * - RN-024: Se verifica trim() para evitar strings solo con espacios
     *
     * @param domicilio Domicilio a validar
     * @throws IllegalArgumentException Si alguna validación falla
     */
    private void validateDomicilio(Domicilio domicilio) {
        if (domicilio == null) {
            throw new IllegalArgumentException("El domicilio no puede ser null");
        }
        if (domicilio.getCalle() == null || domicilio.getCalle().trim().isEmpty()) {
            throw new IllegalArgumentException("La calle no puede estar vacía");
        }
        if (domicilio.getNumero() == null || domicilio.getNumero().trim().isEmpty()) {
            throw new IllegalArgumentException("El número no puede estar vacío");
        }
    }
}