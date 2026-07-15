package combos.service;

import combos.dto.ProductoVista;
import combos.dto.TallaOpcion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Asistente "casero": no usa ningún modelo de IA externo ni tiene costo.
 * Reconoce patrones frecuentes de preguntas (tallas, stock, precio, tipos de
 * prenda) y responde consultando directamente el catálogo real en base de
 * datos, así que nunca inventa productos ni precios.
 *
 * Si más adelante quieres reemplazarlo por un chatbot con OpenAI, esta clase
 * queda como el "fallback" gratuito para cuando no haya presupuesto de API.
 */
@Service
public class AsistenteService {

    @Autowired
    private ProductoService productoService;

    private static final Set<String> TIPOS_CONOCIDOS = Set.of("polo", "pantalon", "casaca", "sueter", "chompa");
    private static final Pattern PATRON_TALLA = Pattern.compile("\\b(xs|s|m|l|xl|xxl)\\b");

    public String responder(String preguntaOriginal) {
        if (preguntaOriginal == null || preguntaOriginal.isBlank()) {
            return "¿En qué puedo ayudarte? Puedes preguntarme por ejemplo: \"¿tienen polos en talla M?\" o \"¿cuánto cuesta la casaca negra?\"";
        }

        String pregunta = normalizar(preguntaOriginal);
        List<ProductoVista> catalogo = productoService.listarAgrupados();

        if (esSaludo(pregunta)) {
            return "¡Hola! Soy el asistente de JVXF Boutique 👋. Puedo ayudarte a revisar tallas, stock y precios. "
                    + "Prueba con algo como \"¿tienen pantalones en talla L?\" o \"¿cuánto cuesta el polo blanco?\"";
        }

        if (contieneAlguna(pregunta, "gracias", "chau", "adios", "hasta luego", "nos vemos")) {
            return "¡Con gusto! Si necesitas algo más sobre tallas, stock o precios, aquí estaré 😊";
        }

        if (contieneAlguna(pregunta, "mi pedido", "mi compra", "donde esta mi", "estado de mi pedido", "ya salio", "cuando llega")) {
            return "Para ver el estado de tu pedido (pendiente, enviado o entregado), entra a \"Mis Pedidos\" desde tu perfil, "
                    + "ahí vas a ver el progreso de cada compra que hayas hecho con tu cuenta.";
        }

        if (contieneAlguna(pregunta, "que prendas tienen", "que productos tienen", "que ropa tienen", "que tienen en tienda", "catalogo", "que venden")) {
            if (catalogo.isEmpty()) return "Por ahora no tengo productos cargados en el catálogo.";
            return "Esto es lo que tenemos disponible ahora mismo:\n" +
                    catalogo.stream().map(ProductoVista::getNombre).distinct().collect(Collectors.joining("\n"));
        }

        if (contieneAlguna(pregunta, "recomienda", "recomiendas", "recomendacion", "sugerencia", "sugiereme", "que me sugieres")) {
            List<ProductoVista> conStock = catalogo.stream().filter(p -> !p.isSinStock()).collect(Collectors.toList());
            if (conStock.isEmpty()) return "Ahora mismo no tengo productos con stock disponible para recomendarte, lo siento.";
            return "Te podrían gustar estas prendas que tenemos con buen stock:\n" +
                    conStock.stream().limit(3)
                            .map(p -> p.getNombre() + " - S/ " + String.format("%.2f", p.getPrecio()))
                            .collect(Collectors.joining("\n"));
        }

        String tallaMencionada = buscarTalla(pregunta);
        String tipoMencionado = buscarTipo(pregunta);

        boolean preguntaStock = contieneAlguna(pregunta, "stock", "hay", "tienen", "queda", "disponib", "talla", "tallas");
        boolean preguntaPrecio = contieneAlguna(pregunta, "precio", "cuesta", "vale", "cuanto");

        // Intentamos matchear por nombre de producto mencionado en la pregunta
        List<ProductoVista> coincidenciasPorNombre = catalogo.stream()
                .filter(p -> contienePalabraDe(pregunta, normalizar(p.getNombre())))
                .collect(Collectors.toList());

        List<ProductoVista> candidatos = !coincidenciasPorNombre.isEmpty()
                ? coincidenciasPorNombre
                : (tipoMencionado != null
                        ? catalogo.stream().filter(p -> normalizar(p.getTipo()).contains(tipoMencionado)).collect(Collectors.toList())
                        : List.of());

        if (preguntaPrecio && !candidatos.isEmpty()) {
            return candidatos.stream()
                    .map(p -> p.getNombre() + ": S/ " + String.format("%.2f", p.getPrecio()))
                    .distinct()
                    .collect(Collectors.joining("\n"));
        }

        if ((preguntaStock || tallaMencionada != null) && !candidatos.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ProductoVista p : candidatos) {
                List<TallaOpcion> tallas = p.getTallas();
                if (tallaMencionada != null) {
                    tallas = tallas.stream()
                            .filter(t -> t.getTalla() != null && t.getTalla().equalsIgnoreCase(tallaMencionada))
                            .collect(Collectors.toList());
                    if (tallas.isEmpty()) {
                        sb.append(p.getNombre()).append(": no manejamos la talla ").append(tallaMencionada.toUpperCase()).append(".\n");
                        continue;
                    }
                }
                String detalle = tallas.stream()
                        .map(t -> t.getTalla() + (t.isDisponible() ? " (" + t.getStock() + " disponibles)" : " (agotada)"))
                        .collect(Collectors.joining(", "));
                sb.append(p.getNombre()).append(": ").append(detalle).append("\n");
            }
            return sb.toString().trim();
        }

        if (!candidatos.isEmpty()) {
            // Encontramos el producto pero no quedó claro qué quiere saber: mandamos resumen
            ProductoVista p = candidatos.get(0);
            String tallasDisponibles = p.getTallas().stream()
                    .filter(TallaOpcion::isDisponible)
                    .map(TallaOpcion::getTalla)
                    .collect(Collectors.joining(", "));
            return p.getNombre() + " cuesta S/ " + String.format("%.2f", p.getPrecio())
                    + (tallasDisponibles.isEmpty() ? ". Actualmente sin stock." : ". Tallas disponibles: " + tallasDisponibles + ".");
        }

        if (tipoMencionado != null) {
            return "Por ahora no encontré " + tipoMencionado + "s" + (tallaMencionada != null ? " en talla " + tallaMencionada.toUpperCase() : "") + " en el catálogo.";
        }

        return "No estoy seguro de haber entendido 🤔. Puedes preguntarme por tallas, stock o precios de un producto, "
                + "por ejemplo: \"¿tienen polos en talla M?\" o \"¿cuánto cuesta la casaca negra?\"";
    }

    private boolean esSaludo(String pregunta) {
        return contieneAlguna(pregunta, "hola", "buenas", "buenos dias", "buenas tardes", "buenas noches");
    }

    private boolean contieneAlguna(String texto, String... palabras) {
        for (String palabra : palabras) {
            if (texto.contains(palabra)) return true;
        }
        return false;
    }

    private boolean contienePalabraDe(String pregunta, String nombreProducto) {
        for (String palabra : nombreProducto.split("\\s+")) {
            if (palabra.length() > 3 && pregunta.contains(palabra)) return true;
        }
        return false;
    }

    private String buscarTipo(String pregunta) {
        for (String tipo : TIPOS_CONOCIDOS) {
            if (pregunta.contains(tipo)) return tipo;
        }
        return null;
    }

    private String buscarTalla(String pregunta) {
        Matcher m = PATRON_TALLA.matcher(pregunta);
        return m.find() ? m.group(1) : null;
    }

    // Quita tildes y pasa a minúsculas para que "pantalón"/"pantalon" o "Cuánto"/"cuanto" matcheen igual
    private String normalizar(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase().trim();
    }
}
