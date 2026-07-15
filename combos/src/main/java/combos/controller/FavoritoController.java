package combos.controller;

import combos.service.FavoritoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * Favoritos es 100% para clientes con cuenta (no tiene sentido para un
 * invitado: se perdería al cerrar el navegador). El botón de corazón en el
 * catálogo/detalle usa /favoritos/{id}/alternar por fetch(), sin recargar
 * la página; si no hay sesión, devolvemos requiereLogin=true y el propio
 * JS del front redirige a /login.
 */
@Controller
public class FavoritoController {

    @Autowired
    private FavoritoService favoritoService;

    @PostMapping("/favoritos/{productoId}/alternar")
    @ResponseBody
    public Map<String, Object> alternar(@PathVariable Long productoId, HttpSession session) {
        Object clienteId = session.getAttribute("clienteLogueado");
        if (clienteId == null) {
            return Map.of("requiereLogin", true);
        }
        boolean quedoFavorito = favoritoService.alternar((Long) clienteId, productoId);
        return Map.of("requiereLogin", false, "favorito", quedoFavorito);
    }

    @GetMapping("/favoritos")
    public String misFavoritos(HttpSession session, Model model) {
        Object clienteId = session.getAttribute("clienteLogueado");
        if (clienteId == null) return "redirect:/login";

        model.addAttribute("listaProductos", favoritoService.listarProductosFavoritos((Long) clienteId));
        return "cliente/favoritos";
    }
}
