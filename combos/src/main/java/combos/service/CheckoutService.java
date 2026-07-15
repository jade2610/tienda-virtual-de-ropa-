package combos.service;

import combos.model.Carrito;
import combos.model.Cliente;
import combos.model.Producto;
import combos.model.Venta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Antes, el checkout guardaba la venta pero NUNCA descontaba el stock de
 * Producto, así que un artículo se podía vender infinitas veces aunque no
 * quedaran unidades reales. Este servicio arregla eso:
 *
 * 1. Vuelve a leer el stock actual de cada producto desde la BD (nunca confía
 *    en lo que el navegador tenía guardado en la sesión, para evitar que
 *    alguien compre con datos viejos).
 * 2. Si algo no alcanza, lanza StockInsuficienteException y NO registra nada.
 * 3. Si todo alcanza, guarda cliente, venta, detalles y descuenta el stock,
 *    todo dentro de una sola transacción (@Transactional): si algo falla a
 *    mitad de camino, se revierte todo — nunca queda una venta a medias.
 */
@Service
public class CheckoutService {

    @Autowired private ProductoService productoService;
    @Autowired private ClienteService clienteService;
    @Autowired private VentaService ventaService;
    @Autowired private CarritoService carritoService;

    @Transactional
    public ResultadoCheckout procesar(String nombre, String email, String metodoPago, String direccionEntrega, List<Carrito> itemsSesion) {

        // 1. Releemos cada producto desde la BD (fuente de verdad, no la sesión) y validamos stock
        List<Carrito> detallesValidados = new ArrayList<>();
        double total = 0;

        for (Carrito item : itemsSesion) {
            Producto productoActual = productoService.buscarPorId(item.getProducto().getId());
            if (productoActual == null) {
                throw new StockInsuficienteException("El producto \"" + item.getProducto().getNombre() + "\" ya no está disponible.");
            }
            if (productoActual.getStock() == null || productoActual.getStock() < item.getCantidad()) {
                int disponibles = productoActual.getStock() == null ? 0 : productoActual.getStock();
                throw new StockInsuficienteException(
                        "Solo quedan " + disponibles + " unidades de \"" + productoActual.getNombre() +
                        "\" (talla " + productoActual.getTalla() + "), pero pediste " + item.getCantidad() + ".");
            }

            Carrito detalle = new Carrito();
            detalle.setProducto(productoActual);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(productoActual.getPrecio()); // precio real y actual, no el guardado en sesión
            detallesValidados.add(detalle);

            total += item.getCantidad() * productoActual.getPrecio();
        }

        // 2. Cliente (nuevo o existente)
        Cliente clienteActual = clienteService.buscarPorEmail(email);
        if (clienteActual == null) {
            clienteActual = new Cliente();
            clienteActual.setNombre(nombre);
            clienteActual.setEmail(email);
            clienteActual.setRol("CLIENTE");
            clienteService.guardar(clienteActual);
        }

        // 3. Venta
        Venta venta = new Venta();
        venta.setTotal(total);
        venta.setFecha(LocalDateTime.now());
        venta.setCliente(clienteActual);
        venta.setMetodoPago(metodoPago);
        venta.setDireccionEntrega(direccionEntrega);
        venta.setEstado("PENDIENTE");
        ventaService.registrarVenta(venta);

        // 4. Detalles ligados a la venta + descuento real del stock
        for (Carrito detalle : detallesValidados) {
            detalle.setVenta(venta);

            Producto producto = detalle.getProducto();
            producto.setStock(producto.getStock() - detalle.getCantidad());
            productoService.guardar(producto);
        }
        carritoService.guardarDetalles(detallesValidados);

        return new ResultadoCheckout(venta, detallesValidados, total);
    }

    public record ResultadoCheckout(Venta venta, List<Carrito> detalles, double total) {}
}
