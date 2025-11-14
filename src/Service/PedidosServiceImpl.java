package Service;

import Models.Pedido;

import java.util.List;
import Dao.PedidoDAO;

/**
 * Implementación del servicio de negocio para la entidad Pedido.
 * Capa intermedia entre la UI y el DAO que aplica validaciones de negocio.
 *
 * Responsabilidades:
 * - Validar datos del pedido ANTES de persistir (RN-035: número, cliente, total obligatorios)
 * - Garantizar unicidad del número de pedido (RN-001)
 * - Coordinar operaciones con el Envío cuando corresponda
 * - Proporcionar métodos de búsqueda (por número y por nombre de cliente)
 * - Implementar eliminación segura del envío asociado cuando se solicita
 *
 * Patrón: Service Layer con inyección de dependencias y coordinación de servicios
 */
public class PedidosServiceImpl implements GenericService<Pedido> {
    /**
     * DAO para acceso a datos de pedido.
     * Inyectado en el constructor (Dependency Injection).
     */
    private final PedidoDAO pedidoDAO;

    /**
     * Servicio de envios para coordinar operaciones transaccionales.
     * IMPORTANTE: PedidoServiceImpl necesita EnvioService porque:
     * - Un pedido puede crear/actualizar su envio al insertarse/actualizarse
     * - El servicio coordina la secuencia: insertar envio → insertar pedido
     * - Implementa eliminación segura: actualizar FK pedido → eliminar envio
     */
    private final EnvioServiceImpl envioServiceImpl;

    /**
     * Constructor con inyección de dependencias.
     * Valida que ambas dependencias no sean null (fail-fast).
     *
     * @param pedidoDAO DAO de pedido (normalmente PedidoDAO)
     * @param envioServiceImpl Servicio de envios para operaciones coordinadas
     * @throws IllegalArgumentException si alguna dependencia es null
     */
    public PedidosServiceImpl(PedidoDAO pedidoDAO, EnvioServiceImpl envioServiceImpl) {
        if (pedidoDAO == null) {
            throw new IllegalArgumentException("PedidoDAO no puede ser null");
        }
        if (envioServiceImpl == null) {
            throw new IllegalArgumentException("EnvioServiceImpl no puede ser null");
        }
        this.pedidoDAO = pedidoDAO;
        this.envioServiceImpl = envioServiceImpl;
    }

    /**
     * Inserta un nuevo pedido en la base de datos.
     *
     * Flujo transaccional complejo:
     * 1. Valida que los datos del pedido sean correctos (ID, clienteNombre)
     * 2. Valida que el ID sea único en el sistema (RN-001)
     * 3. Si el pedido tiene envio asociado:
     *    a. Si envio.id == 0 → Es nuevo, lo inserta en la BD
     *    b. Si envio.id > 0 → Ya existe, lo actualiza
     * 4. Inserta el pedido con la FK envio_id correcta
     *
     * IMPORTANTE: La coordinación con EnvioService permite que el envio
     * obtenga su ID autogenerado ANTES de insertar el pedido (necesario para la FK).
     *
     * @param pedido Pedido a insertar (id será ignorado y regenerado)
     * @throws Exception Si la validación falla, el ID está duplicado, o hay error de BD
     */
    @Override
    public void insertar(Pedido pedido) throws Exception {
        validarPedido(pedido);
        validateNumeroUnique(pedido.getNumero(), null);

        // Coordinación con EnvioService (transaccional)
        if (pedido.getEnvio() != null) {
            if (pedido.getEnvio().getId() == 0) {
                // Envio nuevo: insertar primero para obtener ID autogenerado
                envioServiceImpl.insertar(pedido.getEnvio());
            } else {
                // Envio existente: actualizar datos
                envioServiceImpl.actualizar(pedido.getEnvio());
            }
        }

        pedidoDAO.insertar(pedido);
    }

    /**
     * Actualiza un pedido existente en la base de datos.
     *
     * Validaciones:
     * - El pedido debe tener datos válidos (número, cliente, total)
     * - El ID debe ser > 0 (pedido ya persistido)
     * - El número de pedido debe ser único (RN-001), excepto para el mismo pedido
     *
     * IMPORTANTE: Esta operación NO coordina con EnvioService.
     * Para cambiar el envio de un pedido, usar MenuHandler que:
     * - Asignar nuevo envio: opción 6 (crea nuevo) o 7 (usa existente)
     * - Actualizar envio: opción 9 (modifica envio actual)
     *
     * @param pedido Pedido con los datos actualizados
     * @throws Exception Si la validación falla, el ID está duplicado, o el pedido no existe
     */
    @Override
    public void actualizar(Pedido pedido) throws Exception {
        validarPedido(pedido);
        if (pedido.getId() <= 0) {
            throw new IllegalArgumentException("El ID del pedido debe ser mayor a 0 para actualizar");
        }
        validateNumeroUnique(pedido.getNumero(), pedido.getId());
        pedidoDAO.actualizar(pedido);
    }

    /**
     * Elimina lógicamente un pedido (soft delete).
     * Marca el pedido como eliminado=TRUE sin borrarlo físicamente.
     *
     * ⚠️ IMPORTANTE: Este método NO elimina el envio asociado (RN-037).
     * Si el pedido tiene un envio, este quedará activo en la BD.
     * Esto es correcto porque múltiples pedidos pueden compartir un envio.
     *
     * @param id ID de el pedido a eliminar
     * @throws Exception Si id <= 0 o no existe el pedido
     */
    @Override
    public void eliminar(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        pedidoDAO.eliminar(id);
    }

    /**
     * Obtiene un pedido por su ID.
     * Incluye el envio asociado mediante LEFT JOIN (PedidoDAO).
     *
     * @param id ID del pedido a buscar
     * @return Pedido encontrado (con su envio si tiene), o null si no existe o está eliminada
     * @throws Exception Si id <= 0 o hay error de BD
     */
    @Override
    public Pedido getById(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }

        return pedidoDAO.getById(id);
    }

    /**
     * Obtiene todas los pedidos activos (eliminado=FALSE).
     * Incluye sus envios mediante LEFT JOIN (PedidoDAO).
     *
     * @return Lista de pedidos activos con sus envios (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    @Override
    public List<Pedido> getAll() throws Exception {
        return pedidoDAO.getAll();
    }

    /**
     * Expone el servicio de envios para que MenuHandler pueda usarlo.
     * Necesario para operaciones de menú que trabajan directamente con envio.
     *
     * @return Instancia de EnvioServiceImpl inyectada en este servicio
     */
    public EnvioServiceImpl getEnvioService() {
        return this.envioServiceImpl;
    }

    /**
     * Busca pedido por clienteNombre (búsqueda flexible con LIKE).
     * Usa PedidoDAO.buscarPorNombreCliente() que realiza:
     * - LIKE %filtro% en nombre O apellido
     * - Insensible a mayúsculas/minúsculas (LOWER())
     * - Solo pedidos activos (eliminado=FALSE)
     *
     * Ejemplo: filtro = "ana" encuentra "Ana", "Anastasia", "Mariana".
     *
     * @param filtro Texto a buscar (no puede estar vacío)
     * @return Lista de pedido que coinciden con el filtro (puede estar vacía)
     * @throws IllegalArgumentException Si el filtro está vacío
     * @throws Exception Si hay error de BD
     */
    public List<Pedido> buscarPorNombreCliente(String filtro) throws Exception {
        if (filtro == null || filtro.trim().isEmpty()) {
            throw new IllegalArgumentException("El filtro de búsqueda no puede estar vacío");
        }
        return pedidoDAO.buscarPorNombreCliente(filtro);
    }

    /**
     * Busca un pedido por número exacto.
     * Usa PedidoDAO.buscarPorNumeroDePedido() que realiza búsqueda exacta (=).
     *
     * Uso típico:
     * - Validar unicidad del número de pedido (validateNumeroUnique)
     * - Buscar pedido específico desde el menú
     *
     * @param numeroPedido Número de pedido exacto a buscar (no puede estar vacío)
     * @return Pedido con ese número, o null si no existe o está eliminado
     * @throws IllegalArgumentException Si el número de pedido está vacío
     * @throws Exception Si hay error de BD
     */
    public Pedido buscarPorNumeroDePedido(String numeroPedido) throws Exception {
        if (numeroPedido == null || numeroPedido.trim().isEmpty()) {
            throw new IllegalArgumentException("El número de pedido no puede estar vacío");
        }
        return pedidoDAO.buscarPorNumeroDePedido(numeroPedido);
    }

    /**
     * Elimina un envio de forma SEGURA actualizando primero la FK del pedido.
     * Este es el método RECOMENDADO para eliminar envios (RN-029 solucionado).
     *
     * Flujo transaccional SEGURO:
     * 1. Obtiene el pedido por ID y valida que exista
     * 2. Verifica que el envio pertenezca a ese pedido (evita eliminar envio ajeno)
     * 3. Desasocia el envio del pedido (pedido.envio = null)
     * 4. Actualiza el pedido en BD (envio_id = NULL)
     * 5. Elimina el envio (ahora no hay FKs apuntando a él)
     *
     * DIFERENCIA con EnvioService.eliminar():
     * - EnvioService.eliminar(): Elimina directamente (PELIGROSO, puede dejar FKs huérfanas)
     * - Este método: Primero actualiza FK, luego elimina (SEGURO)
     *
     * Usado en MenuHandler opción 10: "Eliminar envio de un pedido"
     *
     * @param pedidoId ID del pedido dueño del envio
     * @param envioId ID del envio a eliminar
     * @throws IllegalArgumentException Si los IDs son <= 0, el pedido no existe, o el envio no pertenece al pedido
     * @throws Exception Si hay error de BD
     */
    public void eliminarEnvioDePedido(int pedidoId, int envioId) throws Exception {
        if (pedidoId <= 0 || envioId <= 0) {
            throw new IllegalArgumentException("Los IDs deben ser mayores a 0");
        }

        Pedido pedido = pedidoDAO.getById(pedidoId);
        if (pedido == null) {
            throw new IllegalArgumentException("Pedido no encontrado con ID: " + pedidoId);
        }

        if (pedido.getEnvio() == null || pedido.getEnvio().getId() != envioId) {
            throw new IllegalArgumentException("El envio no pertenece a este pedido");
        }

        // Secuencia transaccional: actualizar FK → eliminar envio
        pedido.setEnvio(null);
        pedidoDAO.actualizar(pedido);
        envioServiceImpl.eliminar(envioId);
    }

    /**
     * Valida que un pedido tenga datos correctos.
     *
     * Reglas de negocio aplicadas:
     * - RN-035: Número, nombre de cliente y total son obligatorios
     * - Se verifica trim() para evitar strings solo con espacios
     * - El total no puede ser negativo
     *
     * @param pedido Pedido a validar
     * @throws IllegalArgumentException Si alguna validación falla
     */
    private void validarPedido(Pedido pedido) {
        if (pedido == null) {
            throw new IllegalArgumentException("El pedido no puede ser null");
        }
        if (pedido.getNumero() == null || pedido.getNumero().trim().isEmpty()) {
            throw new IllegalArgumentException("El numero no puede estar vacío");
        }
        if (pedido.getClienteNombre() == null || pedido.getClienteNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del cliente no puede estar vacío");
        }
        if (pedido.getTotal() == null || pedido.getTotal() < 0) {
            throw new IllegalArgumentException("El total no puede ser menor a cero.");
        }
    }

    /**
     * Valida que el número de pedido sea único en el sistema.
     * Implementa la regla de negocio RN-001: "El número de pedido debe ser único".
     *
     * Lógica:
     * 1. Busca si existe un pedido con ese número en la BD
     * 2. Si NO existe → OK, el número es único
     * 3. Si existe → Verifica si es el mismo pedido que estamos actualizando:
     *    a. Si pedidoId == null (INSERT) → Error, número duplicado
     *    b. Si pedidoId != null (UPDATE) y existente.id == pedidoId → OK, es el mismo pedido
     *    c. Si pedidoId != null (UPDATE) y existente.id != pedidoId → Error, número duplicado
     *
     * Ejemplo de uso en UPDATE:
     * - Pedido ID=5 con número="A-100" quiere actualizar otros datos
     * - validateNumeroUnique("A-100", 5) → Encuentra pedido con número="A-100" (ID=5)
     * - Como existente.id (5) == pedidoId (5) → OK, se está actualizando a sí mismo
     *
     * @param numeroPedido Número de pedido a validar
     * @param pedidoId ID del pedido (null para INSERT, != null para UPDATE)
     * @throws IllegalArgumentException Si el número ya existe y pertenece a otro pedido
     * @throws Exception Si hay error de BD al buscar
     */
    private void validateNumeroUnique(String numeroPedido, Integer pedidoId) throws Exception {
        Pedido existente = pedidoDAO.buscarPorNumeroDePedido(numeroPedido);
        if (existente != null) {
            // Ya existe un pedido con ese número
            if (pedidoId == null || existente.getId() != pedidoId) {
                // Es INSERT o UPDATE pero el número pertenece a otro pedido
                throw new IllegalArgumentException("Ya existe un pedido con el número: " + numeroPedido);
            }
            // Si llegamos aquí: es UPDATE y el número pertenece al mismo pedido → OK
        }
    }
}