package combos.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos") // Cabecera del voucher
public class Venta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false) // Jala el ID del Cliente
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private Double total;

    public Venta() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    // Agrégalo debajo de "private Double total;"
    @Column(name = "metodo_pago")
    private String metodoPago;

    // Y al final del archivo, agrega sus Getters y Setters:
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    @Column(nullable = false)
    private String estado = "PENDIENTE"; // PENDIENTE, ENVIADO, ENTREGADO, CANCELADO

    @Column(name = "direccion_entrega")
    private String direccionEntrega;

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getDireccionEntrega() { return direccionEntrega; }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }
}