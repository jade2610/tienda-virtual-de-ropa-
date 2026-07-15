package combos.service;

import combos.model.Carrito;
import combos.model.Venta;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Se encarga de armar el voucher en PDF y enviarlo por correo al cliente
 * apenas se confirma una compra. Cualquier fallo (SMTP caído, credenciales
 * mal puestas, etc.) queda registrado en consola pero NUNCA debe tumbar el
 * flujo de compra: el cliente ya pagó, así que el checkout debe completarse
 * igual aunque el correo no salga.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private QrCodeService qrCodeService;

    @Autowired
    private VentaService ventaService;

    @Autowired
    private CarritoService carritoService;

    @Value("${tienda.nombre:JVXF Boutique}")
    private String nombreTienda;

    @Value("${spring.mail.username:}")
    private String correoRemitente;

    /**
     * Reconstruye el mismo PDF del voucher a partir solo del id de la venta.
     * Es lo que abre el QR al escanearlo: en vez de una página web con los
     * datos sueltos, directamente el comprobante en PDF, como el ticket de
     * un supermercado.
     */
    // Arma el texto plano que va dentro del QR: nombre de la tienda, pedido, fecha,
    // productos y total. Nada de links, así que funciona escaneado desde cualquier
    // celular, esté o no en la misma red que el servidor.
    private String construirTextoQr(Venta venta, List<Carrito> detalles) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        StringBuilder sb = new StringBuilder();
        sb.append(nombreTienda).append("\n");
        sb.append("Pedido #").append(venta.getId()).append(" - ").append(venta.getFecha().format(fmt)).append("\n");
        sb.append("------------------------------\n");
        for (Carrito detalle : detalles) {
            sb.append(detalle.getProducto().getNombre())
              .append(" (").append(detalle.getProducto().getTalla()).append(") x").append(detalle.getCantidad())
              .append(" - S/ ").append(String.format("%.2f", detalle.getCantidad() * detalle.getPrecioUnitario()))
              .append("\n");
        }
        sb.append("------------------------------\n");
        sb.append("TOTAL: S/ ").append(String.format("%.2f", venta.getTotal())).append("\n");
        sb.append("Metodo de pago: ").append(venta.getMetodoPago());
        return sb.toString();
    }

    // Usado por la página web (voucher justo después de comprar, y "Mis Pedidos")
    // para mostrar el mismo QR de texto que lleva el PDF.
    public byte[] generarQrTextoDesdeVenta(Long ventaId) throws Exception {
        Venta venta = ventaService.buscarPorId(ventaId);
        if (venta == null) return null;
        List<Carrito> detalles = carritoService.obtenerDetallesDeVenta(ventaId);
        String texto = construirTextoQr(venta, detalles);
        return qrCodeService.generarPng(texto, 300);
    }

    public byte[] generarVoucherPdfDesdeVenta(Long ventaId) throws DocumentException {
        Venta venta = ventaService.buscarPorId(ventaId);
        if (venta == null) return null;

        List<Carrito> detalles = carritoService.obtenerDetallesDeVenta(ventaId);
        String nombreCliente = venta.getCliente() != null ? venta.getCliente().getNombre() : "Cliente";
        return generarVoucherPdf(venta, detalles, nombreCliente);
    }

    public void enviarConfirmacionCompra(Venta venta, List<Carrito> detalles, String nombreCliente, String emailCliente) {
        try {
            byte[] pdf = generarVoucherPdf(venta, detalles, nombreCliente);

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(emailCliente);
            if (correoRemitente != null && !correoRemitente.isBlank()) {
                helper.setFrom(correoRemitente, nombreTienda);
            }
            helper.setSubject("Confirmación de tu pedido #" + venta.getId() + " - " + nombreTienda);
            helper.setText(construirCuerpoHtml(venta, nombreCliente), true);
            helper.addAttachment("voucher-pedido-" + venta.getId() + ".pdf", () -> new java.io.ByteArrayInputStream(pdf));

            mailSender.send(mensaje);
            log.info("Correo de confirmación enviado a {}", emailCliente);
        } catch (Exception e) {
            // No relanzamos la excepción: la venta ya quedó registrada y el cliente
            // no debe ver un error solo porque el correo no pudo salir.
            log.error("No se pudo enviar el correo de confirmación a {}: {}", emailCliente, e.getMessage());
        }
    }

    public void enviarPromocion(String asunto, String mensaje, List<String> destinatarios) {
        int enviados = 0;
        for (String email : destinatarios) {
            try {
                MimeMessage mimeMensaje = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMensaje, true, "UTF-8");

                helper.setTo(email);
                if (correoRemitente != null && !correoRemitente.isBlank()) {
                    helper.setFrom(correoRemitente, nombreTienda);
                }
                helper.setSubject(asunto);
                helper.setText("""
                        <div style="font-family: Arial, sans-serif; max-width: 500px; margin: auto;">
                            %s
                            <hr/>
                            <p style="font-size:12px;color:#888;">%s - Recibiste este correo porque eres cliente nuestro. Si no quieres recibir más promociones, contáctanos.</p>
                        </div>
                        """.formatted(mensaje, nombreTienda), true);

                mailSender.send(mimeMensaje);
                enviados++;
            } catch (Exception e) {
                log.warn("No se pudo enviar la promoción a {}: {}", email, e.getMessage());
                // Seguimos con el resto de la lista aunque uno falle.
            }
        }
        log.info("Promoción \"{}\" enviada a {} de {} clientes", asunto, enviados, destinatarios.size());
    }

    public void enviarActualizacionEstado(Venta venta, String emailCliente, String nombreCliente) {
        try {
            String estado = venta.getEstado() == null ? "" : venta.getEstado().toUpperCase();

            String tituloEstado;
            String mensajeEstado;
            String colorEstado;
            switch (estado) {
                case "ENVIADO" -> {
                    tituloEstado = "¡Tu pedido ya está en camino! 🚚";
                    mensajeEstado = "Buenas noticias: tu pedido fue despachado y va rumbo a la dirección que registraste.";
                    colorEstado = "#B23A48";
                }
                case "ENTREGADO" -> {
                    tituloEstado = "¡Tu pedido fue entregado! 🎉";
                    mensajeEstado = "Confirmamos que tu pedido llegó a su destino. Esperamos que lo disfrutes.";
                    colorEstado = "#198754";
                }
                case "CANCELADO" -> {
                    tituloEstado = "Tu pedido fue cancelado";
                    mensajeEstado = "Tu pedido fue cancelado. Si el pago ya se había realizado, el stock reservado fue liberado. Si tienes dudas, contáctanos.";
                    colorEstado = "#842029";
                }
                default -> {
                    tituloEstado = "Actualización de tu pedido";
                    mensajeEstado = "El estado de tu pedido cambió a: " + estado + ".";
                    colorEstado = "#422A26";
                }
            }

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(emailCliente);
            if (correoRemitente != null && !correoRemitente.isBlank()) {
                helper.setFrom(correoRemitente, nombreTienda);
            }
            helper.setSubject(tituloEstado + " - Pedido #" + venta.getId() + " - " + nombreTienda);
            helper.setText("""
                    <div style="font-family: Arial, sans-serif; max-width: 500px; margin: auto;">
                        <h2 style="color:%s;">%s</h2>
                        <p>Hola, %s. %s</p>
                        <p style="font-size:13px;color:#666;">Puedes ver el seguimiento completo de tu pedido <strong>#%d</strong> desde "Mis Pedidos" en tu perfil.</p>
                        <hr/>
                        <p style="font-size:12px;color:#888;">%s - Este es un correo automático, por favor no respondas a este mensaje.</p>
                    </div>
                    """.formatted(colorEstado, tituloEstado, nombreCliente, mensajeEstado, venta.getId(), nombreTienda), true);

            mailSender.send(mensaje);
            log.info("Correo de actualización de estado ({}) enviado a {}", estado, emailCliente);
        } catch (Exception e) {
            // El cambio de estado ya quedó guardado en la BD: el correo es un extra,
            // nunca debe bloquear ni revertir la acción del admin.
            log.error("No se pudo enviar el correo de actualización de estado a {}: {}", emailCliente, e.getMessage());
        }
    }

    public void enviarBienvenida(String nombre, String email) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(email);
            if (correoRemitente != null && !correoRemitente.isBlank()) {
                helper.setFrom(correoRemitente, nombreTienda);
            }
            helper.setSubject("¡Bienvenido a " + nombreTienda + "!");
            helper.setText("""
                    <div style="font-family: Arial, sans-serif; max-width: 500px; margin: auto;">
                        <h2 style="color:#333;">¡Hola, %s!</h2>
                        <p>Tu cuenta en %s ya está lista. A partir de ahora, cada vez que compres,
                        tus datos se autocompletarán y podrás ver el historial de tus pedidos
                        desde tu perfil.</p>
                        <p style="font-size:12px;color:#888;">Si tú no creaste esta cuenta, contáctanos.</p>
                    </div>
                    """.formatted(nombre, nombreTienda), true);

            mailSender.send(mensaje);
            log.info("Correo de bienvenida enviado a {}", email);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo de bienvenida a {}: {}", email, e.getMessage());
        }
    }

    public void enviarContrasenaTemporal(String email, String passwordTemporal) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setTo(email);
            if (correoRemitente != null && !correoRemitente.isBlank()) {
                helper.setFrom(correoRemitente, nombreTienda);
            }
            helper.setSubject("Recuperación de contraseña - " + nombreTienda);
            helper.setText("""
                    <div style="font-family: Arial, sans-serif; max-width: 500px; margin: auto;">
                        <h2 style="color:#333;">Recuperación de contraseña</h2>
                        <p>Generamos una contraseña temporal para tu cuenta de administrador:</p>
                        <p style="font-size:20px; font-weight:bold; background:#f4f4f4; padding:10px; text-align:center; border-radius:8px;">%s</p>
                        <p>Inicia sesión con esta contraseña y cámbiala cuanto antes desde tu perfil.</p>
                        <p style="font-size:12px;color:#888;">Si tú no pediste este cambio, contacta al equipo técnico de inmediato.</p>
                    </div>
                    """.formatted(passwordTemporal), true);

            mailSender.send(mensaje);
            log.info("Contraseña temporal enviada a {}", email);
        } catch (Exception e) {
            log.error("No se pudo enviar la contraseña temporal a {}: {}", email, e.getMessage());
        }
    }

    private String construirCuerpoHtml(Venta venta, String nombreCliente) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return """
                <div style="font-family: Arial, sans-serif; max-width: 500px; margin: auto;">
                    <h2 style="color:#333;">¡Gracias por tu compra, %s!</h2>
                    <p>Hemos recibido tu pedido <strong>#%d</strong> correctamente.</p>
                    <p><strong>Fecha:</strong> %s<br/>
                       <strong>Total:</strong> S/ %.2f<br/>
                       <strong>Método de pago:</strong> %s</p>
                    <p>Adjuntamos el voucher en PDF con el detalle de tu pedido.</p>
                    <hr/>
                    <p style="font-size:12px;color:#888;">%s - Este es un correo automático, por favor no respondas a este mensaje.</p>
                </div>
                """.formatted(
                nombreCliente,
                venta.getId(),
                venta.getFecha().format(fmt),
                venta.getTotal(),
                venta.getMetodoPago(),
                nombreTienda
        );
    }

    public byte[] generarVoucherPdf(Venta venta, List<Carrito> detalles, String nombreCliente) throws DocumentException {
        Document documento = new Document(PageSize.A4, 40, 40, 50, 50);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        PdfWriter.getInstance(documento, salida);
        documento.open();

        Font tituloFont = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(30, 30, 30));
        Font normalFont = new Font(Font.HELVETICA, 11);
        Font negritaFont = new Font(Font.HELVETICA, 11, Font.BOLD);
        Font cabeceraTablaFont = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);

        Paragraph titulo = new Paragraph(nombreTienda, tituloFont);
        titulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(titulo);

        Paragraph subtitulo = new Paragraph("Voucher de compra", normalFont);
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        subtitulo.setSpacingAfter(20);
        documento.add(subtitulo);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        documento.add(new Paragraph("Pedido N°: " + venta.getId(), negritaFont));
        documento.add(new Paragraph("Cliente: " + nombreCliente, normalFont));
        documento.add(new Paragraph("Fecha: " + venta.getFecha().format(fmt), normalFont));
        documento.add(new Paragraph("Método de pago: " + venta.getMetodoPago(), normalFont));

        Paragraph espacio = new Paragraph(" ");
        espacio.setSpacingAfter(10);
        documento.add(espacio);

        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{4, 1.5f, 2, 2});

        String[] cabeceras = {"Producto", "Cant.", "P. Unit.", "Subtotal"};
        for (String cabecera : cabeceras) {
            PdfPCell celda = new PdfPCell(new Phrase(cabecera, cabeceraTablaFont));
            celda.setBackgroundColor(new Color(40, 40, 40));
            celda.setPadding(6);
            tabla.addCell(celda);
        }

        for (Carrito detalle : detalles) {
            tabla.addCell(new Phrase(detalle.getProducto().getNombre(), normalFont));
            tabla.addCell(new Phrase(String.valueOf(detalle.getCantidad()), normalFont));
            tabla.addCell(new Phrase(String.format("S/ %.2f", detalle.getPrecioUnitario()), normalFont));
            tabla.addCell(new Phrase(String.format("S/ %.2f", detalle.getCantidad() * detalle.getPrecioUnitario()), normalFont));
        }
        documento.add(tabla);

        // Código QR: en vez de un link (que solo funciona si el celular puede llegar
        // al servidor), contiene directamente el texto del pedido. Cualquier lector
        // de QR lo muestra al instante, sin necesitar red ni que el sitio esté abierto.
        try {
            String textoQr = construirTextoQr(venta, detalles);
            Image qr = generarImagenQr(textoQr);
            qr.setAlignment(Element.ALIGN_CENTER);
            qr.scaleToFit(130, 130);
            documento.add(new Paragraph(" "));
            documento.add(qr);
            Paragraph textoQrLeyenda = new Paragraph("Escanea para ver el detalle de tu pedido", normalFont);
            textoQrLeyenda.setAlignment(Element.ALIGN_CENTER);
            documento.add(textoQrLeyenda);
        } catch (Exception e) {
            log.warn("No se pudo generar el QR para el pedido {}: {}", venta.getId(), e.getMessage());
            // Si el QR falla, seguimos igual: la boleta no debe perderse por esto.
        }

        Paragraph total = new Paragraph("\nTOTAL: S/ " + String.format("%.2f", venta.getTotal()), tituloFont);
        total.setAlignment(Element.ALIGN_RIGHT);
        documento.add(total);

        Paragraph pie = new Paragraph("\n¡Gracias por tu preferencia!", normalFont);
        pie.setAlignment(Element.ALIGN_CENTER);
        documento.add(pie);

        documento.close();
        return salida.toByteArray();
    }

    private Image generarImagenQr(String contenido) throws Exception {
        byte[] png = qrCodeService.generarPng(contenido, 200);
        return Image.getInstance(png);
    }
}
