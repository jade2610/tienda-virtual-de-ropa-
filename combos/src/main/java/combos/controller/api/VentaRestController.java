package combos.controller.api;

import combos.model.Venta;
import combos.service.VentaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Solo lectura, y solo para administradores: expone el historial de ventas
 * para quien quiera construir un dashboard externo o integrarlo con otro
 * sistema, sin tener que raspar las páginas Thymeleaf.
 */
@RestController
@RequestMapping("/api/ventas")
public class VentaRestController {

    @Autowired
    private VentaService ventaService;

    @GetMapping
    public ResponseEntity<?> listarVentas(HttpSession session) {
        if (session.getAttribute("usuarioLogueado") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Debes iniciar sesión como administrador para ver las ventas."));
        }
        List<Venta> ventas = ventaService.listarTodas();
        return ResponseEntity.ok(ventas);
    }
}
