package combos.service;

import combos.model.Carrito;
import combos.model.CarritoGuardado;
import combos.model.Producto;
import combos.repository.CarritoGuardadoRepository;
import combos.repository.CarritoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CarritoService {
    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private CarritoGuardadoRepository carritoGuardadoRepository;

    @Autowired
    private ProductoService productoService;

    public List<Carrito> obtenerDetallesDeVenta(Long ventaId) {
        return carritoRepository.findByVentaId(ventaId);
    }

    // NUEVO: Guarda cada línea del carrito (producto, cantidad, precio) ya asociada
    // a la venta confirmada, para que quede registrada en "detalles_pedido".
    public void guardarDetalles(List<Carrito> detalles) {
        carritoRepository.saveAll(detalles);
    }

    // Usado por el dashboard para calcular ingresos reales por categoría de producto.
    public List<Carrito> listarTodosLosDetalles() {
        return carritoRepository.findAll();
    }

    // ---------------------------------------------------------------
    // Carrito persistente para clientes CON CUENTA (los invitados siguen
    // usando solo la sesión). Guarda una copia en BD cada vez que el
    // carrito cambia, así sobrevive a un reinicio del servidor o a que
    // el cliente entre desde otro dispositivo.
    // ---------------------------------------------------------------

    @Transactional
    public void guardarParaCliente(Long clienteId, List<Carrito> carritoActual) {
        carritoGuardadoRepository.deleteByClienteId(clienteId);
        List<CarritoGuardado> filas = new ArrayList<>();
        for (Carrito item : carritoActual) {
            CarritoGuardado fila = new CarritoGuardado();
            fila.setClienteId(clienteId);
            fila.setProductoId(item.getProducto().getId());
            fila.setCantidad(item.getCantidad());
            filas.add(fila);
        }
        carritoGuardadoRepository.saveAll(filas);
    }

    public List<Carrito> restaurarParaCliente(Long clienteId) {
        List<Carrito> resultado = new ArrayList<>();
        for (CarritoGuardado fila : carritoGuardadoRepository.findByClienteId(clienteId)) {
            Producto producto = productoService.buscarPorId(fila.getProductoId());
            if (producto == null) continue; // el producto pudo haberse eliminado desde entonces

            Carrito item = new Carrito();
            item.setProducto(producto);
            item.setCantidad(fila.getCantidad());
            item.setPrecioUnitario(producto.getPrecio()); // precio actual, no el de cuando lo guardó
            resultado.add(item);
        }
        return resultado;
    }

    // Se llama justo al iniciar sesión: junta lo que el cliente tenía guardado
    // de antes con lo que ya llevaba agregado en esta sesión (por si compraba
    // como invitado y decidió loguearse a mitad de camino).
    @Transactional
    public List<Carrito> sincronizarAlLogin(Long clienteId, List<Carrito> carritoSesionActual) {
        List<Carrito> guardado = restaurarParaCliente(clienteId);
        List<Carrito> combinado = carritoSesionActual != null ? new ArrayList<>(carritoSesionActual) : new ArrayList<>();

        for (Carrito item : guardado) {
            boolean existe = false;
            for (Carrito actual : combinado) {
                if (actual.getProducto().getId().equals(item.getProducto().getId())) {
                    actual.setCantidad(actual.getCantidad() + item.getCantidad());
                    existe = true;
                    break;
                }
            }
            if (!existe) combinado.add(item);
        }

        guardarParaCliente(clienteId, combinado);
        return combinado;
    }

    @Transactional
    public void limpiarParaCliente(Long clienteId) {
        carritoGuardadoRepository.deleteByClienteId(clienteId);
    }
}