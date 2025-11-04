package Models;

import java.util.Date;
import java.util.Objects;

/**
 * Entidad que representa un envio en el sistema.
 * Hereda de Base para obtener id y eliminado.
 *
 * Relación con Persona:
 * - Un Pedido puede tener 0 o 1 Envíos
 * - Un Envío estará asociado solo a un Pedido (Unidireccional 1->1)
 *
 * Tabla BD: envios
 * Campos:
 * - id: INT AUTO_INCREMENT PRIMARY KEY (heredado de Base)
 * - numero: VARCHAR(20) NOT NULL
 * - fecha: DATE NOT NULL
 * - cliente_nombre VARCHAR(120) NOT NULL
 * - total DECIMAL(12, 2) NOT NULL
 * - estado ENUM('NUEVO', 'FACTURADO', 'ENVIADO') NOT NUL
 * - eliminado: BOOLEAN DEFAULT FALSE (heredado de Base)
 */
public class Envio extends Base {
    public enum Empresa { ANDREANI, OCA, CORREO_ARG }
    public enum Tipo { ESTANDAR, EXPRES }
    public enum Estado { EN_PREPARACION, EN_TRANSITO, ENTREGADO }

    /**
     * Nombre de la empresa que realizará el envío.
     * Requerido, no puede ser null ni estar vacío.
     */
    private Empresa empresa;

    /**
     * Número de tracking - para poder seguir el envío.
     * Requerido, no puede ser null ni estar vacío.
     */
    private String tracking;

    /**
     * Tipo de envío.
     * Requerido, debe ser uno de los valores del enum.
     */
    private Tipo tipo;

    /**
     * Costo de envío.
     * Requerido, debe ser mayor a cero.
     */
    private Double costo;

    /**
     * Fecha despacho.
     * Requerido.
     */
    private Date fechaDespacho;

    /**
     * Fecha estimada.
     * Requerido.
     */
    private Date fechaEstimada; // nullable

    /**
     * Estado actual del envío.
     * Requerido, debe ser uno de los valores del enum.
     */
    private Estado estado; // nullable


    /**
     * Constructor completo para reconstruir un Domicilio desde la base de datos.
     * Usado por PersonaDAO y DomicilioDAO al mapear ResultSet.
     *
     * @param id ID del domicilio en la BD
     * @param eliminado El registro se encuentra eliminado o no
     * @param tracking Tracking Number del envío
     * @param empresa Nombre de la empresa
     * @param tipo Tipo de Envio
     * @param costo Costo del envío
     * @param fechaDespacho Fecha que se despachó el  envío
     * @param fechaEstimada Fecha en que se estima arribará el envío
     * @param estado Estado actual del envío
     */
    public Envio(int id, boolean eliminado, String tracking, Empresa empresa, Tipo tipo, Double costo,
                 Date fechaDespacho, Date fechaEstimada, Estado estado) {
        super(id, eliminado);
        this.tracking = tracking;
        this.empresa = empresa;
        this.tipo = tipo;
        this.costo = costo;
        this.fechaDespacho = fechaDespacho;
        this.fechaEstimada = fechaEstimada;
        this.estado = estado;
    }

    /**
     * Constructor por defecto para crear un domicilio nuevo.
     * El ID será asignado por la BD al insertar.
     * El flag eliminado se inicializa en false por Base.
     */
    public Envio() {
        super();
    }

    /**
     * Obtiene el nombre de la empresa.
     * @return Nombre de la empresa
     */
    public Empresa getEmpresa() {
        return empresa;
    }

    /**
     * Establece el nombre de la empresa.
     *
     * @param empresa Nuevo nombre de la calle
     */
    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    /**
     * Obtiene el número de tracking.
     * @return Número de tracking
     */
    public String getTracking() {
        return tracking;
    }

    /**
     * Establece el número de tracking.
     *
     * @param tracking Nuevo número
     */
    public void setTracking(String tracking) {
        this.tracking = tracking;
    }

    /**
     * Representación en texto del envío.
     * Útil para debugging y logging.
     *
     * @return String con todos los campos del envío
     */
    @Override
    public String toString() {
        return "Envio{" +
                "id=" + getId() +
                ", eliminado=" + isEliminado() +
                ", tracking='" + tracking + '\'' +
                ", empresa=" + empresa +
                ", tipo=" + tipo +
                ", costo=" + costo +
                ", fechaDespacho=" + fechaDespacho +
                ", fechaEstimada=" + fechaEstimada +
                ", estado=" + estado +
                '}';
    }

    /**
     * Compara dos envios por igualdad SEMÁNTICA.
     * Dos domicilios son iguales si tienen el mismo tracking number y la misma empresa.
     * Nota: NO se compara por ID, permitiendo detectar direcciones duplicadas.
     *
     * @param o Objeto a comparar
     * @return true si los domicilios tienen la misma calle y número
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Envio envio = (Envio) o;
        return Objects.equals(empresa, envio.empresa) &&
               Objects.equals(tracking, envio.tracking);
    }

    /**
     * Calcula el hash code basado en tracking y empresa.
     * Consistente con equals(): envíos con misma tracking/empresa tienen mismo hash.
     *
     * @return Hash code del envío
     */
    @Override
    public int hashCode() {
        return Objects.hash(empresa, tracking);
    }
}