package combos.controller;

import combos.model.Carrito;
import combos.model.Cliente;
import combos.model.Venta;
import combos.service.CarritoService;
import combos.service.ClienteService;
import combos.service.LoginAttemptService;
import combos.service.NotificacionService;
import combos.service.ProductoService;
import combos.service.VentaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private ProductoService productoService;
    @Autowired private VentaService ventaService;
    @Autowired private ClienteService clienteService;
    @Autowired private CarritoService carritoService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private NotificacionService notificacionService;
    @Autowired private LoginAttemptService loginAttemptService;

    @GetMapping("/login")
    public String mostrarLogin() {
        return "admin/login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String email, @RequestParam String password, HttpSession session, Model model) {
        if (loginAttemptService.estaBloqueado(email)) {
            model.addAttribute("error", "Demasiados intentos fallidos. Intenta de nuevo en " +
                    loginAttemptService.minutosRestantesDeBloqueo(email) + " minuto(s).");
            return "admin/login";
        }

        Cliente cliente = clienteService.buscarPorEmail(email);

        boolean credencialesValidas = cliente != null
                && "ADMIN".equalsIgnoreCase(cliente.getRol())
                && cliente.getPassword() != null
                && passwordEncoder.matches(password, cliente.getPassword());

        if (credencialesValidas) {
            loginAttemptService.registrarExito(email);
            session.setAttribute("usuarioLogueado", cliente.getEmail());
            return "redirect:/admin/dashboard"; 
        }
        loginAttemptService.registrarFallo(email);
        model.addAttribute("error", "Correo o contraseña incorrectos.");
        return "admin/login";
    }

    @GetMapping("/recuperar")
    public String mostrarRecuperar() {
        return "admin/recuperar";
    }

    @PostMapping("/recuperar")
    public String procesarRecuperar(@RequestParam String email, Model model) {
        Cliente cliente = clienteService.buscarPorEmail(email);

        // Por seguridad, mostramos el mismo mensaje exista o no ese correo como admin
        // (si dijéramos "ese correo no existe" cualquiera podría usarlo para adivinar
        // qué correos SÍ son de administrador).
        if (cliente != null && "ADMIN".equalsIgnoreCase(cliente.getRol())) {
            String passwordTemporal = generarPasswordTemporal();
            cliente.setPassword(passwordEncoder.encode(passwordTemporal));
            clienteService.guardar(cliente);
            notificacionService.enviarContrasenaTemporal(cliente.getEmail(), passwordTemporal);
        }

        model.addAttribute("mensaje", "Si ese correo está registrado como administrador, te enviamos una contraseña temporal.");
        return "admin/recuperar";
    }

    private String generarPasswordTemporal() {
        String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(caracteres.charAt(random.nextInt(caracteres.length())));
        }
        return sb.toString();
    }

    @GetMapping("/dashboard")
    public String mostrarDashboard(HttpSession session, Model model) {
        if (session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/admin/login";
        }

        // 1. OBTENER DATOS  DE TU BASE DE DATOS
        List<Venta> listaVentas = ventaService.listarTodas();
        
        // Sumamos el total de dinero real de la caja
        double cajaTotal = listaVentas.stream().mapToDouble(Venta::getTotal).sum();

        // Contamos el inventario y clientes reales
        int totalPrendas = productoService.listarTodos().size();
        int totalClientes = clienteService.listarTodos().size();

        // 2. DATOS PARA EL GRÁFICO DE LÍNEAS (HISTORIAL DE VENTAS REAL)
        List<String> etiquetasVentas = listaVentas.stream()
                .map(v -> "Venta #" + v.getId())
                .collect(Collectors.toList());

        List<Double> montosVentas = listaVentas.stream()
                .map(Venta::getTotal)
                .collect(Collectors.toList());

        // 3. INGRESOS POR CATEGORÍA - datos reales de detalles_pedido, ya no inventados.
        // Cada línea de detalles_pedido está ligada a la venta y al producto real que se vendió,
        // así que agrupamos por Producto.tipo y sumamos cantidad * precioUnitario de cada línea.
        Map<String, Double> ingresosPorTipo = new LinkedHashMap<>();
        for (Carrito detalle : carritoService.listarTodosLosDetalles()) {
            if (detalle.getVenta() == null || detalle.getProducto() == null) continue; // filas huérfanas de antes del fix
            String tipo = detalle.getProducto().getTipo() != null ? detalle.getProducto().getTipo() : "Otros";
            double subtotal = detalle.getCantidad() * detalle.getPrecioUnitario();
            ingresosPorTipo.merge(tipo, subtotal, Double::sum);
        }

        List<String> categorias = new ArrayList<>(ingresosPorTipo.keySet());
        List<Double> montosPorCategoria = new ArrayList<>(ingresosPorTipo.values());

        // 5. TOP 5 PRODUCTOS MÁS VENDIDOS (por unidades, sumando todas sus tallas)
        Map<String, Integer> unidadesPorProducto = new LinkedHashMap<>();
        for (Carrito detalle : carritoService.listarTodosLosDetalles()) {
            if (detalle.getVenta() == null || detalle.getProducto() == null) continue;
            String nombre = detalle.getProducto().getNombre();
            unidadesPorProducto.merge(nombre, detalle.getCantidad(), Integer::sum);
        }
        List<Map.Entry<String, Integer>> masVendidos = unidadesPorProducto.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(5)
                .collect(Collectors.toList());

        // 6. ALERTA DE STOCK BAJO (menos de 5 unidades)
        var productosStockBajo = productoService.listarTodos().stream()
                .filter(p -> p.getStock() != null && p.getStock() < 5)
                .sorted((a, b) -> a.getStock() - b.getStock())
                .collect(Collectors.toList());

        // 4. ENVIAMOS TODO AL HTML
        model.addAttribute("cajaTotal", cajaTotal);
        model.addAttribute("totalPrendas", totalPrendas);
        model.addAttribute("totalClientes", totalClientes);
        
        // Atributos de los gráficos
        model.addAttribute("categorias", categorias);
        model.addAttribute("valoresVentas", montosPorCategoria);
        model.addAttribute("etiquetasVentas", etiquetasVentas);
        model.addAttribute("montosVentas", montosVentas);
        model.addAttribute("sinDatosPorCategoria", categorias.isEmpty());

        model.addAttribute("masVendidos", masVendidos);
        model.addAttribute("productosStockBajo", productosStockBajo);

        return "admin/dashboard"; 
    }
}