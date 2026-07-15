package combos.service;

import combos.model.Carrito;
import combos.model.Cliente;
import combos.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Prueba el punto más crítico del negocio: que NUNCA se pueda vender más
 * stock del que realmente existe, y que el stock se descuente correctamente
 * cuando la compra sí es válida. Este es el bug que arreglamos hace un
 * tiempo (antes se podía comprar con stock en 0), así que estas pruebas
 * evitan que alguien lo rompa sin darse cuenta en el futuro.
 */
@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock private ProductoService productoService;
    @Mock private ClienteService clienteService;
    @Mock private VentaService ventaService;
    @Mock private CarritoService carritoService;

    @InjectMocks
    private CheckoutService checkoutService;

    private Producto poloTallaM;

    @BeforeEach
    void setUp() {
        poloTallaM = new Producto();
        poloTallaM.setId(1L);
        poloTallaM.setNombre("Polo Negro");
        poloTallaM.setTalla("M");
        poloTallaM.setPrecio(49.90);
        poloTallaM.setStock(3);
    }

    @Test
    void rechazaLaCompraSiPideMasDeLoQueHayEnStock() {
        when(productoService.buscarPorId(1L)).thenReturn(poloTallaM);

        Carrito itemDelCarrito = new Carrito();
        itemDelCarrito.setProducto(poloTallaM);
        itemDelCarrito.setCantidad(5); // pide 5, pero solo hay 3

        assertThatThrownBy(() ->
                checkoutService.procesar("Ana", "ana@correo.com", "Tarjeta", "Av. Siempre Viva 123", List.of(itemDelCarrito))
        ).isInstanceOf(StockInsuficienteException.class)
         .hasMessageContaining("Solo quedan 3");
    }

    @Test
    void rechazaLaCompraSiElProductoYaNoExiste() {
        when(productoService.buscarPorId(1L)).thenReturn(null);

        Carrito itemDelCarrito = new Carrito();
        itemDelCarrito.setProducto(poloTallaM);
        itemDelCarrito.setCantidad(1);

        assertThatThrownBy(() ->
                checkoutService.procesar("Ana", "ana@correo.com", "Tarjeta", "Av. Siempre Viva 123", List.of(itemDelCarrito))
        ).isInstanceOf(StockInsuficienteException.class);
    }

    @Test
    void permiteLaCompraYDescuentaElStockCuandoSiAlcanza() {
        when(productoService.buscarPorId(1L)).thenReturn(poloTallaM);
        when(clienteService.buscarPorEmail("ana@correo.com")).thenReturn(null);

        Carrito itemDelCarrito = new Carrito();
        itemDelCarrito.setProducto(poloTallaM);
        itemDelCarrito.setCantidad(2); // pide 2, hay 3: debe alcanzar

        CheckoutService.ResultadoCheckout resultado =
                checkoutService.procesar("Ana", "ana@correo.com", "Tarjeta", "Av. Siempre Viva 123", List.of(itemDelCarrito));

        assertThat(resultado.total()).isEqualTo(2 * 49.90);
        assertThat(poloTallaM.getStock()).isEqualTo(1); // 3 - 2 = 1, el stock quedó descontado
    }

    @Test
    void reutilizaElClienteExistenteEnVezDeCrearUnoNuevo() {
        when(productoService.buscarPorId(1L)).thenReturn(poloTallaM);

        Cliente clienteYaRegistrado = new Cliente();
        clienteYaRegistrado.setId(99L);
        clienteYaRegistrado.setEmail("ana@correo.com");
        when(clienteService.buscarPorEmail("ana@correo.com")).thenReturn(clienteYaRegistrado);

        Carrito itemDelCarrito = new Carrito();
        itemDelCarrito.setProducto(poloTallaM);
        itemDelCarrito.setCantidad(1);

        checkoutService.procesar("Ana", "ana@correo.com", "Tarjeta", "Av. Siempre Viva 123", List.of(itemDelCarrito));

        // No debe intentar crear un cliente nuevo si ya existe uno con ese correo
        org.mockito.Mockito.verify(clienteService, org.mockito.Mockito.never()).guardar(any());
    }
}
