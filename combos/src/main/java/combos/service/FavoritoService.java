package combos.service;

import combos.dto.ProductoVista;
import combos.model.Favorito;
import combos.repository.FavoritoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class FavoritoService {

    @Autowired
    private FavoritoRepository favoritoRepository;

    @Autowired
    private ProductoService productoService;

    /**
     * Agrega o quita el favorito según su estado actual. Devuelve true si
     * quedó marcado como favorito, false si quedó sin marcar.
     */
    public boolean alternar(Long clienteId, Long productoId) {
        if (favoritoRepository.existsByClienteIdAndProductoId(clienteId, productoId)) {
            favoritoRepository.deleteByClienteIdAndProductoId(clienteId, productoId);
            return false;
        }
        favoritoRepository.save(new Favorito(clienteId, productoId));
        return true;
    }

    // Ids de producto (idReferencia) que el cliente ya marcó como favorito,
    // para pintar el corazón activo en el catálogo/detalle.
    public Set<Long> idsFavoritosDeCliente(Long clienteId) {
        if (clienteId == null) return Set.of();
        Set<Long> ids = new HashSet<>();
        for (Favorito f : favoritoRepository.findByClienteIdOrderByFechaAgregadoDesc(clienteId)) {
            ids.add(f.getProductoId());
        }
        return ids;
    }

    // Productos completos (agrupados con sus tallas) para la página "Mis Favoritos".
    public List<ProductoVista> listarProductosFavoritos(Long clienteId) {
        List<ProductoVista> resultado = new ArrayList<>();
        for (Favorito f : favoritoRepository.findByClienteIdOrderByFechaAgregadoDesc(clienteId)) {
            ProductoVista vista = productoService.obtenerGrupoPorId(f.getProductoId());
            if (vista != null) resultado.add(vista); // si el producto ya no existe, simplemente lo omitimos
        }
        return resultado;
    }
}
