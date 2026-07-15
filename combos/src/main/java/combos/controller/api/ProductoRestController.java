package combos.controller.api;

import combos.dto.ProductoVista;
import combos.model.Producto;
import combos.service.ProductoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST del catálogo. Pensada para que cualquier cliente externo
 * (una app móvil, otro sistema, un frontend distinto a este mismo proyecto)
 * pueda consumir el catálogo sin depender de las páginas Thymeleaf.
 *
 * Lectura (GET): pública, cualquiera puede consultar el catálogo.
 * Escritura (POST/PUT/DELETE): protegida con el mismo esquema de sesión
 * que usa el panel de administración (no se duplicó lógica de login nueva).
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoRestController {

    @Autowired
    private ProductoService productoService;

    // GET /api/productos            -> todas las filas (variantes) tal cual están en BD
    @GetMapping
    public List<Producto> listarTodos() {
        return productoService.listarTodos();
    }

    // GET /api/productos/agrupados  -> catálogo agrupado por prenda, con sus tallas (igual que /catalogo)
    @GetMapping("/agrupados")
    public List<ProductoVista> listarAgrupados() {
        return productoService.listarAgrupados();
    }

    // GET /api/productos/5          -> una variante puntual
    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerPorId(@PathVariable Long id) {
        Producto producto = productoService.buscarPorId(id);
        return producto != null ? ResponseEntity.ok(producto) : ResponseEntity.notFound().build();
    }

    // GET /api/productos/agrupados/5 -> el producto agrupado (todas sus tallas) a partir del id de cualquiera de sus filas
    @GetMapping("/agrupados/{id}")
    public ResponseEntity<ProductoVista> obtenerGrupoPorId(@PathVariable Long id) {
        ProductoVista vista = productoService.obtenerGrupoPorId(id);
        return vista != null ? ResponseEntity.ok(vista) : ResponseEntity.notFound().build();
    }

    // POST /api/productos           -> crear una nueva variante/talla (requiere sesión de admin)
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Producto producto, HttpSession session) {
        ResponseEntity<?> noAutorizado = validarSesionAdmin(session);
        if (noAutorizado != null) return noAutorizado;

        producto.setId(null); // por si mandan un id, ignoramos: esto siempre es un alta nueva
        productoService.guardar(producto);
        return ResponseEntity.status(HttpStatus.CREATED).body(producto);
    }

    // PUT /api/productos/5          -> actualizar una variante existente (requiere sesión de admin)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody Producto datos, HttpSession session) {
        ResponseEntity<?> noAutorizado = validarSesionAdmin(session);
        if (noAutorizado != null) return noAutorizado;

        Producto existente = productoService.buscarPorId(id);
        if (existente == null) return ResponseEntity.notFound().build();

        datos.setId(id);
        productoService.guardar(datos);
        return ResponseEntity.ok(datos);
    }

    // DELETE /api/productos/5       -> eliminar una variante (requiere sesión de admin)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id, HttpSession session) {
        ResponseEntity<?> noAutorizado = validarSesionAdmin(session);
        if (noAutorizado != null) return noAutorizado;

        if (productoService.buscarPorId(id) == null) return ResponseEntity.notFound().build();
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> validarSesionAdmin(HttpSession session) {
        if (session.getAttribute("usuarioLogueado") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Debes iniciar sesión como administrador para hacer esto."));
        }
        return null;
    }
}
