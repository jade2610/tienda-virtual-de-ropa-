package combos.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Genera imágenes QR en PNG. Un solo lugar para esta lógica: la usan tanto
 * el PDF del voucher (NotificacionService) como la página web de la boleta
 * (para mostrar el QR también ahí, no solo en el correo).
 */
@Service
public class QrCodeService {

    public byte[] generarPng(String contenido, int tamano) throws WriterException, IOException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matriz = writer.encode(contenido, BarcodeFormat.QR_CODE, tamano, tamano);

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matriz, "PNG", salida);
        return salida.toByteArray();
    }
}
