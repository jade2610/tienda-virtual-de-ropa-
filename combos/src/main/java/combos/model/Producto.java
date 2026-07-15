package combos.model;

import jakarta.persistence.*;

@Entity
@Table(name = "productos")
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;
    
    @Column(name = "descripcion") 
    private String description;
    
    @Column(nullable = false)
    private Double precio;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private String talla;

    @Column(name = "imagen_url")
    private String imagenUrl;

    @Column(nullable = false)
    private String genero; // Corregido: nombre de variable

    @Column(nullable = false)
    private String tipo;

    public Producto() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }
    
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    
    public String getTalla() { return talla; }
    public void setTalla(String talla) { this.talla = talla; }
    
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    // --- AQUÍ ESTABAN LOS MÉTODOS FALTANTES ---
    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}