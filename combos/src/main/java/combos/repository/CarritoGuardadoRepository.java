package combos.repository;

import combos.model.CarritoGuardado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarritoGuardadoRepository extends JpaRepository<CarritoGuardado, Long> {
    List<CarritoGuardado> findByClienteId(Long clienteId);
    void deleteByClienteId(Long clienteId);
}
