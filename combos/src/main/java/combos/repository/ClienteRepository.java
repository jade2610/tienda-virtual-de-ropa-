package combos.repository;

import combos.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Cliente findByEmail(String email); // Consulta personalizada para verificar credenciales
    boolean existsByRol(String rol);
}