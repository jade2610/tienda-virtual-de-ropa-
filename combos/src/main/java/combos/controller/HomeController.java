package combos.controller;

import combos.model.Cliente;
import combos.service.ClienteService;
import combos.service.ProductoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private combos.service.FavoritoService favoritoService;

    // 1. La Portada (Libre, sin cargar base de datos)
    @GetMapping("/")
    public String index() {
        return "index"; 
    }

    // 2. El Catálogo (Va a MySQL, jala la ropa y la manda a la vista)
    @GetMapping("/catalogo")
    public String mostrarCatalogo(@org.springframework.web.bind.annotation.RequestParam(required = false) String tipo,
                                   HttpSession session, Model model) {
        model.addAttribute("listaProductos", productoService.listarAgrupados());
        model.addAttribute("tipoPreseleccionado", tipo);
        agregarNombreClienteSiHaySesion(session, model);
        model.addAttribute("idsFavoritos", favoritoService.idsFavoritosDeCliente(idClienteDeSesion(session)));
        return "cliente/catalogo"; // Busca el archivo en templates/cliente/catalogo.html
    }

    // 3. Detalle de un producto: muestra sus tallas disponibles y el asistente
    @GetMapping("/producto/{id}")
    public String mostrarDetalle(@org.springframework.web.bind.annotation.PathVariable Long id, HttpSession session, Model model) {
        var producto = productoService.obtenerGrupoPorId(id);
        if (producto == null) {
            return "redirect:/catalogo";
        }
        model.addAttribute("producto", producto);
        agregarNombreClienteSiHaySesion(session, model);
        model.addAttribute("esFavorito", favoritoService.idsFavoritosDeCliente(idClienteDeSesion(session)).contains(producto.getIdReferencia()));
        return "cliente/detalleProducto";
    }

    private Long idClienteDeSesion(HttpSession session) {
        Object clienteId = session.getAttribute("clienteLogueado");
        return clienteId == null ? null : (Long) clienteId;
    }

    private void agregarNombreClienteSiHaySesion(HttpSession session, Model model) {
        Object clienteId = session.getAttribute("clienteLogueado");
        if (clienteId != null) {
            Cliente cliente = clienteService.buscarPorId((Long) clienteId);
            if (cliente != null) {
                model.addAttribute("nombreClienteSesion", cliente.getNombre());
            }
        }
    }
}