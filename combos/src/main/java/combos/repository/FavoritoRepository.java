package combos.repository;

import combos.model.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {
    List<Favorito> findByClienteIdOrderByFechaAgregadoDesc(Long clienteId);
    Optional<Favorito> findByClienteIdAndProductoId(Long clienteId, Long productoId);
    boolean existsByClienteIdAndProductoId(Long clienteId, Long productoId);
    void deleteByClienteIdAndProductoId(Long clienteId, Long productoId);
}
