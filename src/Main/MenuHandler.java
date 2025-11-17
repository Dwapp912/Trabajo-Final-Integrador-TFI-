package Main;

import Models.Pedido;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import Models.Envio;
import Service.EnvioServiceImpl;
import Service.PedidosServiceImpl;
import java.time.LocalDate;

/**
 * Controlador de las operaciones del menú (Menu Handler).
 * Gestiona toda la lógica de interacción con el usuario para operaciones CRUD.
 *
 * Responsabilidades:
 * - Capturar entrada del usuario desde consola (Scanner)
 * - Validar entrada básica (conversión de tipos, valores vacíos)
 * - Invocar servicios de negocio (PedidoService, EnvioService)
 * - Mostrar resultados y mensajes de error al usuario
 * - Coordinar operaciones complejas (crear pedido con envio, etc.)
 *
 * Patrón: Controller (MVC) - capa de presentación en arquitectura de 4 capas
 * Arquitectura: Main → Service → DAO → Models
 *
 * IMPORTANTE: Este handler NO contiene lógica de negocio.
 * Todas las validaciones de negocio están en la capa Service.
 */
public class MenuHandler {
    /**
     * Scanner compartido para leer entrada del usuario.
     * Inyectado desde AppMenu para evitar múltiples Scanners de System.in.
     */
    private final Scanner scanner;

    /**
     * Servicio de pedido para operaciones CRUD.
     * También proporciona acceso a EnvioService mediante getEnvioService().
     */
    private final PedidosServiceImpl pedidosService;
    private final EnvioServiceImpl enviosService;

    /**
     * Constructor con inyección de dependencias.
     * Valida que las dependencias no sean null (fail-fast).
     *
     * @param scanner        Scanner compartido para entrada de usuario
     * @param pedidosService Servicio de pedidos
     * @param enviosService Servicio de envios
     * @throws IllegalArgumentException si alguna dependencia es null
     */
    public MenuHandler(Scanner scanner, PedidosServiceImpl pedidosService, EnvioServiceImpl enviosService) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner no puede ser null");
        }
        if (pedidosService == null) {
            throw new IllegalArgumentException("PedidosService no puede ser null");
        }
        this.scanner = scanner;
        this.pedidosService = pedidosService;
        this.enviosService = enviosService;
    }

    /**
     * Opción 1: Crear nuevo pedido (con envio opcional).
     *
     * Flujo:
     * 1. Solicita número de pedido, fecha, nombre del cliente y total
     * 2. Pregunta si desea agregar envío
     * 3. Si sí, captura datos del envío
     * 4. Crea objeto Pedido y opcionalmente Envío
     * 5. Invoca pedidosService.insertar() que valida datos y unicidad del número
     *
     * Input trimming: Aplica .trim() a todas las entradas (patrón consistente).
     *
     * Manejo de errores:
     * - IllegalArgumentException: Validaciones de negocio (muestra mensaje al usuario)
     * - SQLException: Errores de BD (muestra mensaje al usuario)
     * - Todos los errores se capturan y muestran, NO se propagan al menú principal
     */
    public void crearPedido() {
        try {
            System.out.print("Número de Tracking: ");
            String numeroPedido = scanner.nextLine().trim();
            // TODO: Parsear fecha desde scanner
            System.out.println("Ingrese fecha del Pedido:");
            LocalDate fecha = obtenerFechaDesdeScanner();
            System.out.print("Nombre del Cliente: ");
            String nombreCliente = scanner.nextLine().trim();
            System.out.print("Total del Pedido: ");
            Double totalPedido = Double.valueOf(scanner.nextLine().trim());
            Pedido.Estado estadoPedido = Pedido.Estado.NUEVO;

            Pedido pedido = new Pedido(0, false, numeroPedido, fecha, nombreCliente,  totalPedido, estadoPedido, null);
            pedidosService.insertar(pedido);
            System.out.print("¿Desea agregar un envio? (s/n): ");
            if (scanner.nextLine().equalsIgnoreCase("s")) {
                Envio envio = crearEnvio(pedido);
                pedido.setEnvio(envio);
            }

            System.out.println("Pedido creado exitosamente con ID: " + pedido.getId());
        } catch (Exception e) {
            System.err.println("Error al crear el pedido: " + e.getMessage());
        }
    }

    /**
     * Opción 2: Listar pedidos (todos o filtrados por nombre de cliente).
     *
     * Submenú:
     * 1. Listar todos los pedidos activos (getAll)
     * 2. Buscar por nombre o apellido con LIKE (buscarPorNombreCliente)
     *
     * Muestra:
     * - ID, Nombre, Apellido
     * - Envio (si tiene): id
     *
     * Manejo de casos especiales:
     * - Si no hay pedidos: Muestra "No se encontraron pedidos"
     * - Si el pedido no tiene envio: Solo muestra datos del pedido
     *
     * Búsqueda por nombre de cliente:
     * - Usa PedidoDAO.buscarPorNombreCliente() que hace LIKE '%filtro%'
     * - Insensible a mayúsculas en MySQL (depende de collation)
     */
    public void listarPedidos() {
        try {
            System.out.print("¿Desea (1) listar todos o (2) buscar por nombre cliente? Ingrese opcion: ");
            int subopcion = Integer.parseInt(scanner.nextLine());

            List<Pedido> pedidos;
            if (subopcion == 1) {
                pedidos = pedidosService.getAll();
            } else if (subopcion == 2) {
                System.out.print("Ingrese texto a buscar: ");
                String filtro = scanner.nextLine().trim();
                pedidos = pedidosService.buscarPorNombreCliente(filtro);
            } else {
                System.out.println("Opcion invalida.");
                return;
            }

            if (pedidos.isEmpty()) {
                System.out.println("No se encontraron pedidos.");
                return;
            }

            for (Pedido p : pedidos) {
                System.out.println("ID: " + p.getId() + ", Numero: " + p.getNumero() +
                        ", Nombre Cliente: " + p.getClienteNombre() + ", Total: " + p.getTotal());
                if (p.getEnvio() != null) {
                    System.out.println("   Envío: " + p.getEnvio().getEmpresa() +
                            " " + p.getEnvio().getTracking());
                }
            }
        } catch (Exception e) {
            System.err.println("Error al listar pedidos: " + e.getMessage());
        }
    }

    /**
     * Opción 3: Actualizar pedido existente.
     *
     * Flujo:
     * 1. Solicita ID del pedido
     * 2. Obtiene pedido actual de la BD
     * 3. Muestra valores actuales y permite actualizar:
     *    - Número de pedido (Enter para mantener actual)
     *    - Nombre del cliente (Enter para mantener actual)
     *    - Total (Enter para mantener actual)
     * 4. Llama a actualizarEnvioDePedido() para manejar cambios en el envío asociado
     * 5. Invoca pedidosService.actualizar() que valida:
     *    - Datos obligatorios (número, cliente, total)
     *    - Número de pedido único (RN-001), excepto para el mismo pedido
     *
     * Patrón "Enter para mantener":
     * - Lee input con scanner.nextLine().trim()
     * - Si isEmpty() → NO actualiza el campo (mantiene valor actual)
     * - Si tiene valor → Actualiza el campo
     *
     * Nota: La actualización del envío se realiza con actualizarEnvioDePedido().
     */
    public void actualizarPedido() {
        try {
            System.out.print("ID del pedido a actualizar: ");
            Pedido p = obtenerPedidoDesdeScanner();
            if (p == null) return;

            System.out.print("Nuevo número de pedido (actual: " + p.getNumero() + ", Enter para mantener): ");
            String numero = scanner.nextLine().trim();
            if (!numero.isEmpty()) {
                p.setNumero(numero);
            }

            System.out.print("Nuevo nombre de cliente (actual: " + p.getClienteNombre() + ", Enter para mantener): ");
            String nombreCliente = scanner.nextLine().trim();
            if (!nombreCliente.isEmpty()) {
                p.setClienteNombre(nombreCliente);
            }

            System.out.print("Nuevo total (actual: " + p.getTotal() + ", Enter para mantener): ");
            String totalStr = scanner.nextLine().trim();
            if (!totalStr.isEmpty()) {
                Double total = Double.parseDouble(totalStr);
                p.setTotal(total);
            }

            actualizarEnvioDePedido(p);
            pedidosService.actualizar(p);
            System.out.println("Pedido actualizado exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al actualizar pedido: " + e.getMessage());
        }
    }

    private Pedido obtenerPedidoDesdeScanner() throws Exception {
        int id = Integer.parseInt(scanner.nextLine());
        Pedido p = pedidosService.getById(id);

        if (p == null) {
            System.out.println("Pedido no encontrado.");
            return null;
        }
        return p;
    }

    /**
     * Opción 4: Eliminar pedido (soft delete).
     *
     * Flujo:
     * 1. Solicita ID del pedido
     * 2. Invoca pedidoService.eliminar() que:
     *    - Marca pedido.eliminado = TRUE
     *    - NO elimina el envio asociado (RN-037)
     *
     * Nota: El envío no se elimina automáticamente para evitar inconsistencias.
     * Si también desea eliminar el envío asociado de forma segura:
     * - Use la opción 10: "Eliminar envío por ID de pedido" (eliminarEnvioDePedido)
     * - Esa opción primero desasocia el envío del pedido y luego lo elimina
     */
    public void eliminarPedido() {
        try {
            System.out.print("ID del pedido a eliminar: ");
            int id = Integer.parseInt(scanner.nextLine());
            pedidosService.eliminar(id);
        } catch (Exception e) {
            System.err.println("Error al eliminar pedido, ingrese un número válido mayor a 0. Detalle: " + e.getMessage());
        }
    }

    /**
     * Opción 6: Listar todos los envios activos.
     *
     * Muestra: ID, Empresa, Tracking, Tipo, Estado y Costo
     *
     * Uso típico:
     * - Ver envíos disponibles
     * - Consultar ID de envío para actualizar (opción 7) o eliminar (opción 8)
     *
     * Nota: Solo muestra envíos con eliminado=FALSE (soft delete), según la implementación del servicio.
     */
    public void listarEnvios() {
        try {
            List<Envio> envios = pedidosService.getEnvioService().getAll();
            if (envios.isEmpty()) {
                System.out.println("No se encontraron envios.");
                return;
            }
            for (Envio d : envios) {
                System.out.println("ID envio : " + d.getId() + " Empresa de envio: " + d.getEmpresa() + " Numero de tracking : " + d.getTracking());
            }
        } catch (Exception e) {
            System.err.println("Error al listar envíos: " + e.getMessage());
        }
    }

    /**
     * Opción 7: Actualizar envío por ID.
     *
     * Flujo:
     * 1. Solicita ID del envío
     * 2. Obtiene el envío actual de la BD
     * 3. Muestra valores actuales y permite actualizar:
     *    - Empresa
     *    - Tracking (Enter para mantener actual)
     *    - Tipo
     *    - Estado
     *    - Costo (Enter para mantener actual)
     *    - Fecha de despacho (Enter para mantener actual)
     * 4. Invoca envioService.actualizar()
     *
     * Patrón "Enter para mantener":
     * - Si el usuario presiona Enter sin texto, se mantiene el valor actual
     */
    public void actualizarEnvioPorId() {
        try {
            System.out.print("ID del envio a actualizar: ");
            int id = Integer.parseInt(scanner.nextLine());
            Envio envio = pedidosService.getEnvioService().getByIdUpdate(id);

            if (envio == null) {
                System.out.println("Envio no encontrado.");
                return;
            } else if (envio.isEliminado()) {
                checkEliminado(envio, id);
            } else {
                actualizarEnvioPorId(envio);
            }
        } catch (Exception e) {
            System.err.println("Error al actualizar envío: " + e.getMessage());
        }
    }


    public void actualizarEnvioPorId(Envio envio) {

        Envio.Empresa empresa = obtenerEmpresaDesdeScanner(envio.getEmpresa());
        envio.setEmpresa(empresa);
        System.out.print("Nuevo tacking (actual: " + envio.getTracking() + ", Enter para mantener): ");
        String numero = scanner.nextLine().trim();
        if (!numero.isEmpty()) {
            envio.setTracking(numero);
        } else {
            envio.setTracking(envio.getTracking());
        }

        Envio.Tipo tipo = obtenerTipoDesdeScanner(envio.getTipo());
        envio.setTipo(tipo);
        Envio.Estado estado = obtenerEstadoDesdeScanner(envio.getEstado());
        envio.setEstado(estado);
        System.out.print("Nuevo costo (actual: " + envio.getCosto() + "): ");
        String inputCosto = scanner.nextLine().trim();
        if (!inputCosto.isEmpty()) {
            boolean valido = true;
            while (valido) {
                try {
                    double costo = Double.parseDouble(inputCosto);

                    if (costo <= 0) {
                        System.out.println("Ingrese numero mayor a 0");
                        inputCosto = scanner.nextLine().trim();
                    } else {
                        envio.setCosto(costo);
                        valido = false;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Formato de numero no valido " + e.getMessage());
                    inputCosto = scanner.nextLine().trim();
                }
            }
        } else {
            envio.setCosto(envio.getCosto());
        }

        System.out.print("Nuevo fecha de despacho (actual: " + envio.getFechaDespacho() + ", Enter para mantener ingrese cualquier valor para cambiar fecha): ");
        String opcion = scanner.nextLine().trim();
        if (opcion.isEmpty()) {
            envio.setFechaDespacho(envio.getFechaDespacho());
        } else {
            LocalDate fecha = obtenerFechaDesdeScanner();;
            envio.setFechaDespacho(fecha);
        }

        try {
            pedidosService.getEnvioService().actualizar(envio);
        } catch (Exception ex) {
            System.out.println("Error al actualizar pedido " + ex.getMessage());
        }
        System.out.println("Envío actualizado exitosamente.");
    }

    private LocalDate obtenerFechaDesdeScanner() {
        System.out.print("Ingrese dia: (DD) ");
        int dia = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Ingrese mes: (MM) ");
        int mes = (Integer.parseInt(scanner.nextLine().trim()));
        System.out.print("Ingrese año: (AAAA) ");
        int ano = Integer.parseInt(scanner.nextLine().trim());
        return LocalDate.of(ano, mes, dia);
    }


    /**
     * Opción 8: Eliminar envio por ID (PELIGROSO - soft delete directo).
     *
     * Advertencia: Este método elimina el envío por su ID sin verificar si está asociado a un pedido.
     * Si hay un pedido referenciando este envío, podría dejar datos inconsistentes.
     *
     * Flujo:
     * 1. Solicita ID del envio
     * 2. Invoca envioService.eliminar() directamente
     * 3. Marca envio.eliminado = TRUE
     *
     * Alternativa segura: Opción 10 (eliminarEnvioDePedido)
     * - Primero desasocia el envío del pedido
     * - Luego elimina el envío
     * - Garantiza consistencia referencial
     *
     * Uso válido:
     * - Cuando se está seguro de que el envío NO está asociado a ningún pedido
     * - Limpiar envíos creados por error
     */
    public void eliminarEnvioPorId() {
        try {
            System.out.print("ID del envío a eliminar: ");
            int id = Integer.parseInt(scanner.nextLine());
            pedidosService.getEnvioService().eliminar(id);
            System.out.println("Envío eliminado exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al eliminar envío: " + e.getMessage());
        }
    }

    /**
     * Opción 9: Actualizar envio de un pedido específico.
     *
     * Flujo:
     * 1. Solicita ID del pedido
     * 2. Verifica que el pedido exista y tenga envío asociado
     * 3. Muestra valores actuales del envío
     * 4. Permite actualizar campos del envío (empresa, tracking, tipo, estado, costo, fecha)
     * 5. Invoca envioService.actualizar()
     *
     * Nota: Esta opción toma el envío desde el pedido para asegurar que se actualice el correcto.
     */
    public void actualizarEnvioPorPedido() {
        try {
            System.out.print("ID de el pedido cuyo envío desea actualizar: ");
            Pedido p = obtenerPedidoDesdeScanner();
            if (p == null) return;
            Envio envio = pedidosService.getEnvioService().getByIdUpdate(p.getEnvio().getId());

            if (envio == null) {
                System.out.println("El pedido no tiene envio asociado.");
                return;
            }

            if (envio.isEliminado() ) {
                System.out.println("El envío" + envio + "\nFigura como eliminado quiere reinsertar el mismo envio con los mismos datos? (s/n)");
                String subopcion = scanner.nextLine().trim();
                if (subopcion.equalsIgnoreCase("s")) {
                    envio.setEliminado(false);
                    pedidosService.getEnvioService().restaurar(p.getId());
                    System.out.println("Envio reinsertado exitosamente");
                    ;
                } else {

                    actualizarEnvioPorId(envio);
                }
            } else {
                actualizarEnvioPorId(envio);
            }

        } catch (Exception e) {
            System.err.println("Error al actualizar envio: " + e.getMessage());
        }
    }

    public void checkEliminado(Envio envio, int pedidoId) {
        try {
            System.out.println("El envío" + envio + "\nFigura como eliminado quiere reinsertar el mismo envio con los mismos datos? (s/n)");
            String subopcion = scanner.nextLine().trim();
            if (subopcion.equalsIgnoreCase("s")) {
                envio.setEliminado(false);
                pedidosService.getEnvioService().restaurar(pedidoId);
                System.out.println("Envio reinsertado exitosamente");
                ;
            } else {
                actualizarEnvioPorId(envio);
            }
        } catch (Exception e) {
            System.err.println("Error al actualizar envio: " + e.getMessage());
        }
    }



    /**
     * Opción 10: Eliminar envio de un pedido (MÉTODO SEGURO - RN-029 solucionado).
     *
     * Flujo transaccional SEGURO:
     * 1. Solicita ID del pedido
     * 2. Verifica que el pedido exista y tenga envio
     * 3. Invoca pedidoService.eliminarEnvioDePedido() que:
     *    a. Desasocia envio de pedido (pedido.envio = null)
     *    b. Actualiza pedido en BD (envio_id = NULL)
     *    c. Elimina el envio (ahora no hay FKs apuntando a él)
     *
     * Ventaja sobre opción 8 (eliminarEnvioPorId):
     * - Garantiza consistencia: Primero actualiza la FK, luego elimina
     * - Evita referencias huérfanas
     *
     * Este es el método RECOMENDADO para eliminar envios en producción.
     */
    public void eliminarEnvioDePedido() {
        try {
            System.out.print("ID de el pedido cuyo envío desea eliminar: ");
            Pedido p = obtenerPedidoDesdeScanner();
            if (p == null) return;

            if (p.getEnvio() == null) {
                System.out.println("El pedido no tiene envío asociado.");
                return;
            }

            int envioId = p.getEnvio().getId();
            pedidosService.eliminarEnvioDePedido(p.getId(), envioId);
            System.out.println("Envío eliminado exitosamente y referencia actualizada.");
        } catch (Exception e) {
            System.err.println("Error al eliminar envío: " + e.getMessage());
        }
    }

    /**
     * Método auxiliar: Captura desde consola los datos y crea un Envío asociado a un Pedido.
     *
     * Flujo:
     * 1. Solicita tracking, empresa, tipo, estado y costo
     * 2. Solicita fechas de despacho y estimada (valida orden lógico de fechas)
     * 3. Crea el objeto Envio en memoria y lo inserta usando enviosService
     *
     * Notas:
     * - Valida que la fecha de despacho no sea anterior a la fecha del pedido
     * - Valida que la fecha estimada no sea anterior a la fecha de despacho
     * - Devuelve null (inserción realizada por el servicio); el pedido puede consultarse luego para obtener el envío
     */



    public Envio crearEnvio(Pedido pedido) {
        try {
            System.out.print("Tracking: ");
            String tracking = scanner.nextLine().trim();
            Envio.Empresa empresa = obtenerEmpresaDesdeScanner(Envio.Empresa.OCA);
            Envio.Tipo tipo = obtenerTipoDesdeScanner(Envio.Tipo.EXPRES);
            Envio.Estado estado = obtenerEstadoDesdeScanner(Envio.Estado.EN_PREPARACION);

            System.out.print("Costo Envio: ");
            Double costo = Double.parseDouble(scanner.nextLine());

            LocalDate fechaDespacho = obtenerFechaPosteriorA(pedido.getFecha(), "despacho", "pedido");
            LocalDate fechaEstimada = obtenerFechaPosteriorA(fechaDespacho, "llegada", "despacho");

            Envio envio = new Envio(0, false, tracking, empresa, tipo, costo, fechaDespacho, fechaEstimada, estado, pedido);
            enviosService.insertar(envio);

        } catch (Exception e) {
            System.err.println("Error al crear envío: " + e.getMessage());

        }
        return null;
    }

    private LocalDate obtenerFechaPosteriorA(LocalDate fechaAnterior, String nombreFechaAObtener, String nombreFechaAnterior) {
        LocalDate fechaDespacho = null;
        do {
            System.out.println("Ingrese fecha " + nombreFechaAObtener);
            fechaDespacho = obtenerFechaDesdeScanner();
            if (fechaDespacho.isBefore(fechaAnterior)){
                System.out.println("La fecha de no puede ser anterior a la fecha de " + nombreFechaAnterior);
            }
        } while (fechaDespacho.isBefore(fechaAnterior));
        return fechaDespacho;
    }

    private Envio.Estado obtenerEstadoDesdeScanner(Envio.Estado estadoActual) {
        if (estadoActual != null) {
            System.out.print("Actual/Default: " + estadoActual + " (Enter para mantener). ");
        }
        System.out.print("OPCIONES: (1: EN PREPARACION, 2: EN_TRANSITO, 3: ENTREGADO): ");
        Envio.Estado estado = Map.of(
                "1", Envio.Estado.EN_PREPARACION,
                "2", Envio.Estado.EN_TRANSITO,
                "3", Envio.Estado.ENTREGADO).getOrDefault(scanner.nextLine().trim(), estadoActual);
        return estado;
    }

    private Envio.Tipo obtenerTipoDesdeScanner(Envio.Tipo tipoActual) {
        if (tipoActual != null) {
            System.out.print("Actual/Default: " + tipoActual + " (Enter para mantener). ");
        }
        System.out.print("OPCIONES: (1: ESTÁNDAR, 2: EXPRES): ");
        Envio.Tipo tipo = Map.of(
                "1", Envio.Tipo.ESTANDAR,
                "2", Envio.Tipo.EXPRES).getOrDefault(scanner.nextLine().trim(),  tipoActual);
        return tipo;
    }

    private Envio.Empresa obtenerEmpresaDesdeScanner(Envio.Empresa empresaActual) {
        if (empresaActual != null) {
            System.out.print("Actual/Default: " + empresaActual + " (Enter para mantener). ");
        }
        System.out.print("OPCIONES: (1: CORREO ARG, 2: ANDREANI, 3: OCA): ");
        Envio.Empresa empresa =Map.of(
                "1", Envio.Empresa.CORREO_ARG,
                "2", Envio.Empresa.ANDREANI,
                "3", Envio.Empresa.OCA).getOrDefault(scanner.nextLine().trim(), empresaActual);
        return empresa;
    }

    public void crearEnvio() {
        try {
            System.out.print("ID del pedido a asignar Envio: ");
            Pedido p = obtenerPedidoDesdeScanner();
            if (p == null) return;
            if (p.getEnvio()!=null){
                System.out.println("El pedido ya tiene un envío asignado");
                }
            else{
                crearEnvio(p);
            }
        } catch (Exception e) {
            System.err.println("Error al crear envío: " + e.getMessage());

        }

    }
    /**
     * Método auxiliar privado: Maneja la actualización o alta del envío dentro de actualizarPedido().
     *
     * Casos:
     * 1. El pedido TIENE envío:
     *    - Pregunta si desea actualizar
     *    - Permite cambiar empresa y tracking (Enter para mantener)
     *    - Actualiza el envío en BD
     *
     * 2. El pedido NO TIENE envío:
     *    - Pregunta si desea agregar uno
     *    - Si sí, captura datos con crearEnvio(pedido) e inserta el envío
     *    - Asocia el envío al pedido
     *
     * Usado exclusivamente por actualizarPedido() (opción 3).
     *
     * @param p Pedido al que se le actualizará/agregará el envío
     * @throws Exception Si hay error al insertar/actualizar envío
     */
    private void actualizarEnvioDePedido(Pedido p) throws Exception {
        if (p.getEnvio() != null) {
            System.out.print("¿Desea actualizar el envío? (s/n): ");
            if (scanner.nextLine().equalsIgnoreCase("s")) {
                actualizarEnvioPorId(p.getEnvio());
            }
        } else {
            System.out.print("El pedido no tiene envío. ¿Desea agregar uno? (s/n): ");
            if (scanner.nextLine().equalsIgnoreCase("s")) {
                Envio nuevoEnvío = crearEnvio(p);
                p.setEnvio(nuevoEnvío);
            }
        }
    }
}