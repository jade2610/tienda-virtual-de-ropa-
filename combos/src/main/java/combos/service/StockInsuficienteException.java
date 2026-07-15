package combos.service;

/**
 * Se lanza cuando un cliente intenta comprar más unidades de las que
 * realmente hay en stock. Se captura en el controller para mostrar el
 * mensaje en el checkout, sin registrar la venta.
 */
public class StockInsuficienteException extends RuntimeException {
    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
