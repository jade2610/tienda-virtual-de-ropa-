package combos.controller;

import combos.model.Carrito;
import combos.model.Cliente;
import combos.model.Producto;
import combos.model.Venta;
import combos.service.CarritoService;
import combos.service.CheckoutService;
import combos.service.ClienteService;
import combos.service.NotificacionService;
import combos.service.ProductoService;
import combos.service.StockInsuficienteException;
import combos.service.VentaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/carrito")
public class CarritoController {

    @Autowired private ProductoService productoService;
    @Autowired private VentaService ventaService;
    @Autowired private ClienteService clienteService;
    @Autowired private CarritoService carritoService;
    @Autowired private NotificacionService notificacionService;
    @Autowired private CheckoutService checkoutService;

    @GetMapping("/ver")
    public String verCarrito(HttpSession session, Model model) {
        List<Carrito> miCarrito = (List<Carrito>) session.getAttribute("carrito");
        if (miCarrito == null) {
            miCarrito = new ArrayList<>();
        }

        double total = 0;
        for (Carrito detalle : miCarrito) {
            total += detalle.getCantidad() * detalle.getPrecioUnitario();
        }

        model.addAttribute("detalles", miCarrito);
        model.addAttribute("total", total);
        
        return "cliente/carrito"; 
    }

    @PostMapping("/agregar/{id}")
    public String agregarAlCarrito(@PathVariable("id") Long id, @RequestParam(defaultValue = "1") int cantidad, HttpSession session) {
        Producto producto = productoService.buscarPorId(id);

        if (producto != null) {
            List<Carrito> miCarrito = (List<Carrito>) session.getAttribute("carrito");
            if (miCarrito == null) {
                miCarrito = new ArrayList<>();
            }

            boolean existe = false;
            for (Carrito detalle : miCarrito) {
                if (detalle.getProducto().getId().equals(producto.getId())) {
                    detalle.setCantidad(detalle.getCantidad() + cantidad);
                    existe = true;
                    break;
                }
            }

            if (!existe) {
                Carrito detalle = new Carrito();
                detalle.setProducto(producto);
                detalle.setPrecioUnitario(producto.getPrecio());
                detalle.setCantidad(cantidad); 
                miCarrito.add(detalle);
            }

            session.setAttribute("carrito", miCarrito);

            // Si el cliente tiene cuenta y sesión iniciada, guardamos una copia en BD
            // para que sobreviva a un reinicio del servidor o a un cambio de dispositivo.
            Object clienteId = session.getAttribute("clienteLogueado");
            if (clienteId != null) {
                carritoService.guardarParaCliente((Long) clienteId, miCarrito);
            }
        }
        return "redirect:/catalogo"; 
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarDelCarrito(@PathVariable Long id, HttpSession session) {
        List<Carrito> miCarrito = (List<Carrito>) session.getAttribute("carrito");
        
        if (miCarrito != null) {
            miCarrito.removeIf(detalle -> detalle.getProducto().getId().equals(id));
            session.setAttribute("carrito", miCarrito);

            Object clienteId = session.getAttribute("clienteLogueado");
            if (clienteId != null) {
                carritoService.guardarParaCliente((Long) clienteId, miCarrito);
            }
        }
        return "redirect:/carrito/ver"; 
    }

    @GetMapping("/checkout")
    public String mostrarCheckout(HttpSession session, Model model) {
        List<Carrito> miCarrito = (List<Carrito>) session.getAttribute("carrito");
        
        if (miCarrito == null || miCarrito.isEmpty()) {
            return "redirect:/catalogo";
        }

        double total = 0;
        for (Carrito detalle : miCarrito) {
            total += detalle.getCantidad() * detalle.getPrecioUnitario();
        }

        model.addAttribute("detalles", miCarrito);
        model.addAttribute("total", total);

        // Si el cliente ya tiene cuenta y sesión iniciada, autocompletamos sus datos:
        // son suyos, guardados en nuestra propia BD, sin depender de ningún servicio externo.
        Object clienteId = session.getAttribute("clienteLogueado");
        if (clienteId != null) {
            Cliente cliente = clienteService.buscarPorId((Long) clienteId);
            if (cliente != null) {
                model.addAttribute("nombreAutocompletado", cliente.getNombre());
                model.addAttribute("emailAutocompletado", cliente.getEmail());
            }
        }
        
        return "cliente/checkout"; 
    }

    @PostMapping("/procesar")
    public String procesarCompra(@RequestParam String nombre, @RequestParam String email, 
                                 @RequestParam(defaultValue = "Tarjeta de Crédito/Débito") String metodoPago,
                                 @RequestParam String direccion,
                                 HttpSession session, Model model) {
        List<Carrito> miCarrito = (List<Carrito>) session.getAttribute("carrito");
        
        if (miCarrito == null || miCarrito.isEmpty()) {
            return "redirect:/catalogo";
        }

        CheckoutService.ResultadoCheckout resultado;
        try {
            resultado = checkoutService.procesar(nombre, email, metodoPago, direccion, miCarrito);
        } catch (StockInsuficienteException e) {
            // No se registró nada: mostramos el motivo y devolvemos al cliente al checkout
            // con su carrito intacto para que ajuste la cantidad.
            double total = 0;
            for (Carrito detalle : miCarrito) {
                total += detalle.getCantidad() * detalle.getPrecioUnitario();
            }
            model.addAttribute("error", e.getMessage());
            model.addAttribute("detalles", miCarrito);
            model.addAttribute("total", total);
            return "cliente/checkout";
        }

        Venta nuevaVenta = resultado.venta();
        List<Carrito> detalles = resultado.detalles();

        // Enviamos el correo de confirmación con el voucher en PDF adjunto.
        // Si falla (SMTP no configurado, etc.) no interrumpe la compra.
        notificacionService.enviarConfirmacionCompra(nuevaVenta, detalles, nombre, email);

        model.addAttribute("nombreCliente", nombre);
        model.addAttribute("emailCliente", email);
        model.addAttribute("detalles", detalles);
        model.addAttribute("total", resultado.total());
        model.addAttribute("numeroPedido", nuevaVenta.getId());

        session.removeAttribute("carrito");

        Object clienteId = session.getAttribute("clienteLogueado");
        if (clienteId != null) {
            carritoService.limpiarParaCliente((Long) clienteId);
        }
        
        return "cliente/voucher"; 
    }
}