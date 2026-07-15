package combos.controller;

import combos.model.Cliente;
import combos.service.ClienteService;
import combos.service.NotificacionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/clientes")
public class ClienteAdminController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private NotificacionService notificacionService;

    @GetMapping
    public String listarClientes(HttpSession session, Model model) {
        // La barrera de seguridad de siempre
        if (session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/admin/login";
        }
        
        // Enviamos la lista de clientes a la vista
        model.addAttribute("listaClientes", clienteService.listarTodos());
        return "admin/clientes"; 
    }

    @PostMapping("/enviar-promocion")
    public String enviarPromocion(@RequestParam String asunto, @RequestParam String mensaje,
                                   HttpSession session, Model model) {
        if (session.getAttribute("usuarioLogueado") == null) {
            return "redirect:/admin/login";
        }

        List<String> destinatarios = clienteService.listarTodos().stream()
                .filter(c -> "CLIENTE".equalsIgnoreCase(c.getRol()) && c.getEmail() != null)
                .map(Cliente::getEmail)
                .distinct()
                .collect(Collectors.toList());

        notificacionService.enviarPromocion(asunto, mensaje, destinatarios);

        model.addAttribute("listaClientes", clienteService.listarTodos());
        model.addAttribute("mensajePromocion", "Promoción enviada a " + destinatarios.size() + " cliente(s).");
        return "admin/clientes";
    }
}
