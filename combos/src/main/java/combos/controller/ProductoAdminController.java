package combos.controller;

import combos.model.Producto;
import combos.service.ProductoService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Controller
@RequestMapping("/productos")
public class ProductoAdminController {

    private static final Logger log = LoggerFactory.getLogger(ProductoAdminController.class);

    @Autowired
    private ProductoService productoService;

    @GetMapping
    public String listarProductos(HttpSession session, Model model) {
        if (session.getAttribute("usuarioLogueado") == null) return "redirect:/admin/login";
        model.addAttribute("listaProductos", productoService.listarAgrupados());
        return "admin/productos"; 
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioCrear(@RequestParam(value = "duplicarDe", required = false) Long duplicarDe,
                                          HttpSession session, Model model) {
        if (session.getAttribute("usuarioLogueado") == null) return "redirect:/admin/login";

        if (duplicarDe != null) {
            // Precargamos los datos compartidos (nombre, descripción, precio, género, tipo, imagen)
            // de una prenda ya existente, dejando talla y stock en blanco para la nueva variante.
            Producto base = productoService.buscarPorId(duplicarDe);
            if (base != null) {
                Producto nuevaTalla = new Producto();
                nuevaTalla.setNombre(base.getNombre());
                nuevaTalla.setDescription(base.getDescription());
                nuevaTalla.setPrecio(base.getPrecio());
                nuevaTalla.setGenero(base.getGenero());
                nuevaTalla.setTipo(base.getTipo());
                nuevaTalla.setImagenUrl(base.getImagenUrl());
                model.addAttribute("producto", nuevaTalla);
                model.addAttribute("agregandoTallaDe", base.getNombre());
                return "admin/registrarProducto";
            }
        }

        model.addAttribute("producto", new Producto());
        return "admin/registrarProducto";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, HttpSession session, Model model) {
        if (session.getAttribute("usuarioLogueado") == null) return "redirect:/admin/login";
        model.addAttribute("producto", productoService.buscarPorId(id));
        return "admin/registrarProducto";
    }

    @PostMapping("/guardar")
    public String guardarProducto(@ModelAttribute Producto producto, 
                                  @RequestParam(value = "imagenArchivo", required = false) MultipartFile imagenArchivo,
                                  @RequestParam(value = "imagenUrlTexto", required = false) String imagenUrlTexto,
                                  @RequestParam(value = "tipoImagen", required = false) String tipoImagen,
                                  @RequestParam(value = "accion", defaultValue = "guardar") String accion,
                                  HttpSession session) {
        
        if (session.getAttribute("usuarioLogueado") == null) return "redirect:/admin/login";

        try {
            // Lógica para procesar la imagen (Archivo o URL)
            if ("btnArchivo".equals(tipoImagen) && imagenArchivo != null && !imagenArchivo.isEmpty()) {
                Path directorio = Paths.get("uploads"); // Carpeta en la raíz del proyecto
                if (!Files.exists(directorio)) Files.createDirectories(directorio);
                
                String nombreUnico = UUID.randomUUID().toString() + "_" + imagenArchivo.getOriginalFilename().replace(" ", "_");
                Files.copy(imagenArchivo.getInputStream(), directorio.resolve(nombreUnico), StandardCopyOption.REPLACE_EXISTING);
                
                producto.setImagenUrl("/uploads/" + nombreUnico);
            } 
            else if ("btnUrl".equals(tipoImagen) && imagenUrlTexto != null && !imagenUrlTexto.isEmpty()) {
                producto.setImagenUrl(imagenUrlTexto);
            }
            // Si el producto es nuevo y no tiene imagen, ponemos un placeholder
            else if (producto.getId() == null && (producto.getImagenUrl() == null || producto.getImagenUrl().isEmpty())) {
                producto.setImagenUrl("https://placehold.co/400x500?text=Sin+Foto");
            }

            productoService.guardar(producto);

            // "Guardar y agregar otra talla" -> volvemos al formulario ya precargado con los
            // mismos datos de esta prenda, para que el admin solo tenga que poner talla y stock.
            if ("agregarTalla".equals(accion)) {
                return "redirect:/productos/nuevo?duplicarDe=" + producto.getId();
            }
        } catch (Exception e) {
            log.error("Error al guardar el producto '{}': {}", producto.getNombre(), e.getMessage(), e);
        }
        return "redirect:/productos"; 
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarProducto(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("usuarioLogueado") == null) return "redirect:/admin/login";
        productoService.eliminar(id);
        return "redirect:/productos";
    }
}