package Main;

/**
 * Clase utilitaria para mostrar el menú de la aplicación.
 * Solo contiene métodos estáticos de visualización (no tiene estado).
 *
 * Responsabilidades:
 * - Mostrar el menú principal con todas las opciones disponibles
 * - Formatear la salida de forma consistente
 *
 * Patrón: Utility class (solo métodos estáticos, no instanciable)
 *
 * IMPORTANTE: Esta clase NO lee entrada del usuario.
 * Solo muestra el menú. AppMenu es responsable de leer la opción.
 */
public class MenuDisplay {
    /**
     * Muestra el menú principal con todas las opciones CRUD.
     *
     * Opciones de Personas (1-4):
     * 1. Crear persona: Permite crear persona con domicilio opcional
     * 2. Listar personas: Lista todas o busca por nombre/apellido
     * 3. Actualizar persona: Actualiza datos de persona y opcionalmente su domicilio
     * 4. Eliminar persona: Soft delete de persona (NO elimina domicilio asociado)
     *
     * Opciones de Domicilios (5-10):
     * 5. Crear domicilio: Crea domicilio independiente (sin asociar a persona)
     * 6. Listar domicilios: Lista todos los domicilios activos
     * 7. Actualizar domicilio por ID: Actualiza domicilio directamente (afecta a TODAS las personas)
     * 8. Eliminar domicilio por ID: PELIGROSO - puede dejar FKs huérfanas (RN-029)
     * 9. Actualizar domicilio por ID de persona: Busca persona primero, luego actualiza su domicilio
     * 10. Eliminar domicilio por ID de persona: SEGURO - actualiza FK primero, luego elimina (RN-029)
     *
     * Opción de salida:
     * 0. Salir: Termina la aplicación
     *
     * Formato:
     * - Separador visual "========= MENU ========="
     * - Lista numerada clara
     * - Prompt "Ingrese una opcion: " sin salto de línea (espera input)
     *
     * Nota: Los números de opción corresponden al switch en AppMenu.processOption().
     */
    public static void mostrarMenuPrincipal() {
        System.out.println("\n========= MENU =========");
        System.out.println("1. Crear persona");
        System.out.println("2. Listar personas");
        System.out.println("3. Actualizar persona");
        System.out.println("4. Eliminar persona");
        System.out.println("5. Crear domicilio");
        System.out.println("6. Listar domicilios");
        System.out.println("7. Actualizar domicilio por ID");
        System.out.println("8. Eliminar domicilio por ID");
        System.out.println("9. Actualizar domicilio por ID de persona");
        System.out.println("10. Eliminar domicilio por ID de persona");
        System.out.println("0. Salir");
        System.out.print("Ingrese una opcion: ");
    }
}