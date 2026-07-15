package combos.repository;

import combos.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {
    List<Venta> findByClienteId(Long clienteId); // Jala el historial de compras de un cliente

    List<Venta> findAllByOrderByFechaDesc(); // Historial del admin: más recientes primero
    List<Venta> findByClienteIdOrderByFechaDesc(Long clienteId); // "Mis pedidos" del cliente: más recientes primero
}