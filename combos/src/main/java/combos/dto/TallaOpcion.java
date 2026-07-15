package combos.dto;

/**
 * Representa una fila real de "productos" (que en tu BD equivale a una talla
 * específica) pero vista desde el catálogo como una opción seleccionable
 * dentro de la tarjeta de un producto agrupado.
 */
public class TallaOpcion {
    private Long id;       // id real del Producto (esa fila = esa talla)
    private String talla;
    private Integer stock;
    private String imagenUrl; // foto propia de esa talla/modelado, si el admin subió una distinta

    public TallaOpcion(Long id, String talla, Integer stock, String imagenUrl) {
        this.id = id;
        this.talla = talla;
        this.stock = stock;
        this.imagenUrl = imagenUrl;
    }

    public Long getId() { return id; }
    public String getTalla() { return talla; }
    public Integer getStock() { return stock; }
    public String getImagenUrl() { return imagenUrl; }
    public boolean isDisponible() { return stock != null && stock > 0; }
}
