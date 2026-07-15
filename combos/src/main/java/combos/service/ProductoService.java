package combos.service;

import combos.dto.ProductoVista;
import combos.dto.TallaOpcion;
import combos.model.Producto;
import combos.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class ProductoService {
    @Autowired
    private ProductoRepository productoRepository;

    private static final List<String> ORDEN_TALLAS = List.of("XS", "S", "M", "L", "XL", "XXL");

    public List<Producto> listarTodos() { return productoRepository.findAll(); }
    public Producto buscarPorId(Long id) { return productoRepository.findById(id).orElse(null); }
    public void guardar(Producto p) { productoRepository.save(p); }
    public void eliminar(Long id) { productoRepository.deleteById(id); }

    /**
     * Agrupa las filas de "productos" (cada una es una talla distinta del
     * mismo artículo) en tarjetas únicas por producto, con la lista de tallas
     * disponibles para armar el selector en el catálogo. No requiere tocar
     * la base de datos: usa los mismos campos nombre/genero/tipo que ya tienes.
     */
    public List<ProductoVista> listarAgrupados() {
        LinkedHashMap<String, ProductoVista> grupos = new LinkedHashMap<>();

        for (Producto p : productoRepository.findAll()) {
            String clave = normalizar(p.getNombre()) + "|" + normalizar(p.getGenero()) + "|" + normalizar(p.getTipo());

            ProductoVista vista = grupos.computeIfAbsent(clave, k -> {
                ProductoVista v = new ProductoVista();
                v.setIdReferencia(p.getId());
                v.setNombre(p.getNombre());
                v.setDescription(p.getDescription());
                v.setPrecio(p.getPrecio());
                v.setImagenUrl(p.getImagenUrl());
                v.setGenero(p.getGenero());
                v.setTipo(p.getTipo());
                return v;
            });

            vista.getTallas().add(new TallaOpcion(p.getId(), p.getTalla(), p.getStock(), p.getImagenUrl()));
        }

        List<ProductoVista> resultado = new ArrayList<>(grupos.values());
        for (ProductoVista v : resultado) {
            v.getTallas().sort(Comparator.comparingInt(this::ordenTalla));
        }
        return resultado;
    }

    /**
     * Trae el producto agrupado (todas sus tallas) a partir del id de
     * CUALQUIERA de sus filas/variantes. Se usa para la página de detalle.
     */
    public ProductoVista obtenerGrupoPorId(Long id) {
        Producto base = productoRepository.findById(id).orElse(null);
        if (base == null) return null;

        String claveBuscada = normalizar(base.getNombre()) + "|" + normalizar(base.getGenero()) + "|" + normalizar(base.getTipo());

        return listarAgrupados().stream()
                .filter(v -> (normalizar(v.getNombre()) + "|" + normalizar(v.getGenero()) + "|" + normalizar(v.getTipo())).equals(claveBuscada))
                .findFirst()
                .orElse(null);
    }

    private int ordenTalla(TallaOpcion t) {
        if (t.getTalla() == null) return 99;
        int idx = ORDEN_TALLAS.indexOf(t.getTalla().trim().toUpperCase());
        return idx == -1 ? 99 : idx;
    }

    private String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase();
    }
}