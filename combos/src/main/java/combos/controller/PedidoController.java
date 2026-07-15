package combos.controller;

import combos.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * A esto apunta el QR impreso en la boleta: en vez de abrir una página web
 * con los datos del pedido, descarga/abre directamente el voucher en PDF
 * (el mismo que se manda por correo), como el ticket digital de un
 * supermercado. No requiere sesión ni login: cualquiera con el link (o el
 * QR físico) puede verlo, igual que pasaría con un ticket de papel.
 */
@RestController
public class PedidoController {

    private static final Logger log = LoggerFactory.getLogger(PedidoController.class);

    @Autowired private NotificacionService notificacionService;

    // Lo que abre el QR: el voucher en PDF de ese pedido.
    @GetMapping("/pedido/{id}")
    public ResponseEntity<byte[]> descargarVoucher(@PathVariable Long id) {
        try {
            byte[] pdf = notificacionService.generarVoucherPdfDesdeVenta(id);
            if (pdf == null) return ResponseEntity.notFound().build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add("Content-Disposition", "inline; filename=voucher-pedido-" + id + ".pdf");
            return new ResponseEntity<>(pdf, headers, 200);
        } catch (Exception e) {
            log.error("No se pudo generar el voucher del pedido {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Imagen PNG del QR con el texto del pedido (no un link: funciona sin importar
    // en qué red esté el celular que lo escanea).
    @GetMapping("/pedido/{id}/qr")
    public ResponseEntity<byte[]> qrDelPedido(@PathVariable Long id) {
        try {
            byte[] png = notificacionService.generarQrTextoDesdeVenta(id);
            if (png == null) return ResponseEntity.notFound().build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            return new ResponseEntity<>(png, headers, 200);
        } catch (Exception e) {
            log.warn("No se pudo generar el QR para el pedido {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
