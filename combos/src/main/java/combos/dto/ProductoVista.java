package combos.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Agrupa todas las filas de "productos" que representan el mismo artículo
 * (mismo nombre, género y tipo) en una sola tarjeta con selector de tallas,
 * para que el cliente no vea la misma prenda repetida una vez por talla.
 */
public class ProductoVista {
    private Long idReferencia; // id de una de sus filas (cualquiera), usado solo para armar el link de detalle
    private String nombre;
    private String description;
    private Double precio;
    private String imagenUrl;
    private String genero;
    private String tipo;
    private final List<TallaOpcion> tallas = new ArrayList<>();

    public Long getIdReferencia() { return idReferencia; }
    public void setIdReferencia(Long idReferencia) { this.idReferencia = idReferencia; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrecio() { return precio; }
    public void setPrecio(Double precio) { this.precio = precio; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public List<TallaOpcion> getTallas() { return tallas; }

    // Primera talla con stock > 0. La usamos como selección por defecto en el catálogo.
    public TallaOpcion getPrimeraDisponible() {
        return tallas.stream().filter(TallaOpcion::isDisponible).findFirst().orElse(null);
    }

    public boolean isSinStock() {
        return getPrimeraDisponible() == null;
    }
}
