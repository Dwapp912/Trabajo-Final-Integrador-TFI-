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
            throw new IllegalArgumentException("PersonaService no puede ser null");
        }
        this.scanner = scanner;
        this.pedidosService = pedidosService;
        this.enviosService = enviosService;
    }

    /**
     * Opción 1: Crear nuevo pedido (con envio opcional).
     *
     * Flujo:
     * 1. Solicita clienteNombre y ID
     * 2. Pregunta si desea agregar envio
     * 3. Si sí, captura calle y número
     * 4. Crea objeto Pedido y opcionalmente Envio
     * 5. Invoca pedidoService.insertar() que:
     *    - Valida datos (clienteNombre, ID obligatorios)
     *    - Valida ID único (RN-001)
     *    - Si hay envio, lo inserta primero (obtiene ID)
     *    - Inserta pedido con FK envio_id correcta
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
     * Opción 2: Listar pedidos (todos o filtrados por nombre/apellido).
     *
     * Submenú:
     * 1. Listar todos los pedidos actios (getAll)
     * 2. Buscar por nombre o apellido con LIKE (buscarPorclienteNombre)
     *
     * Muestra:
     * - ID, Nombre, Apellido
     * - Envio (si tiene): id
     *
     * Manejo de casos especiales:
     * - Si no hay pedidos: Muestra "No se encontraron pedidos"
     * - Si el pedido no tiene envio: Solo muestra datos del pedido
     *
     * Búsqueda por nombre/apellido:
     * - Usa PedidoDAO.buscarPorclienteNombre() que hace LIKE '%filtro%'
     * - Insensible a mayúsculas en MySQL (depende de collation)
     * - Busca en nombre O apellido
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
     *    - Nombre (Enter para mantener actual)
     *    - Apellido (Enter para mantener actual)
     *    - ID (Enter para mantener actual)
     * 4. Llama a actualizarEnvioDePedido() para manejar cambios en envio
     * 5. Invoca pedidoService.actualizar() que valida:
     *    - Datos obligatorios (clienteNombre, ID)
     *    - ID único (RN-001), excepto para el mismo pedido
     *
     * Patrón "Enter para mantener":
     * - Lee input con scanner.nextLine().trim()
     * - Si isEmpty() → NO actualiza el campo (mantiene valor actual)
     * - Si tiene valor → Actualiza el campo
     *
     * IMPORTANTE: Esta operación NO actualiza el envio directamente.
     * El envio se maneja en actualizarEnvioDePedido() que puede:
     * - Actualizar envio existente (afecta a TODOS los pedidos que lo comparten)
     * - Agregar nuevo envio si el pedido no tenía
     * - Dejar envio sin cambios
     */
    public void actualizarPedido() {
        try {
            System.out.print("ID del pedido a actualizar: ");
            int id = Integer.parseInt(scanner.nextLine());
            Pedido p = pedidosService.getById(id);

            if (p == null) {
                System.out.println("Persona no encontrada.");
                return;
            }

            System.out.print("Nuevo nombre (actual: " + p.getNumero() + ", Enter para mantener): ");
            String nombre = scanner.nextLine().trim();
            if (!nombre.isEmpty()) {
                p.setNumero(nombre);
            }

            System.out.print("Nuevo apellido (actual: " + p.getClienteNombre() + ", Enter para mantener): ");
            String apellido = scanner.nextLine().trim();
            if (!apellido.isEmpty()) {
                p.setClienteNombre(apellido);
            }

            System.out.print("Nuevo DNI (actual: " + p.getTotal() + ", Enter para mantener): ");
            Double dni = Double.parseDouble(scanner.nextLine().trim());
            p.setTotal(dni);

            actualizarEnvioDePedido(p);
            pedidosService.actualizar(p);
            System.out.println("Persona actualizada exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al actualizar persona: " + e.getMessage());
        }
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
     * IMPORTANTE: El envio NO se elimina porque:
     * - Múltiples pedidos pueden compartir un envio
     * - Si se eliminara, afectaría a otros pedidos
     *
     * Si se quiere eliminar también el envio:
     * - Usar opción 10: "Eliminar envio de un pedido" (eliminarEnvioPorPedido)
     * - Esa opción primero desasocia el envio, luego lo elimina (seguro)
     */
    public void eliminarPedido() {
        try {
            System.out.print("ID del pedido a eliminar: ");
            int id = Integer.parseInt(scanner.nextLine());
            pedidosService.eliminar(id);
        } catch (Exception e) {
            System.err.println("Error al eliminar persona: " + e.getMessage());
        }
    }

    /**
     * Opción 6: Listar todos los envios activos.
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
                System.out.println("No se encontraron domicilios.");
                return;
            }
            for (Envio d : envios) {
                System.out.println("ID: " + d.getId() + ", " + d.getEmpresa() + " " + d.getTracking());
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
            System.out.print("ID del domicilio a actualizar: ");
            int id = Integer.parseInt(scanner.nextLine());
            Envio envio = pedidosService.getEnvioService().getById(id);

            if (envio == null) {
                System.out.println("Domicilio no encontrado.");
                return;
            }

            System.out.print("Nueva empresa (actual: " + envio.getEmpresa() + ", Enter para mantener): ");
            String empresaString = scanner.nextLine().trim();
            if (!empresaString.isEmpty()) {
                Envio.Empresa empresa = Envio.Empresa.valueOf(empresaString);
                envio.setEmpresa(empresa);
            }

            System.out.print("Nuevo tacking (actual: " + envio.getTracking() + ", Enter para mantener): ");
            String numero = scanner.nextLine().trim();
            if (!numero.isEmpty()) {
                envio.setTracking(numero);
            }

            pedidosService.getEnvioService().actualizar(envio);
            System.out.println("Envío actualizado exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al actualizar envío: " + e.getMessage());
        }
    }

    /**
     * Opción 8: Eliminar envio por ID (PELIGROSO - soft delete directo).
     *
     * ⚠️ PELIGRO (RN-029): Este método NO verifica si hay pedidos asociados.
     * Si hay pedidos con FK a este envio, quedarán con referencia huérfana.
     *
     * Flujo:
     * 1. Solicita ID del envio
     * 2. Invoca envioService.eliminar() directamente
     * 3. Marca envio.eliminado = TRUE
     *
     * Problemas potenciales:
     * - Pedidos con envio_id apuntando a envio "eliminado"
     * - Datos inconsistentes en la BD
     *
     * ALTERNATIVA SEGURA: Opción 10 (eliminarEnvioPorPedido)
     * - Primero desasocia envio del pedido (envio_id = NULL)
     * - Luego elimina el envio
     * - Garantiza consistencia
     *
     * Uso válido:
     * - Cuando se está seguro de que el envio NO tiene pedidos asociados
     * - Limpiar envios creados por error
     */
    public void eliminarDomicilioPorId() {
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
     * Opción 7: Actualizar envio de un pedido específico.
     *
     * Flujo:
     * 1. Solicita ID del pedido
     * 2. Verifica que el pedido exista y tenga envio
     * 3. Muestra valores actuales del envio
     * 4. Permite actualizar calle y número
     * 5. Invoca envioService.actualizar()
     *
     * ⚠️ IMPORTANTE (RN-040): Esta operación actualiza el envio compartido.
     * Si otros pedidos tienen el mismo envio, también se les actualizará.
     *
     * Diferencia con opción 9 (actualizarEnvioPorId):
     * - Esta opción: Busca pedido primero, luego actualiza su envio
     * - Opción 9: Actualiza envio directamente por ID
     *
     * Ambas tienen el mismo efecto (RN-040): afectan a TODOS los pedidos
     * que comparten el envio.
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

            Envio d = p.getEnvio();
            System.out.print("Nueva empresa (" + d.getEmpresa() + "): ");
            String empresaString = scanner.nextLine().trim();
            if (!empresaString.isEmpty()) {
                d.setEmpresa(Envio.Empresa.valueOf(empresaString));
            }

            System.out.print("Nuevo tracking (" + d.getTracking() + "): ");
            String tracking = scanner.nextLine().trim();
            if (!tracking.isEmpty()) {
                d.setTracking(tracking);
            }

            pedidosService.getEnvioService().actualizar(d);
            System.out.println("Domicilio actualizado exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al actualizar domicilio: " + e.getMessage());
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
     * - Garantiza consistencia: Primero actualiza FK, luego elimina
     * - NO deja referencias huérfanas
     * - Implementa eliminación segura recomendada en RN-029
     *
     * Este es el método RECOMENDADO para eliminar envios en producción.
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
    public void crearEnvio()  {
        try {
            System.out.print("ID del pedido a asignar Envio: ");
            int id = Integer.parseInt(scanner.nextLine());
            Pedido p = pedidosService.getById(id);
            if (p == null) {
                System.out.println("Persona no encontrada.");
                return;
            }
            System.out.print("Tracking: ");
            String tracking = scanner.nextLine().trim();
            System.out.print("Empresa: ");
            Envio.Empresa empresa = Envio.Empresa.valueOf(scanner.nextLine().trim());
            System.out.print("Tipo Envio: ");
            Envio.Tipo tipo = Envio.Tipo.valueOf(scanner.nextLine().trim());
            System.out.print("Estado Envio: ");
            Envio.Estado estado = Envio.Estado.valueOf(scanner.nextLine().trim());
            System.out.print("Costo Envio: ");
            Double costo =  Double.parseDouble(scanner.nextLine());

            System.out.print("Ingrese fecha despacho");
            System.out.print("Ingrese dia (DD)");
            int dia = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Ingrese mes: (MM)");
            int mes = (Integer.parseInt(scanner.nextLine().trim()));
            System.out.print("Ingrese ano: (AAAA");
            int ano = Integer.parseInt(scanner.nextLine().trim());
            LocalDate fechaDespacho = LocalDate.of(ano, mes, dia);

            System.out.print("Ingrese fecha estimada");
            System.out.print("Ingrese dia (DD)");
            int diaE = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Ingrese mes: (MM)");
            int mesE = (Integer.parseInt(scanner.nextLine().trim()));
            System.out.print("Ingrese ano: (AAAA");
            int anoE = Integer.parseInt(scanner.nextLine().trim());
            LocalDate fechaEstimada = LocalDate.of(anoE, mesE, diaE);



            Envio envio = new Envio(0, false, tracking, empresa, tipo, costo, fechaDespacho, fechaEstimada, estado,p);
            this.enviosService.insertar(envio);

        }
        catch (Exception e) {
            System.err.println("Error al crear envío: " + e.getMessage());

        }

    }

    public Envio crearEnvio(Pedido pedido){
        try{
            System.out.print("Tracking: ");
            String tracking = scanner.nextLine().trim();
            System.out.print("Empresa: ");
            Envio.Empresa empresa = Envio.Empresa.valueOf(scanner.nextLine().trim());
            System.out.print("Tipo Envio: ");
            Envio.Tipo tipo = Envio.Tipo.valueOf(scanner.nextLine().trim());
            System.out.print("Estado Envio: ");
            Envio.Estado estado = Envio.Estado.valueOf(scanner.nextLine().trim());
            System.out.print("Costo Envio: ");
            Double costo =  Double.parseDouble(scanner.nextLine());

            System.out.print("Ingrese fecha despacho");
            System.out.print("Ingrese dia (DD)");
            int dia = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Ingrese mes: (MM)");
            int mes = (Integer.parseInt(scanner.nextLine().trim()));
            System.out.print("Ingrese ano: (AAAA");
            int ano = Integer.parseInt(scanner.nextLine().trim());
            LocalDate fechaDespacho = LocalDate.of(ano, mes, dia);

            System.out.print("Ingrese fecha estimada");
            System.out.print("Ingrese dia (DD)");
            int diaE = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Ingrese mes: (MM)");
            int mesE = (Integer.parseInt(scanner.nextLine().trim()));
            System.out.print("Ingrese ano: (AAAA");
            int anoE = Integer.parseInt(scanner.nextLine().trim());
            LocalDate fechaEstimada = LocalDate.of(anoE, mesE, diaE);



            Envio envio = new Envio(0, false, tracking, empresa, tipo, costo, fechaDespacho, fechaEstimada, estado, pedido);
            this.enviosService.insertar(envio);
            return envio;
        }
        catch(Exception e) {
            System.err.println("Error al eliminar envío: " + e.getMessage());
            return null;
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
     * IMPORTANTE: El parámetro Persona se modifica in-place (setDomicilio).
     * El caller debe invocar personaService.actualizar() después para persistir.
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