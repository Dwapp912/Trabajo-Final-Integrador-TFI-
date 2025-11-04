package Models;

import java.util.Objects;

/**
 * Entidad que representa un domicilio (dirección) en el sistema.
 * Hereda de Base para obtener id y eliminado.
 *
 * Relación con Persona:
 * - Una Persona puede tener 0 o 1 Domicilio
 * - Un Domicilio puede estar asociado a múltiples Personas (relación N:1 desde Persona)
 *
 * Tabla BD: domicilios
 * Campos:
 * - id: INT AUTO_INCREMENT PRIMARY KEY (heredado de Base)
 * - calle: VARCHAR(100) NOT NULL
 * - numero: VARCHAR(10) NOT NULL
 * - eliminado: BOOLEAN DEFAULT FALSE (heredado de Base)
 */
public class Domicilio extends Base {
    /**
     * Nombre de la calle.
     * Requerido, no puede ser null ni estar vacío.
     */
    private String calle;

    /**
     * Número de la dirección.
     * Puede incluir letras (ej: "123A", "S/N").
     * Requerido, no puede ser null ni estar vacío.
     */
    private String numero;

    /**
     * Constructor completo para reconstruir un Domicilio desde la base de datos.
     * Usado por PersonaDAO y DomicilioDAO al mapear ResultSet.
     *
     * @param id ID del domicilio en la BD
     * @param calle Nombre de la calle
     * @param numero Número de la dirección
     */
    public Domicilio(int id, String calle, String numero) {
        super(id, false); // Llama al constructor de Base con eliminado=false
        this.calle = calle;
        this.numero = numero;
    }

    /**
     * Constructor por defecto para crear un domicilio nuevo.
     * El ID será asignado por la BD al insertar.
     * El flag eliminado se inicializa en false por Base.
     */
    public Domicilio() {
        super();
    }

    /**
     * Obtiene el nombre de la calle.
     * @return Nombre de la calle
     */
    public String getCalle() {
        return calle;
    }

    /**
     * Establece el nombre de la calle.
     * Validación: DomicilioServiceImpl verifica que no esté vacío.
     *
     * @param calle Nuevo nombre de la calle
     */
    public void setCalle(String calle) {
        this.calle = calle;
    }

    /**
     * Obtiene el número de la dirección.
     * @return Número de la dirección
     */
    public String getNumero() {
        return numero;
    }

    /**
     * Establece el número de la dirección.
     * Validación: DomicilioServiceImpl verifica que no esté vacío.
     *
     * @param numero Nuevo número
     */
    public void setNumero(String numero) {
        this.numero = numero;
    }

    /**
     * Representación en texto del domicilio.
     * Útil para debugging y logging.
     *
     * @return String con todos los campos del domicilio
     */
    @Override
    public String toString() {
        return "Domicilio{" +
                "id=" + getId() +
                ", calle='" + calle + '\'' +
                ", numero='" + numero + '\'' +
                ", eliminado=" + isEliminado() +
                '}';
    }

    /**
     * Compara dos domicilios por igualdad SEMÁNTICA.
     * Dos domicilios son iguales si tienen la misma calle y número.
     * Nota: NO se compara por ID, permitiendo detectar direcciones duplicadas.
     *
     * @param o Objeto a comparar
     * @return true si los domicilios tienen la misma calle y número
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Domicilio domicilio = (Domicilio) o;
        return Objects.equals(calle, domicilio.calle) &&
               Objects.equals(numero, domicilio.numero);
    }

    /**
     * Calcula el hash code basado en calle y número.
     * Consistente con equals(): domicilios con misma calle/número tienen mismo hash.
     *
     * @return Hash code del domicilio
     */
    @Override
    public int hashCode() {
        return Objects.hash(calle, numero);
    }
}