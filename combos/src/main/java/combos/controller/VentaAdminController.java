package combos.controller;

import combos.model.Carrito;
import combos.model.Producto;
import combos.model.Venta;
import combos.service.CarritoService; // Asegúrate de tener este servicio creado
import combos.service.NotificacionService;
import combos.service.ProductoService;
import combos.service.VentaService; // Asegúrate de tener este servicio creado
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/ventas")
public class VentaAdminController {

    @Autowired
    private VentaService ventaService;

    @Autowired
    private CarritoService carritoService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private NotificacionService notificacionService;

    // Mostrar el historial maestro de ventas
    @GetMapping
    public String listarVentas(HttpSession session, Model model) {
        // Barrera de seguridad
        if (session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/admin/login";
        }
        
        // Enviamos la lista de comprobantes a la vista
        model.addAttribute("listaVentas", ventaService.listarTodas());
        return "admin/ventas"; 
    }

    // Cambiar el estado de un pedido (PENDIENTE, ENVIADO, ENTREGADO, CANCELADO).
    // Si se cancela, devolvemos el stock de cada producto de ese pedido.
    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam String nuevoEstado, HttpSession session) {
        if (session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/admin/login";
        }

        Venta venta = ventaService.buscarPorId(id);
        if (venta == null) return "redirect:/ventas";

        boolean estadoRealmenteCambio = !nuevoEstado.equalsIgnoreCase(venta.getEstado());

        boolean seEstaCancel = "CANCELADO".equalsIgnoreCase(nuevoEstado) && !"CANCELADO".equalsIgnoreCase(venta.getEstado());
        if (seEstaCancel) {
            List<Carrito> detalles = carritoService.obtenerDetallesDeVenta(id);
            for (Carrito detalle : detalles) {
                Producto producto = detalle.getProducto();
                producto.setStock(producto.getStock() + detalle.getCantidad());
                productoService.guardar(producto);
            }
        }

        venta.setEstado(nuevoEstado.toUpperCase());
        ventaService.registrarVenta(venta);

        // Avisamos al cliente por correo, sin bloquear la respuesta si el envío falla o demora.
        if (estadoRealmenteCambio && venta.getCliente() != null && venta.getCliente().getEmail() != null) {
            notificacionService.enviarActualizacionEstado(venta, venta.getCliente().getEmail(), venta.getCliente().getNombre());
        }

        return "redirect:/ventas";
    }
}