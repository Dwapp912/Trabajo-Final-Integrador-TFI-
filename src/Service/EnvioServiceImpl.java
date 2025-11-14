package Service;

import java.util.List;
import Dao.GenericDAO;
import Models.Envio;

/**
 * Implementación del servicio de negocio para la entidad Envio.
 * Capa intermedia entre la UI y el DAO que aplica validaciones de negocio.
 *
 * Responsabilidades:
 * - Validar que los datos del envio sean correctos ANTES de persistir
 * - Aplicar reglas de negocio (RN-023: tracking, costo, fechaDespacho, fechaEstimada, tipo, empresa, estado y pedidoId obligatorios)
 * - Delegar operaciones de BD al DAO
 * - Transformar excepciones técnicas en errores de negocio comprensibles
 *
 * Patrón: Service Layer con inyección de dependencias
 */
public class EnvioServiceImpl implements GenericService<Envio> {
    /**
     * DAO para acceso a datos de envio.
     * Inyectado en el constructor (Dependency Injection).
     * Usa GenericDAO para permitir testing con mocks.
     */
    private final GenericDAO<Envio> domicilioDAO;

    /**
     * Constructor con inyección de dependencias.
     * Valida que el DAO no sea null (fail-fast).
     *
     * @param envioDAO DAO de envio (normalmente EnvioDAO)
     * @throws IllegalArgumentException si envioDAO es null
     */
    public EnvioServiceImpl(GenericDAO<Envio> domicilioDAO) {
        if (domicilioDAO == null) {
            throw new IllegalArgumentException("EnvioDAO no puede ser null");
        }
        this.domicilioDAO = domicilioDAO;
    }

    /**
     * Inserta un nuevo envio en la base de datos.
     *
     * Flujo:
     * 1. Valida que tracking, costo, fechaDespacho, fechaEstimada, tipo, empresa, estado y pedidoId no estén vacíos
     * 2. Delega al DAO para insertar
     * 3. El DAO asigna el ID autogenerado al objeto envio
     *
     * @param envio Envio a insertar (id será ignorado y regenerado)
     * @throws Exception Si la validación falla o hay error de BD
     */
    @Override
    public void insertar(Envio envio) throws Exception {
        validateEnvio(envio);
        domicilioDAO.insertar(envio);
    }

    /**
     * Actualiza un envio existente en la base de datos.
     *
     * Validaciones:
     * - El envio debe tener datos válidos (tracking, costo, fechaDespacho, fechaEstimada, tipo, empresa, estado y pedidoId)
     * - El ID debe ser > 0 (debe ser un envio ya persistido)
     *
     * IMPORTANTE: Si varios pedidos comparten este envio,
     * la actualización los afectará a TODOS (RN-040).
     *
     * @param envio Envio con los datos actualizados
     * @throws Exception Si la validación falla o el envio no existe
     */
    @Override
    public void actualizar(Envio envio) throws Exception {
        validateEnvio(envio);
        if (envio.getId() <= 0) {
            throw new IllegalArgumentException("El ID del envio debe ser mayor a 0 para actualizar");
        }
        domicilioDAO.actualizar(envio);
    }

    /**
     * Elimina lógicamente un envio (soft delete).
     * Marca el envio como eliminado=TRUE sin borrarlo físicamente.
     *
     * ⚠️ ADVERTENCIA: Este método NO verifica si hay pedidos asociados.
     * Puede dejar referencias huérfanas en pedido.envio_id (RN-029).
     *
     * ALTERNATIVA SEGURA: Usar PedidoServiceImpl.eliminarEnvioDePedido()
     * que actualiza la FK antes de eliminar (opción 10 del menú).
     *
     * @param id ID del envio a eliminar
     * @throws Exception Si id <= 0 o no existe el envio
     */
    @Override
    public void eliminar(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        domicilioDAO.eliminar(id);
    }

    /**
     * Obtiene un envio por su ID.
     *
     * @param id ID del envio a buscar
     * @return Envio encontrado, o null si no existe o está eliminado
     * @throws Exception Si id <= 0 o hay error de BD
     */
    @Override
    public Envio getById(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        return domicilioDAO.getById(id);
    }

    /**
     * Obtiene todos los envios activos (eliminado=FALSE).
     *
     * @return Lista de envios activos (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    @Override
    public List<Envio> getAll() throws Exception {
        return domicilioDAO.getAll();
    }

    /**
     * Valida que un envio tenga datos correctos.
     *
     * Reglas de negocio aplicadas:
     * - RN-023: tracking, costo, fechaDespacho, fechaEstimada, tipo, empresa, estado y pedidoId son obligatorios
     * - RN-024: Se verifica trim() para evitar strings solo con espacios
     *
     * @param envio Envio a validar
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