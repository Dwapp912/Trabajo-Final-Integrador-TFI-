package Models;

import java.time.LocalDate;
import java.util.Date;
import java.util.Objects;


/**
 * Entidad que representa un pedido en el sistema.
 * Hereda de Base para obtener id y eliminado.
 *
 * Relación con Envío:
 * - Un Pedido puede tener 0 o 1 Envío (relación opcional)
 * - La relación se materializa con la FK pedidoId en la tabla envio
 *
 * Tabla BD: pedido
 * Campos:
 * - id: INT AUTO_INCREMENT PRIMARY KEY (heredado de Base)
 * - numero: VARCHAR(20) NOT NULL
 * - fecha: DATE NOT NULL
 * - clienteNombre: VARCHAR(120) NOT NULL
 * - total: DECIMAL(12, 2) NOT NULL
 * - estado ENUM('NUEVO', 'FACTURADO', 'ENVIADO')
 * - eliminado: BOOLEAN DEFAULT FALSE (heredado de Base)
 */
public class Pedido extends Base {

    public Estado getEstado() {
        return estado;
    }

    public enum Estado { NUEVO, FACTURADO, ENVIADO }
    /** Número de pedido. Requerido, no puede ser null ni vacío. */
    private String numero;

    /** Nombre del cliente. Requerido, no puede ser null ni vacío. */
    private String clienteNombre;

    /**
     * Total del pedido. Requerido, no puede ser null.
     * Validado en PedidosServiceImpl para que no sea menor a cero.
     */
    private Double total;
    /**
     * Estado del pedido.
     */
    private Estado estado; //

    /**
     * Fecha del pedido
     */
    private LocalDate fecha;

    /**
     * Envio asociado al pedido.
     * Puede ser null (Pedido sin envío).
     */
    private Envio envio;

    /**
     * Constructor completo para reconstruir un Pedido desde la BD.
     * Usado por PedidoDAO al mapear ResultSet.
     * El envío se asigna posteriormente con setEnvio().
     */
    public Pedido(int id, boolean eliminado, String numero, LocalDate fecha, String clienteNombre,
                  double total, Estado estado, Envio envio) {
        super(id, eliminado);
        this.numero = numero;
        this.fecha = fecha;
        this.clienteNombre = clienteNombre;
        this.total = total;
        this.estado = estado;
        this.envio = envio;
    }

    /** Constructor por defecto para crear un pedido nuevo sin ID. */
    public Pedido() {
        super();
    }

    public String getNumero() {
        return numero;
    }

    /**
     * Establece el número del pedido.
     * Validación: PedidosServiceImpl verifica que no esté vacío.
     */
    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    /**
     * Establece el nombre del cliente del pedido.
     * Validación: PedidosServiceImpl verifica que no esté vacío.
     */
    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public Double getTotal() {
        return total;
    }

    /**
     * Establece el total del pedido.
     * Validación: PersonaServiceImpl verifica que no sea menor a cero.
     */
    public void setTotal(Double total) {
        this.total = total;
    }

    public Envio getEnvio() {
        return envio;
    }

    /**
     * Asocia o desasocia un domicilio a la persona.
     * Si domicilio es null, la FK domicilio_id será NULL en la BD.
     */
    public void setEnvio(Envio envio) {
        this.envio = envio;
    }

    public LocalDate getFecha() {
        return this.fecha;
    }

    @Override
    public String toString() {
        return "Pedido{" +
                "id=" + getId() +
                ", eliminado=" + isEliminado() +
                ", numero='" + numero + '\'' +
                ", fecha=" + fecha +
                ", clienteNombre='" + clienteNombre + '\'' +
                ", total=" + total +
                ", estado=" + estado +
                ", envio=" + (envio != null ? envio.getId() : null) +
                '}';
    }

    /**
     * Compara dos pedidos por número (identificador único).
     * Dos pedidos son iguales si tienen el mismo número.
     * Correcto porque el número de pedido es único en el sistema.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pedido pedido = (Pedido) o;
        return Objects.equals(numero, pedido.numero);
    }

    /**
     * Hash code basado en DNI.
     * Consistente con equals(): personas con mismo DNI tienen mismo hash.
     */
    @Override
    public int hashCode() {
        return Objects.hash(total);
    }
}