package combos.service;

import combos.model.Venta;
import combos.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class VentaService {
    
    @Autowired
    private VentaRepository ventaRepository;

    // NUEVO: Método para que el Administrador vea todo el historial en la tabla
    // (ordenado del pedido más reciente al más antiguo)
    public List<Venta> listarTodas() { 
        return ventaRepository.findAllByOrderByFechaDesc(); 
    }

    // Tus métodos originales intactos:
    public void registrarVenta(Venta v) { 
        ventaRepository.save(v); 
    }
    
    public List<Venta> historialCliente(Long clienteId) { 
        return ventaRepository.findByClienteIdOrderByFechaDesc(clienteId); 
    }
    
    public Venta buscarPorId(Long id) { 
        return ventaRepository.findById(id).orElse(null); 
    }
}