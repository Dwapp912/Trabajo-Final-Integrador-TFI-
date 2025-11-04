package Models;

import java.util.Objects;

/**
 * Entidad que representa una persona en el sistema.
 * Hereda de Base para obtener id y eliminado.
 *
 * Relación con Domicilio:
 * - Una Persona puede tener 0 o 1 Domicilio (relación opcional)
 * - Se relaciona mediante FK domicilio_id en la tabla personas
 *
 * Tabla BD: personas
 * Campos:
 * - id: INT AUTO_INCREMENT PRIMARY KEY (heredado de Base)
 * - nombre: VARCHAR(50) NOT NULL
 * - apellido: VARCHAR(50) NOT NULL
 * - dni: VARCHAR(20) NOT NULL UNIQUE (regla de negocio RN-001)
 * - domicilio_id: INT NULL (FK a domicilios)
 * - eliminado: BOOLEAN DEFAULT FALSE (heredado de Base)
 */
public class Persona extends Base {
    /** Nombre de la persona. Requerido, no puede ser null ni vacío. */
    private String nombre;

    /** Apellido de la persona. Requerido, no puede ser null ni vacío. */
    private String apellido;

    /**
     * DNI de la persona. Requerido, no puede ser null ni vacío.
     * ÚNICO en el sistema (validado en BD y en PersonaServiceImpl.validateDniUnique()).
     */
    private String dni;

    /**
     * Domicilio asociado a la persona.
     * Puede ser null (persona sin domicilio).
     * Se carga mediante LEFT JOIN en PersonaDAO.
     */
    private Domicilio domicilio;

    /**
     * Constructor completo para reconstruir una Persona desde la BD.
     * Usado por PersonaDAO al mapear ResultSet.
     * El domicilio se asigna posteriormente con setDomicilio().
     */
    public Persona(int id, String nombre, String apellido, String dni) {
        super(id, false);
        this.nombre = nombre;
        this.apellido = apellido;
        this.dni = dni;
    }

    /** Constructor por defecto para crear una persona nueva sin ID. */
    public Persona() {
        super();
    }

    public String getNombre() {
        return nombre;
    }

    /**
     * Establece el nombre de la persona.
     * Validación: PersonaServiceImpl verifica que no esté vacío.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    /**
     * Establece el apellido de la persona.
     * Validación: PersonaServiceImpl verifica que no esté vacío.
     */
    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getDni() {
        return dni;
    }

    /**
     * Establece el DNI de la persona.
     * Validación: PersonaServiceImpl verifica que sea único en insert/update.
     */
    public void setDni(String dni) {
        this.dni = dni;
    }

    public Domicilio getDomicilio() {
        return domicilio;
    }

    /**
     * Asocia o desasocia un domicilio a la persona.
     * Si domicilio es null, la FK domicilio_id será NULL en la BD.
     */
    public void setDomicilio(Domicilio domicilio) {
        this.domicilio = domicilio;
    }

    @Override
    public String toString() {
        return "Persona{" +
                "id=" + getId() +
                ", nombre='" + nombre + '\'' +
                ", apellido='" + apellido + '\'' +
                ", dni='" + dni + '\'' +
                ", domicilio=" + domicilio +
                ", eliminado=" + isEliminado() +
                '}';
    }

    /**
     * Compara dos personas por DNI (identificador único).
     * Dos personas son iguales si tienen el mismo DNI.
     * Correcto porque DNI es único en el sistema.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Persona persona = (Persona) o;
        return Objects.equals(dni, persona.dni);
    }

    /**
     * Hash code basado en DNI.
     * Consistente con equals(): personas con mismo DNI tienen mismo hash.
     */
    @Override
    public int hashCode() {
        return Objects.hash(dni);
    }
}