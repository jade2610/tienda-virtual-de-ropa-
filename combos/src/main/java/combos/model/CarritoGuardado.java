package combos.model;

import jakarta.persistence.*;

/**
 * Antes el carrito vivía solo en HttpSession: si el servidor reiniciaba, o
 * si el cliente entraba desde otro dispositivo, el carrito desaparecía.
 * Esta tabla guarda una copia del carrito de cada CLIENTE CON CUENTA (los
 * invitados siguen usando solo la sesión, como antes) para poder
 * restaurarlo al iniciar sesión.
 */
@Entity
@Table(name = "carrito_guardado")
public class CarritoGuardado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(nullable = false)
    private Integer cantidad;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
}
