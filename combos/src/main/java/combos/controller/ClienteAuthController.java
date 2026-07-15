package combos.controller;

import combos.model.Carrito;
import combos.model.Cliente;
import combos.model.Venta;
import combos.service.CarritoService;
import combos.service.ClienteService;
import combos.service.LoginAttemptService;
import combos.service.NotificacionService;
import combos.service.VentaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Cuentas de cliente, separadas por completo del login de administrador
 * (usa su propia clave de sesión "clienteLogueado", nunca "usuarioLogueado").
 * Es 100% opcional: quien no quiera crear cuenta sigue comprando como
 * invitado, igual que siempre.
 */
@Controller
public class ClienteAuthController {

    @Autowired private ClienteService clienteService;
    @Autowired private VentaService ventaService;
    @Autowired private CarritoService carritoService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private NotificacionService notificacionService;
    @Autowired private LoginAttemptService loginAttemptService;

    @GetMapping("/registro")
    public String mostrarRegistro() {
        return "cliente/registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@RequestParam String nombre, @RequestParam String email,
                                    @RequestParam String password, HttpSession session, Model model) {

        Cliente existente = clienteService.buscarPorEmail(email);

        if (existente != null && "ADMIN".equalsIgnoreCase(existente.getRol())) {
            model.addAttribute("error", "Ese correo ya está en uso.");
            return "cliente/registro";
        }

        Cliente cliente;
        if (existente != null) {
            // Ya existía como "invitado" (compró antes sin cuenta): lo convertimos en cuenta real,
            // así el historial de esas compras anteriores queda ligado automáticamente.
            if (existente.getPassword() != null) {
                model.addAttribute("error", "Ese correo ya tiene una cuenta. Inicia sesión en vez de registrarte.");
                return "cliente/registro";
            }
            cliente = existente;
            cliente.setNombre(nombre);
        } else {
            cliente = new Cliente();
            cliente.setNombre(nombre);
            cliente.setEmail(email);
            cliente.setRol("CLIENTE");
        }

        cliente.setPassword(passwordEncoder.encode(password));
        clienteService.guardar(cliente);

        notificacionService.enviarBienvenida(cliente.getNombre(), cliente.getEmail());

        session.setAttribute("clienteLogueado", cliente.getId());
        return "redirect:/catalogo";
    }

    @GetMapping("/login")
    public String mostrarLogin() {
        return "cliente/loginCliente";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String email, @RequestParam String password,
                                 HttpSession session, Model model) {
        if (loginAttemptService.estaBloqueado(email)) {
            model.addAttribute("error", "Demasiados intentos fallidos. Intenta de nuevo en " +
                    loginAttemptService.minutosRestantesDeBloqueo(email) + " minuto(s).");
            return "cliente/loginCliente";
        }

        Cliente cliente = clienteService.login(email, password);

        if (cliente == null || "ADMIN".equalsIgnoreCase(cliente.getRol())) {
            loginAttemptService.registrarFallo(email);
            model.addAttribute("error", "Correo o contraseña incorrectos.");
            return "cliente/loginCliente";
        }

        loginAttemptService.registrarExito(email);
        session.setAttribute("clienteLogueado", cliente.getId());

        // Restauramos su carrito guardado (si tenía) y lo fusionamos con lo que
        // ya llevaba en esta sesión (por si compraba como invitado y a mitad
        // de camino decidió loguearse).
        List<Carrito> carritoActual = (List<Carrito>) session.getAttribute("carrito");
        List<Carrito> combinado = carritoService.sincronizarAlLogin(cliente.getId(), carritoActual);
        session.setAttribute("carrito", combinado);

        return "redirect:/catalogo";
    }

    @GetMapping("/perfil")
    public String mostrarPerfil(HttpSession session, Model model) {
        Object clienteId = session.getAttribute("clienteLogueado");
        if (clienteId == null) return "redirect:/login";

        Cliente cliente = clienteService.buscarPorId((Long) clienteId);
        if (cliente == null) return "redirect:/login";

        model.addAttribute("cliente", cliente);
        return "cliente/perfil";
    }

    @PostMapping("/perfil/cambiar-password")
    public String cambiarPassword(@RequestParam String passwordActual, @RequestParam String passwordNueva,
                                   @RequestParam String passwordNuevaConfirmar, HttpSession session, Model model) {
        Object clienteId = session.getAttribute("clienteLogueado");
        if (clienteId == null) return "redirect:/login";

        Cliente cliente = clienteService.buscarPorId((Long) clienteId);
        if (cliente == null) return "redirect:/login";

        model.addAttribute("cliente", cliente);

        if (cliente.getPassword() == null || !passwordEncoder.matches(passwordActual, cliente.getPassword())) {
            model.addAttribute("error", "Tu contraseña actual no es correcta.");
            return "cliente/perfil";
        }
        if (!passwordNueva.equals(passwordNuevaConfirmar)) {
            model.addAttribute("error", "La nueva contraseña no coincide con la confirmación.");
            return "cliente/perfil";
        }
        if (passwordNueva.length() < 6) {
            model.addAttribute("error", "La nueva contraseña debe tener al menos 6 caracteres.");
            return "cliente/perfil";
        }

        cliente.setPassword(passwordEncoder.encode(passwordNueva));
        clienteService.guardar(cliente);
        model.addAttribute("mensaje", "Tu contraseña se actualizó correctamente.");
        return "cliente/perfil";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("clienteLogueado");
        return "redirect:/catalogo";
    }

    @GetMapping("/recuperar")
    public String mostrarRecuperar() {
        return "cliente/recuperarCliente";
    }

    @PostMapping("/recuperar")
    public String procesarRecuperar(@RequestParam String email, Model model) {
        Cliente cliente = clienteService.buscarPorEmail(email);

        // Mismo mensaje exista o no la cuenta, para no revelar qué correos están registrados.
        if (cliente != null && "CLIENTE".equalsIgnoreCase(cliente.getRol()) && cliente.getPassword() != null) {
            String passwordTemporal = generarPasswordTemporal();
            cliente.setPassword(passwordEncoder.encode(passwordTemporal));
            clienteService.guardar(cliente);
            notificacionService.enviarContrasenaTemporal(cliente.getEmail(), passwordTemporal);
        }

        model.addAttribute("mensaje", "Si ese correo tiene una cuenta, te enviamos una contraseña temporal.");
        return "cliente/recuperarCliente";
    }

    private String generarPasswordTemporal() {
        String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(caracteres.charAt(random.nextInt(caracteres.length())));
        }
        return sb.toString();
    }

    @GetMapping("/mis-pedidos")
    public String misPedidos(HttpSession session, Model model) {
        Object clienteId = session.getAttribute("clienteLogueado");
        if (clienteId == null) {
            return "redirect:/login";
        }

        List<Venta> pedidos = ventaService.historialCliente((Long) clienteId);

        // Armamos cada pedido junto con sus productos, para no tener que consultar
        // la BD de nuevo desde el HTML.
        List<PedidoConDetalle> pedidosConDetalle = new ArrayList<>();
        for (Venta venta : pedidos) {
            List<Carrito> detalles = carritoService.obtenerDetallesDeVenta(venta.getId());
            pedidosConDetalle.add(new PedidoConDetalle(venta, detalles));
        }
        // Los más recientes primero
        pedidosConDetalle.sort((a, b) -> b.venta().getId().compareTo(a.venta().getId()));

        model.addAttribute("pedidos", pedidosConDetalle);
        return "cliente/misPedidos";
    }

    public record PedidoConDetalle(Venta venta, List<Carrito> detalles) {}
}
