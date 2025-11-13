package Main;

import Models.Pedido;

import java.util.List;
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
 * - Invocar servicios de negocio (PedidosService, EnvioService)
 * - Mostrar resultados y mensajes de error al usuario
 * - Coordinar operaciones complejas (crear pedido con envío, etc.)
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
     * Servicio de pedidos para operaciones CRUD.
     * También proporciona acceso a DomicilioService mediante getDomicilioService().
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
     * Opción 1: Crear nuevo pedido (con envío opcional).
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
            System.out.println("Fecha del Pedido: ");
            // TODO: Parsear fecha desde scanner

            System.out.print("Ingrese dia del Pedido: (DD) ");
            int dia = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Ingrese mes: (MM) ");
            int mes = (Integer.parseInt(scanner.nextLine().trim()));
            System.out.print("Ingrese ano: (AAAA) ");
            int ano = Integer.parseInt(scanner.nextLine().trim());
            LocalDate fecha = LocalDate.of(ano, mes, dia);
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
     * 2. Buscar por nombre de cliente con LIKE
     *
     * Muestra:
     * - ID, Número, Nombre de Cliente, Total
     * - Envío (si tiene): Empresa y Tracking
     *
     * Manejo de casos especiales:
     * - Si no hay pedidos: Muestra "No se encontraron pedidos"
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
     * 1. Solicita ID de la persona
     * 2. Obtiene persona actual de la BD
     * 3. Muestra valores actuales y permite actualizar:
     *    - Nombre (Enter para mantener actual)
     *    - Apellido (Enter para mantener actual)
     *    - DNI (Enter para mantener actual)
     * 4. Llama a actualizarDomicilioDePersona() para manejar cambios en domicilio
     * 5. Invoca personaService.actualizar() que valida:
     *    - Datos obligatorios (nombre, apellido, DNI)
     *    - DNI único (RN-001), excepto para la misma persona
     *
     * Patrón "Enter para mantener":
     * - Lee input con scanner.nextLine().trim()
     * - Si isEmpty() → NO actualiza el campo (mantiene valor actual)
     * - Si tiene valor → Actualiza el campo
     *
     * IMPORTANTE: Esta operación NO actualiza el domicilio directamente.
     * El domicilio se maneja en actualizarDomicilioDePersona() que puede:
     * - Actualizar domicilio existente (afecta a TODAS las personas que lo comparten)
     * - Agregar nuevo domicilio si la persona no tenía
     * - Dejar domicilio sin cambios
     */
    public void actualizarPedido() {
        try {
            System.out.print("ID del pedido a actualizar: ");
            int id = Integer.parseInt(scanner.nextLine());
            Pedido p = pedidosService.getById(id);

            if (p == null) {
                System.out.println("Pedido no encontrado.");
                return;
            }

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

    /**
     * Opción 4: Eliminar persona (soft delete).
     *
     * Flujo:
     * 1. Solicita ID de la persona
     * 2. Invoca personaService.eliminar() que:
     *    - Marca persona.eliminado = TRUE
     *    - NO elimina el domicilio asociado (RN-037)
     *
     * IMPORTANTE: El domicilio NO se elimina porque:
     * - Múltiples personas pueden compartir un domicilio
     * - Si se eliminara, afectaría a otras personas
     *
     * Si se quiere eliminar también el domicilio:
     * - Usar opción 10: "Eliminar domicilio de una persona" (eliminarDomicilioPorPersona)
     * - Esa opción primero desasocia el domicilio, luego lo elimina (seguro)
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
     * Opción 6: Listar todos los domicilios activos.
     *
     * Muestra: ID, Calle Número
     *
     * Uso típico:
     * - Ver domicilios disponibles antes de asignar a persona (opción 7)
     * - Consultar ID de domicilio para actualizar (opción 9) o eliminar (opción 8)
     *
     * Nota: Solo muestra domicilios con eliminado=FALSE (soft delete).
     */
    public void listarEnvios() {
        try {
            List<Envio> envios = pedidosService.getEnvioService().getAll();
            if (envios.isEmpty()) {
                System.out.println("No se encontraron envios.");
                return;
            }
            for (Envio d : envios) {
                System.out.println("ID ded envio : " + d.getId() + " Empresa de envio: " + d.getEmpresa() + " Numero de tracking : " + d.getTracking());
            }
        } catch (Exception e) {
            System.err.println("Error al listar envíos: " + e.getMessage());
        }
    }

    /**
     * Opción 9: Actualizar domicilio por ID.
     *
     * Flujo:
     * 1. Solicita ID del domicilio
     * 2. Obtiene domicilio actual de la BD
     * 3. Muestra valores actuales y permite actualizar:
     *    - Calle (Enter para mantener actual)
     *    - Número (Enter para mantener actual)
     * 4. Invoca domicilioService.actualizar()
     *
     * ⚠️ IMPORTANTE (RN-040): Si varias personas comparten este domicilio,
     * la actualización los afectará a TODAS.
     *
     * Ejemplo:
     * - Domicilio ID=1 "Av. Siempreviva 742" está asociado a 3 personas
     * - Si se actualiza a "Calle Nueva 123", las 3 personas tendrán la nueva dirección
     *
     * Esto es CORRECTO para familias que viven juntas.
     * Si se quiere cambiar la dirección de UNA sola persona:
     * 1. Crear nuevo domicilio (opción 5)
     * 2. Asignar a la persona (opción 7)
     */
    public void actualizarEnvioPorId() {
        try {
            System.out.print("ID del envio a actualizar: ");
            int id = Integer.parseInt(scanner.nextLine());
            Envio envio = pedidosService.getEnvioService().getById(id);

            if (envio == null) {
                System.out.println("Envio no encontrado.");
                return;
            } else {
                actualizarEnvioPorId(envio);
            }
        } catch (Exception e) {
            System.err.println("Error al actualizar envío: " + e.getMessage());
        }
    }

    public void actualizarEnvioPorId(Envio envio) {

        System.out.print("Nueva empresa (actual: " + envio.getEmpresa() + ", Enter para mantener): ");
        String empresaString = scanner.nextLine().trim();
        if (!empresaString.isEmpty()) {
            Envio.Empresa empresa = Envio.Empresa.valueOf(empresaString);
            envio.setEmpresa(empresa);
        } else {
            envio.setEmpresa(envio.getEmpresa());
        }

        System.out.print("Nuevo tacking (actual: " + envio.getTracking() + ", Enter para mantener): ");
        String numero = scanner.nextLine().trim();
        if (!numero.isEmpty()) {
            envio.setTracking(numero);
        } else {
            envio.setTracking(envio.getTracking());
        }

        System.out.print("Nuevo Tipo (actual: " + envio.getTipo() + ", Enter para mantener): ");
        String tipo = scanner.nextLine().trim();
        if (!tipo.isEmpty()) {
            envio.setTipo(Envio.Tipo.valueOf(tipo));
        } else {
            envio.setTipo(envio.getTipo());
        }

        System.out.print("Nuevo Estado (actual: " + envio.getEstado() + ", Enter para mantener): ");
        String estado = scanner.nextLine().trim();
        if (!estado.isEmpty()) {
            envio.setEstado(Envio.Estado.valueOf(estado));
        } else {
            envio.setEstado(envio.getEstado());
        }

        System.out.print("Nuevo costo (actual: " + envio.getCosto() + ", Enter para mantener): ");
        String inputCosto = scanner.nextLine().trim();
        if (!inputCosto.isEmpty()) {
            boolean valido = true;
            while (valido == true) {
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
            System.out.print("Ingrese dia (DD) ");
            int dia = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Ingrese mes: (MM) ");
            int mes = (Integer.parseInt(scanner.nextLine().trim()));
            System.out.print("Ingrese ano: (AAAA) ");
            int ano = Integer.parseInt(scanner.nextLine().trim());
            LocalDate fecha = LocalDate.of(ano, mes, dia);
            envio.setFechaDespacho(fecha);
        }

        try {
            pedidosService.getEnvioService().actualizar(envio);
        } catch (Exception ex) {
            System.out.println("Error al actualizar pedido " + ex.getMessage());
        }
        System.out.println("Envío actualizado exitosamente.");
    }





    /**
     * Opción 8: Eliminar domicilio por ID (PELIGROSO - soft delete directo).
     *
     * ⚠️ PELIGRO (RN-029): Este método NO verifica si hay personas asociadas.
     * Si hay personas con FK a este domicilio, quedarán con referencia huérfana.
     *
     * Flujo:
     * 1. Solicita ID del domicilio
     * 2. Invoca domicilioService.eliminar() directamente
     * 3. Marca domicilio.eliminado = TRUE
     *
     * Problemas potenciales:
     * - Personas con domicilio_id apuntando a domicilio "eliminado"
     * - Datos inconsistentes en la BD
     *
     * ALTERNATIVA SEGURA: Opción 10 (eliminarDomicilioPorPersona)
     * - Primero desasocia domicilio de la persona (domicilio_id = NULL)
     * - Luego elimina el domicilio
     * - Garantiza consistencia
     *
     * Uso válido:
     * - Cuando se está seguro de que el domicilio NO tiene personas asociadas
     * - Limpiar domicilios creados por error
     */
    public void eliminarEnvioPorId() {
        try {
            System.out.print("ID del domicilio a eliminar: ");
            int id = Integer.parseInt(scanner.nextLine());
            pedidosService.getEnvioService().eliminar(id);
            System.out.println("Domicilio eliminado exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al eliminar domicilio: " + e.getMessage());
        }
    }

    /**
     * Opción 7: Actualizar domicilio de una persona específica.
     *
     * Flujo:
     * 1. Solicita ID de la persona
     * 2. Verifica que la persona exista y tenga domicilio
     * 3. Muestra valores actuales del domicilio
     * 4. Permite actualizar calle y número
     * 5. Invoca domicilioService.actualizar()
     *
     * ⚠️ IMPORTANTE (RN-040): Esta operación actualiza el domicilio compartido.
     * Si otras personas tienen el mismo domicilio, también se les actualizará.
     *
     * Diferencia con opción 9 (actualizarDomicilioPorId):
     * - Esta opción: Busca persona primero, luego actualiza su domicilio
     * - Opción 9: Actualiza domicilio directamente por ID
     *
     * Ambas tienen el mismo efecto (RN-040): afectan a TODAS las personas
     * que comparten el domicilio.
     */
    public void actualizarEnvioPorPedido() {
        try {
            System.out.print("ID de el pedido cuyo envio desea actualizar: ");
            int pedidoId = Integer.parseInt(scanner.nextLine());
            Pedido p = pedidosService.getById(pedidoId);

            if (p == null) {
                System.out.println("Pedido no encontrado.");
                return;
            }

            if (p.getEnvio() == null) {
                System.out.println("El pedido no tiene envio asociado.");
                return;
            }

            Envio i = p.getEnvio();
            actualizarEnvioPorId(i);
        }
        catch(Exception e) {
            System.err.println("Error al actualizar envio: " + e.getMessage());
        }
    }

    /**
     * Opción 10: Eliminar domicilio de una persona (MÉTODO SEGURO - RN-029 solucionado).
     *
     * Flujo transaccional SEGURO:
     * 1. Solicita ID de la persona
     * 2. Verifica que la persona exista y tenga domicilio
     * 3. Invoca personaService.eliminarDomicilioDePersona() que:
     *    a. Desasocia domicilio de persona (persona.domicilio = null)
     *    b. Actualiza persona en BD (domicilio_id = NULL)
     *    c. Elimina el domicilio (ahora no hay FKs apuntando a él)
     *
     * Ventaja sobre opción 8 (eliminarDomicilioPorId):
     * - Garantiza consistencia: Primero actualiza FK, luego elimina
     * - NO deja referencias huérfanas
     * - Implementa eliminación segura recomendada en RN-029
     *
     * Este es el método RECOMENDADO para eliminar domicilios en producción.
     */
    public void eliminarEnvioDePedido() {
        try {
            System.out.print("ID de el pedido cuyo envío desea eliminar: ");
            int pedidoId = Integer.parseInt(scanner.nextLine());
            Pedido p = pedidosService.getById(pedidoId);

            if (p == null) {
                System.out.println("Pedido no encontrado.");
                return;
            }

            if (p.getEnvio() == null) {
                System.out.println("El pedido no tiene envío asociado.");
                return;
            }

            int envioId = p.getEnvio().getId();
            pedidosService.eliminarEnvioDePedido(pedidoId, envioId);
            System.out.println("Envío eliminado exitosamente y referencia actualizada.");
        } catch (Exception e) {
            System.err.println("Error al eliminar envío: " + e.getMessage());
        }
    }

    /**
     * Método auxiliar privado: Crea un objeto Domicilio capturando calle y número.
     *
     * Flujo:
     * 1. Solicita calle (con trim)
     * 2. Solicita número (con trim)
     * 3. Crea objeto Domicilio con ID=0 (será asignado por BD al insertar)
     *
     * Usado por:
     * - crearPersona(): Para agregar domicilio al crear persona
     * - actualizarDomicilioDePersona(): Para agregar domicilio a persona sin domicilio
     *
     * Nota: NO persiste en BD, solo crea el objeto en memoria.
     * El caller es responsable de insertar el domicilio.
     *
     * @return Domicilio nuevo (no persistido, ID=0)
     */
   
    

    public Envio crearEnvio(Pedido pedido) {
        try {
            System.out.print("Tracking: ");
            String tracking = scanner.nextLine().trim();
            System.out.print("Empresa: ");
            Envio.Empresa empresa = Envio.Empresa.valueOf(scanner.nextLine().trim());
            System.out.print("Tipo Envio: ");
            Envio.Tipo tipo = Envio.Tipo.valueOf(scanner.nextLine().trim());
            System.out.print("Estado Envio: ");
            Envio.Estado estado = Envio.Estado.valueOf(scanner.nextLine().trim());
            System.out.print("Costo Envio: ");
            Double costo = Double.parseDouble(scanner.nextLine());

            LocalDate fechaDespacho = null;

            do {
                System.out.println("Ingrese fecha despacho");
                System.out.print("Ingrese dia (DD)");
                int dia = Integer.parseInt(scanner.nextLine().trim());
                System.out.print("Ingrese mes: (MM)");
                int mes = (Integer.parseInt(scanner.nextLine().trim()));
                System.out.print("Ingrese ano: (AAAA) ");
                int ano = Integer.parseInt(scanner.nextLine().trim());
                fechaDespacho = LocalDate.of(ano, mes, dia);
                if (fechaDespacho.isBefore(pedido.getFecha())){
                    System.out.println("La fecha de despacho no puede ser anterior a la del pedido");
                }
            } while (fechaDespacho.isBefore(pedido.getFecha()));
            {

            }
            LocalDate fechaEstimada = null;

            do {
                System.out.println("Ingrese fecha estimada de llegada");
                System.out.print("Ingrese dia (DD)");
                int diaE = Integer.parseInt(scanner.nextLine().trim());
                System.out.print("Ingrese mes: (MM)");
                int mesE = (Integer.parseInt(scanner.nextLine().trim()));
                System.out.print("Ingrese ano: (AAAA) ");
                int anoE = Integer.parseInt(scanner.nextLine().trim());
                fechaEstimada = LocalDate.of(anoE, mesE, diaE);

                if (fechaEstimada.isBefore(fechaDespacho)) {
                    System.out.println("la Fecha Estimada de llegada no puede ser menor a la de despacho");
                } else {
                    Envio envio = new Envio(0, false, tracking, empresa, tipo, costo, fechaDespacho, fechaEstimada, estado, pedido);
                    enviosService.insertar(envio);
                }
            } while (fechaEstimada.isBefore(fechaDespacho));

        } catch (Exception e) {
            System.err.println("Error al crear envío: " + e.getMessage());

        }
        return null;
    }
    
     public void crearEnvio() {
        try {
            System.out.print("ID del pedido a asignar Envio: ");
            int id = Integer.parseInt(scanner.nextLine());
            Pedido p = pedidosService.getById(id);
            if (p == null) {
                System.out.println("Pedido no encontrado.");
            }
            else if (p.getEnvio()!=null){                
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
     * Método auxiliar privado: Maneja actualización de domicilio dentro de actualizar persona.
     *
     * Casos:
     * 1. Persona TIENE domicilio:
     *    - Pregunta si desea actualizar
     *    - Si sí, permite cambiar calle y número (Enter para mantener)
     *    - Actualiza domicilio en BD (afecta a TODAS las personas que lo comparten)
     *
     * 2. Persona NO TIENE domicilio:
     *    - Pregunta si desea agregar uno
     *    - Si sí, captura calle y número con crearDomicilio()
     *    - Inserta domicilio en BD (obtiene ID)
     *    - Asocia domicilio a la persona
     *
     * Usado exclusivamente por actualizarPersona() (opción 3).
     *
     * @param p Persona a la que se le actualizará/agregará domicilio
     * @throws Exception Si hay error al insertar/actualizar domicilio
     */
    private void actualizarEnvioDePedido(Pedido p) throws Exception {
        if (p.getEnvio() != null) {
            System.out.print("¿Desea actualizar el envío? (s/n): ");
            if (scanner.nextLine().equalsIgnoreCase("s")) {
                System.out.print("Nueva empresa (" + p.getEnvio().getEmpresa() + "): ");
                String empresaString = scanner.nextLine().trim();
                if (!empresaString.isEmpty()) {
                    p.getEnvio().setEmpresa(Envio.Empresa.valueOf(empresaString));
                }

                System.out.print("Nuevo tracking (" + p.getEnvio().getTracking() + "): ");
                String tracking = scanner.nextLine().trim();
                if (!tracking.isEmpty()) {
                    p.getEnvio().setTracking(tracking);
                }

                pedidosService.getEnvioService().actualizar(p.getEnvio());
            }
        } else {
            System.out.print("El pedido no tiene envío. ¿Desea agregar uno? (s/n): ");
            if (scanner.nextLine().equalsIgnoreCase("s")) {
                Envio nuevoDom = crearEnvio(p);
                pedidosService.getEnvioService().insertar(nuevoDom);
                p.setEnvio(nuevoDom);
            }
        }
    }
}