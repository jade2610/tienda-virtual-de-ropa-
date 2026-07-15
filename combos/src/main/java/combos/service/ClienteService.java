package combos.service;

import combos.model.Cliente;
import combos.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ClienteService {
    
    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Cliente login(String email, String password) {
        Cliente c = clienteRepository.findByEmail(email);
        if (c != null && c.getPassword() != null && passwordEncoder.matches(password, c.getPassword())) {
            return c;
        }
        return null;
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    public void guardar(Cliente cliente) {
        clienteRepository.save(cliente);
    }

    // --- ¡NUEVO! Permite buscar si el cliente ya compró antes ---
    public Cliente buscarPorEmail(String email) {
        return clienteRepository.findByEmail(email);
    }

    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id).orElse(null);
    }
}