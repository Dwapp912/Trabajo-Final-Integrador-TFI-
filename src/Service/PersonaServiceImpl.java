package Service;

import Models.Pedido;

import java.util.List;
import Dao.PersonaDAO;

/**
 * Implementación del servicio de negocio para la entidad Persona.
 * Capa intermedia entre la UI y el DAO que aplica validaciones de negocio complejas.
 *
 * Responsabilidades:
 * - Validar datos de persona ANTES de persistir (RN-035: nombre, apellido, DNI obligatorios)
 * - Garantizar unicidad del DNI en el sistema (RN-001)
 * - COORDINAR operaciones entre Persona y Domicilio (transaccionales)
 * - Proporcionar métodos de búsqueda especializados (por DNI, nombre/apellido)
 * - Implementar eliminación SEGURA de domicilios (evita FKs huérfanas)
 *
 * Patrón: Service Layer con inyección de dependencias y coordinación de servicios
 */
public class PersonaServiceImpl implements GenericService<Pedido> {
    /**
     * DAO para acceso a datos de personas.
     * Inyectado en el constructor (Dependency Injection).
     */
    private final PersonaDAO personaDAO;

    /**
     * Servicio de domicilios para coordinar operaciones transaccionales.
     * IMPORTANTE: PersonaServiceImpl necesita DomicilioService porque:
     * - Una persona puede crear/actualizar su domicilio al insertarse/actualizarse
     * - El servicio coordina la secuencia: insertar domicilio → insertar persona
     * - Implementa eliminación segura: actualizar FK persona → eliminar domicilio
     */
    private final DomicilioServiceImpl domicilioServiceImpl;

    /**
     * Constructor con inyección de dependencias.
     * Valida que ambas dependencias no sean null (fail-fast).
     *
     * @param personaDAO DAO de personas (normalmente PersonaDAO)
     * @param domicilioServiceImpl Servicio de domicilios para operaciones coordinadas
     * @throws IllegalArgumentException si alguna dependencia es null
     */
    public PersonaServiceImpl(PersonaDAO personaDAO, DomicilioServiceImpl domicilioServiceImpl) {
        if (personaDAO == null) {
            throw new IllegalArgumentException("PersonaDAO no puede ser null");
        }
        if (domicilioServiceImpl == null) {
            throw new IllegalArgumentException("DomicilioServiceImpl no puede ser null");
        }
        this.personaDAO = personaDAO;
        this.domicilioServiceImpl = domicilioServiceImpl;
    }

    /**
     * Inserta una nueva persona en la base de datos.
     *
     * Flujo transaccional complejo:
     * 1. Valida que los datos de la persona sean correctos (nombre, apellido, DNI)
     * 2. Valida que el DNI sea único en el sistema (RN-001)
     * 3. Si la persona tiene domicilio asociado:
     *    a. Si domicilio.id == 0 → Es nuevo, lo inserta en la BD
     *    b. Si domicilio.id > 0 → Ya existe, lo actualiza
     * 4. Inserta la persona con la FK domicilio_id correcta
     *
     * IMPORTANTE: La coordinación con DomicilioService permite que el domicilio
     * obtenga su ID autogenerado ANTES de insertar la persona (necesario para la FK).
     *
     * @param pedido Persona a insertar (id será ignorado y regenerado)
     * @throws Exception Si la validación falla, el DNI está duplicado, o hay error de BD
     */
    @Override
    public void insertar(Pedido pedido) throws Exception {
        validatePersona(pedido);
        validateDniUnique(pedido.getTotal(), null);

        // Coordinación con DomicilioService (transaccional)
        if (pedido.getEnvio() != null) {
            if (pedido.getEnvio().getId() == 0) {
                // Domicilio nuevo: insertar primero para obtener ID autogenerado
                domicilioServiceImpl.insertar(pedido.getEnvio());
            } else {
                // Domicilio existente: actualizar datos
                domicilioServiceImpl.actualizar(pedido.getEnvio());
            }
        }

        personaDAO.insertar(pedido);
    }

    /**
     * Actualiza una persona existente en la base de datos.
     *
     * Validaciones:
     * - La persona debe tener datos válidos (nombre, apellido, DNI)
     * - El ID debe ser > 0 (debe ser una persona ya persistida)
     * - El DNI debe ser único (RN-001), excepto para la misma persona
     *
     * IMPORTANTE: Esta operación NO coordina con DomicilioService.
     * Para cambiar el domicilio de una persona, usar MenuHandler que:
     * - Asignar nuevo domicilio: opción 6 (crea nuevo) o 7 (usa existente)
     * - Actualizar domicilio: opción 9 (modifica domicilio actual)
     *
     * @param pedido Persona con los datos actualizados
     * @throws Exception Si la validación falla, el DNI está duplicado, o la persona no existe
     */
    @Override
    public void actualizar(Pedido pedido) throws Exception {
        validatePersona(pedido);
        if (pedido.getId() <= 0) {
            throw new IllegalArgumentException("El ID de la persona debe ser mayor a 0 para actualizar");
        }
        validateDniUnique(pedido.getTotal(), pedido.getId());
        personaDAO.actualizar(pedido);
    }

    /**
     * Elimina lógicamente una persona (soft delete).
     * Marca la persona como eliminado=TRUE sin borrarla físicamente.
     *
     * ⚠️ IMPORTANTE: Este método NO elimina el domicilio asociado (RN-037).
     * Si la persona tiene un domicilio, este quedará activo en la BD.
     * Esto es correcto porque múltiples personas pueden compartir un domicilio.
     *
     * @param id ID de la persona a eliminar
     * @throws Exception Si id <= 0 o no existe la persona
     */
    @Override
    public void eliminar(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        personaDAO.eliminar(id);
    }

    /**
     * Obtiene una persona por su ID.
     * Incluye el domicilio asociado mediante LEFT JOIN (PersonaDAO).
     *
     * @param id ID de la persona a buscar
     * @return Persona encontrada (con su domicilio si tiene), o null si no existe o está eliminada
     * @throws Exception Si id <= 0 o hay error de BD
     */
    @Override
    public Pedido getById(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        return personaDAO.getById(id);
    }

    /**
     * Obtiene todas las personas activas (eliminado=FALSE).
     * Incluye sus domicilios mediante LEFT JOIN (PersonaDAO).
     *
     * @return Lista de personas activas con sus domicilios (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    @Override
    public List<Pedido> getAll() throws Exception {
        return personaDAO.getAll();
    }

    /**
     * Expone el servicio de domicilios para que MenuHandler pueda usarlo.
     * Necesario para operaciones de menú que trabajan directamente con domicilios.
     *
     * @return Instancia de DomicilioServiceImpl inyectada en este servicio
     */
    public DomicilioServiceImpl getDomicilioService() {
        return this.domicilioServiceImpl;
    }

    /**
     * Busca personas por nombre o apellido (búsqueda flexible con LIKE).
     * Usa PersonaDAO.buscarPorNombreApellido() que realiza:
     * - LIKE %filtro% en nombre O apellido
     * - Insensible a mayúsculas/minúsculas (LOWER())
     * - Solo personas activas (eliminado=FALSE)
     *
     * Uso típico: El usuario ingresa "juan" y encuentra "Juan Pérez", "María Juana", etc.
     *
     * @param filtro Texto a buscar (no puede estar vacío)
     * @return Lista de personas que coinciden con el filtro (puede estar vacía)
     * @throws IllegalArgumentException Si el filtro está vacío
     * @throws Exception Si hay error de BD
     */
    public List<Pedido> buscarPorNombreApellido(String filtro) throws Exception {
        if (filtro == null || filtro.trim().isEmpty()) {
            throw new IllegalArgumentException("El filtro de búsqueda no puede estar vacío");
        }
        return personaDAO.buscarPorNombreApellido(filtro);
    }

    /**
     * Busca una persona por DNI exacto.
     * Usa PersonaDAO.buscarPorDni() que realiza búsqueda exacta (=).
     *
     * Uso típico:
     * - Validar unicidad del DNI (validateDniUnique)
     * - Buscar persona específica desde el menú (opción 4)
     *
     * @param dni DNI exacto a buscar (no puede estar vacío)
     * @return Persona con ese DNI, o null si no existe o está eliminada
     * @throws IllegalArgumentException Si el DNI está vacío
     * @throws Exception Si hay error de BD
     */
    public Pedido buscarPorDni(String dni) throws Exception {
        if (dni == null || dni.trim().isEmpty()) {
            throw new IllegalArgumentException("El DNI no puede estar vacío");
        }
        return personaDAO.buscarPorDni(dni);
    }

    /**
     * Elimina un domicilio de forma SEGURA actualizando primero la FK de la persona.
     * Este es el método RECOMENDADO para eliminar domicilios (RN-029 solucionado).
     *
     * Flujo transaccional SEGURO:
     * 1. Obtiene la persona por ID y valida que exista
     * 2. Verifica que el domicilio pertenezca a esa persona (evita eliminar domicilio ajeno)
     * 3. Desasocia el domicilio de la persona (persona.domicilio = null)
     * 4. Actualiza la persona en BD (domicilio_id = NULL)
     * 5. Elimina el domicilio (ahora no hay FKs apuntando a él)
     *
     * DIFERENCIA con DomicilioService.eliminar():
     * - DomicilioService.eliminar(): Elimina directamente (PELIGROSO, puede dejar FKs huérfanas)
     * - Este método: Primero actualiza FK, luego elimina (SEGURO)
     *
     * Usado en MenuHandler opción 10: "Eliminar domicilio de una persona"
     *
     * @param personaId ID de la persona dueña del domicilio
     * @param domicilioId ID del domicilio a eliminar
     * @throws IllegalArgumentException Si los IDs son <= 0, la persona no existe, o el domicilio no pertenece a la persona
     * @throws Exception Si hay error de BD
     */
    public void eliminarDomicilioDePersona(int personaId, int domicilioId) throws Exception {
        if (personaId <= 0 || domicilioId <= 0) {
            throw new IllegalArgumentException("Los IDs deben ser mayores a 0");
        }

        Pedido pedido = personaDAO.getById(personaId);
        if (pedido == null) {
            throw new IllegalArgumentException("Persona no encontrada con ID: " + personaId);
        }

        if (pedido.getEnvio() == null || pedido.getEnvio().getId() != domicilioId) {
            throw new IllegalArgumentException("El domicilio no pertenece a esta persona");
        }

        // Secuencia transaccional: actualizar FK → eliminar domicilio
        pedido.setEnvio(null);
        personaDAO.actualizar(pedido);
        domicilioServiceImpl.eliminar(domicilioId);
    }

    /**
     * Valida que una persona tenga datos correctos.
     *
     * Reglas de negocio aplicadas:
     * - RN-035: Nombre, apellido y DNI son obligatorios
     * - RN-036: Se verifica trim() para evitar strings solo con espacios
     *
     * @param pedido Persona a validar
     * @throws IllegalArgumentException Si alguna validación falla
     */
    private void validatePersona(Pedido pedido) {
        if (pedido == null) {
            throw new IllegalArgumentException("La persona no puede ser null");
        }
        if (pedido.getNumero() == null || pedido.getNumero().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (pedido.getClienteNombre() == null || pedido.getClienteNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El apellido no puede estar vacío");
        }
        if (pedido.getTotal() == null || pedido.getTotal().trim().isEmpty()) {
            throw new IllegalArgumentException("El DNI no puede estar vacío");
        }
    }

    /**
     * Valida que un DNI sea único en el sistema.
     * Implementa la regla de negocio RN-001: "El DNI debe ser único".
     *
     * Lógica:
     * 1. Busca si existe una persona con ese DNI en la BD
     * 2. Si NO existe → OK, el DNI es único
     * 3. Si existe → Verifica si es la misma persona que estamos actualizando:
     *    a. Si personaId == null (INSERT) → Error, DNI duplicado
     *    b. Si personaId != null (UPDATE) y existente.id == personaId → OK, es la misma persona
     *    c. Si personaId != null (UPDATE) y existente.id != personaId → Error, DNI duplicado
     *
     * Ejemplo de uso correcto en UPDATE:
     * - Persona ID=5 con DNI="12345678" quiere actualizar su nombre
     * - validateDniUnique("12345678", 5) → Encuentra persona con DNI="12345678" (ID=5)
     * - Como existente.id (5) == personaId (5) → OK, la persona se está actualizando a sí misma
     *
     * @param dni DNI a validar
     * @param personaId ID de la persona (null para INSERT, != null para UPDATE)
     * @throws IllegalArgumentException Si el DNI ya existe y pertenece a otra persona
     * @throws Exception Si hay error de BD al buscar
     */
    private void validateDniUnique(String dni, Integer personaId) throws Exception {
        Pedido existente = personaDAO.buscarPorDni(dni);
        if (existente != null) {
            // Existe una persona con ese DNI
            if (personaId == null || existente.getId() != personaId) {
                // Es INSERT (personaId == null) o es UPDATE pero el DNI pertenece a otra persona
                throw new IllegalArgumentException("Ya existe una persona con el DNI: " + dni);
            }
            // Si llegamos aquí: es UPDATE y el DNI pertenece a la misma persona → OK
        }
    }
}