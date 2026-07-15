package combos.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Un "me gusta" de un cliente sobre un producto. producto_id apunta a
 * cualquier fila/talla de ese artículo (igual que idReferencia en
 * ProductoVista): alcanza para reconstruir el grupo completo con
 * ProductoService.obtenerGrupoPorId().
 */
@Entity
@Table(name = "favoritos")
public class Favorito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "fecha_agregado", nullable = false)
    private LocalDateTime fechaAgregado;

    public Favorito() {}

    public Favorito(Long clienteId, Long productoId) {
        this.clienteId = clienteId;
        this.productoId = productoId;
        this.fechaAgregado = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }
    public LocalDateTime getFechaAgregado() { return fechaAgregado; }
    public void setFechaAgregado(LocalDateTime fechaAgregado) { this.fechaAgregado = fechaAgregado; }
}
