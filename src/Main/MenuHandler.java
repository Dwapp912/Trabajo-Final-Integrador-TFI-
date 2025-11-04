package Main;

import Models.Persona;
import java.util.List;
import java.util.Scanner;
import Models.Domicilio;
import Service.PersonaServiceImpl;

/**
 * Controlador de las operaciones del menú (Menu Handler).
 * Gestiona toda la lógica de interacción con el usuario para operaciones CRUD.
 *
 * Responsabilidades:
 * - Capturar entrada del usuario desde consola (Scanner)
 * - Validar entrada básica (conversión de tipos, valores vacíos)
 * - Invocar servicios de negocio (PersonaService, DomicilioService)
 * - Mostrar resultados y mensajes de error al usuario
 * - Coordinar operaciones complejas (crear persona con domicilio, etc.)
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
     * Servicio de personas para operaciones CRUD.
     * También proporciona acceso a DomicilioService mediante getDomicilioService().
     */
    private final PersonaServiceImpl personaService;

    /**
     * Constructor con inyección de dependencias.
     * Valida que las dependencias no sean null (fail-fast).
     *
     * @param scanner Scanner compartido para entrada de usuario
     * @param personaService Servicio de personas
     * @throws IllegalArgumentException si alguna dependencia es null
     */
    public MenuHandler(Scanner scanner, PersonaServiceImpl personaService) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner no puede ser null");
        }
        if (personaService == null) {
            throw new IllegalArgumentException("PersonaService no puede ser null");
        }
        this.scanner = scanner;
        this.personaService = personaService;
    }

    /**
     * Opción 1: Crear nueva persona (con domicilio opcional).
     *
     * Flujo:
     * 1. Solicita nombre, apellido y DNI
     * 2. Pregunta si desea agregar domicilio
     * 3. Si sí, captura calle y número
     * 4. Crea objeto Persona y opcionalmente Domicilio
     * 5. Invoca personaService.insertar() que:
     *    - Valida datos (nombre, apellido, DNI obligatorios)
     *    - Valida DNI único (RN-001)
     *    - Si hay domicilio, lo inserta primero (obtiene ID)
     *    - Inserta persona con FK domicilio_id correcta
     *
     * Input trimming: Aplica .trim() a todas las entradas (patrón consistente).
     *
     * Manejo de errores:
     * - IllegalArgumentException: Validaciones de negocio (muestra mensaje al usuario)
     * - SQLException: Errores de BD (muestra mensaje al usuario)
     * - Todos los errores se capturan y muestran, NO se propagan al menú principal
     */
    public void crearPersona() {
        try {
            System.out.print("Nombre: ");
            String nombre = scanner.nextLine().trim();
            System.out.print("Apellido: ");
            String apellido = scanner.nextLine().trim();
            System.out.print("DNI: ");
            String dni = scanner.nextLine().trim();

            Domicilio domicilio = null;
            System.out.print("¿Desea agregar un domicilio? (s/n): ");
            if (scanner.nextLine().equalsIgnoreCase("s")) {
                domicilio = crearDomicilio();
            }

            Persona persona = new Persona(0, nombre, apellido, dni);
            persona.setDomicilio(domicilio);
            personaService.insertar(persona);
            System.out.println("Persona creada exitosamente con ID: " + persona.getId());
        } catch (Exception e) {
            System.err.println("Error al crear persona: " + e.getMessage());
        }
    }

    /**
     * Opción 2: Listar personas (todas o filtradas por nombre/apellido).
     *
     * Submenú:
     * 1. Listar todas las personas activas (getAll)
     * 2. Buscar por nombre o apellido con LIKE (buscarPorNombreApellido)
     *
     * Muestra:
     * - ID, Nombre, Apellido, DNI
     * - Domicilio (si tiene): Calle Número
     *
     * Manejo de casos especiales:
     * - Si no hay personas: Muestra "No se encontraron personas"
     * - Si la persona no tiene domicilio: Solo muestra datos de persona
     *
     * Búsqueda por nombre/apellido:
     * - Usa PersonaDAO.buscarPorNombreApellido() que hace LIKE '%filtro%'
     * - Insensible a mayúsculas en MySQL (depende de collation)
     * - Busca en nombre O apellido
     */
    public void listarPersonas() {
        try {
            System.out.print("¿Desea (1) listar todos o (2) buscar por nombre/apellido? Ingrese opcion: ");
            int subopcion = Integer.parseInt(scanner.nextLine());

            List<Persona> personas;
            if (subopcion == 1) {
                personas = personaService.getAll();
            } else if (subopcion == 2) {
                System.out.print("Ingrese texto a buscar: ");
                String filtro = scanner.nextLine().trim();
                personas = personaService.buscarPorNombreApellido(filtro);
            } else {
                System.out.println("Opcion invalida.");
                return;
            }

            if (personas.isEmpty()) {
                System.out.println("No se encontraron personas.");
                return;
            }

            for (Persona p : personas) {
                System.out.println("ID: " + p.getId() + ", Nombre: " + p.getNombre() +
                        ", Apellido: " + p.getApellido() + ", DNI: " + p.getDni());
                if (p.getDomicilio() != null) {
                    System.out.println("   Domicilio: " + p.getDomicilio().getCalle() +
                            " " + p.getDomicilio().getNumero());
                }
            }
        } catch (Exception e) {
            System.err.println("Error al listar personas: " + e.getMessage());
        }
    }

    /**
     * Opción 3: Actualizar persona existente.
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
    public void actualizarPersona() {
        try {
            System.out.print("ID de la persona a actualizar: ");
            int id = Integer.parseInt(scanner.nextLine());
            Persona p = personaService.getById(id);

            if (p == null) {
                System.out.println("Persona no encontrada.");
                return;
            }

            System.out.print("Nuevo nombre (actual: " + p.getNombre() + ", Enter para mantener): ");
            String nombre = scanner.nextLine().trim();
            if (!nombre.isEmpty()) {
                p.setNombre(nombre);
            }

            System.out.print("Nuevo apellido (actual: " + p.getApellido() + ", Enter para mantener): ");
            String apellido = scanner.nextLine().trim();
            if (!apellido.isEmpty()) {
                p.setApellido(apellido);
            }

            System.out.print("Nuevo DNI (actual: " + p.getDni() + ", Enter para mantener): ");
            String dni = scanner.nextLine().trim();
            if (!dni.isEmpty()) {
                p.setDni(dni);
            }

            actualizarDomicilioDePersona(p);
            personaService.actualizar(p);
            System.out.println("Persona actualizada exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al actualizar persona: " + e.getMessage());
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
    public void eliminarPersona() {
        try {
            System.out.print("ID de la persona a eliminar: ");
            int id = Integer.parseInt(scanner.nextLine());
            personaService.eliminar(id);
            System.out.println("Persona eliminada exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al eliminar persona: " + e.getMessage());
        }
    }

    /**
     * Opción 5: Crear domicilio independiente (sin asociar a persona).
     *
     * Flujo:
     * 1. Llama a crearDomicilio() para capturar calle y número
     * 2. Invoca domicilioService.insertar() que:
     *    - Valida calle y número obligatorios (RN-023)
     *    - Inserta en BD y asigna ID autogenerado
     * 3. Muestra ID generado
     *
     * Uso típico:
     * - Crear domicilio que luego se asignará a varias personas (opción 7)
     * - Pre-cargar domicilios en la BD
     */
    public void crearDomicilioIndependiente() {
        try {
            Domicilio domicilio = crearDomicilio();
            personaService.getDomicilioService().insertar(domicilio);
            System.out.println("Domicilio creado exitosamente con ID: " + domicilio.getId());
        } catch (Exception e) {
            System.err.println("Error al crear domicilio: " + e.getMessage());
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
    public void listarDomicilios() {
        try {
            List<Domicilio> domicilios = personaService.getDomicilioService().getAll();
            if (domicilios.isEmpty()) {
                System.out.println("No se encontraron domicilios.");
                return;
            }
            for (Domicilio d : domicilios) {
                System.out.println("ID: " + d.getId() + ", " + d.getCalle() + " " + d.getNumero());
            }
        } catch (Exception e) {
            System.err.println("Error al listar domicilios: " + e.getMessage());
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
    public void actualizarDomicilioPorId() {
        try {
            System.out.print("ID del domicilio a actualizar: ");
            int id = Integer.parseInt(scanner.nextLine());
            Domicilio d = personaService.getDomicilioService().getById(id);

            if (d == null) {
                System.out.println("Domicilio no encontrado.");
                return;
            }

            System.out.print("Nueva calle (actual: " + d.getCalle() + ", Enter para mantener): ");
            String calle = scanner.nextLine().trim();
            if (!calle.isEmpty()) {
                d.setCalle(calle);
            }

            System.out.print("Nuevo numero (actual: " + d.getNumero() + ", Enter para mantener): ");
            String numero = scanner.nextLine().trim();
            if (!numero.isEmpty()) {
                d.setNumero(numero);
            }

            personaService.getDomicilioService().actualizar(d);
            System.out.println("Domicilio actualizado exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al actualizar domicilio: " + e.getMessage());
        }
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
    public void eliminarDomicilioPorId() {
        try {
            System.out.print("ID del domicilio a eliminar: ");
            int id = Integer.parseInt(scanner.nextLine());
            personaService.getDomicilioService().eliminar(id);
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
    public void actualizarDomicilioPorPersona() {
        try {
            System.out.print("ID de la persona cuyo domicilio desea actualizar: ");
            int personaId = Integer.parseInt(scanner.nextLine());
            Persona p = personaService.getById(personaId);

            if (p == null) {
                System.out.println("Persona no encontrada.");
                return;
            }

            if (p.getDomicilio() == null) {
                System.out.println("La persona no tiene domicilio asociado.");
                return;
            }

            Domicilio d = p.getDomicilio();
            System.out.print("Nueva calle (" + d.getCalle() + "): ");
            String calle = scanner.nextLine().trim();
            if (!calle.isEmpty()) {
                d.setCalle(calle);
            }

            System.out.print("Nuevo numero (" + d.getNumero() + "): ");
            String numero = scanner.nextLine().trim();
            if (!numero.isEmpty()) {
                d.setNumero(numero);
            }

            personaService.getDomicilioService().actualizar(d);
            System.out.println("Domicilio actualizado exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al actualizar domicilio: " + e.getMessage());
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
    public void eliminarDomicilioPorPersona() {
        try {
            System.out.print("ID de la persona cuyo domicilio desea eliminar: ");
            int personaId = Integer.parseInt(scanner.nextLine());
            Persona p = personaService.getById(personaId);

            if (p == null) {
                System.out.println("Persona no encontrada.");
                return;
            }

            if (p.getDomicilio() == null) {
                System.out.println("La persona no tiene domicilio asociado.");
                return;
            }

            int domicilioId = p.getDomicilio().getId();
            personaService.eliminarDomicilioDePersona(personaId, domicilioId);
            System.out.println("Domicilio eliminado exitosamente y referencia actualizada.");
        } catch (Exception e) {
            System.err.println("Error al eliminar domicilio: " + e.getMessage());
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
     * - crearDomicilioIndependiente(): Para crear domicilio sin asociar
     * - actualizarDomicilioDePersona(): Para agregar domicilio a persona sin domicilio
     *
     * Nota: NO persiste en BD, solo crea el objeto en memoria.
     * El caller es responsable de insertar el domicilio.
     *
     * @return Domicilio nuevo (no persistido, ID=0)
     */
    private Domicilio crearDomicilio() {
        System.out.print("Calle: ");
        String calle = scanner.nextLine().trim();
        System.out.print("Numero: ");
        String numero = scanner.nextLine().trim();
        return new Domicilio(0, calle, numero);
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
    private void actualizarDomicilioDePersona(Persona p) throws Exception {
        if (p.getDomicilio() != null) {
            System.out.print("¿Desea actualizar el domicilio? (s/n): ");
            if (scanner.nextLine().equalsIgnoreCase("s")) {
                System.out.print("Nueva calle (" + p.getDomicilio().getCalle() + "): ");
                String calle = scanner.nextLine().trim();
                if (!calle.isEmpty()) {
                    p.getDomicilio().setCalle(calle);
                }

                System.out.print("Nuevo numero (" + p.getDomicilio().getNumero() + "): ");
                String numero = scanner.nextLine().trim();
                if (!numero.isEmpty()) {
                    p.getDomicilio().setNumero(numero);
                }

                personaService.getDomicilioService().actualizar(p.getDomicilio());
            }
        } else {
            System.out.print("La persona no tiene domicilio. ¿Desea agregar uno? (s/n): ");
            if (scanner.nextLine().equalsIgnoreCase("s")) {
                Domicilio nuevoDom = crearDomicilio();
                personaService.getDomicilioService().insertar(nuevoDom);
                p.setDomicilio(nuevoDom);
            }
        }
    }
}