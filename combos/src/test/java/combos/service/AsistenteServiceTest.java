package combos.service;

import combos.dto.ProductoVista;
import combos.dto.TallaOpcion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsistenteServiceTest {

    @Mock private ProductoService productoService;

    @InjectMocks
    private AsistenteService asistenteService;

    @BeforeEach
    void setUp() {
        ProductoVista polo = new ProductoVista();
        polo.setNombre("Polo Negro");
        polo.setTipo("Polo");
        polo.setGenero("Hombre");
        polo.setPrecio(49.90);
        polo.getTallas().add(new TallaOpcion(1L, "M", 5, null));
        polo.getTallas().add(new TallaOpcion(2L, "L", 0, null)); // agotada

        when(productoService.listarAgrupados()).thenReturn(List.of(polo));
    }

    @Test
    void respondeConUnSaludoSiLePreguntanHola() {
        String respuesta = asistenteService.responder("Hola");
        assertThat(respuesta).containsIgnoringCase("hola");
    }

    @Test
    void diceQueHayStockCuandoLaTallaTieneUnidadesDisponibles() {
        String respuesta = asistenteService.responder("¿tienen el polo negro en talla M?");
        assertThat(respuesta).contains("5 disponibles");
    }

    @Test
    void diceQueEstaAgotadaCuandoLaTallaNoTieneStock() {
        String respuesta = asistenteService.responder("¿tienen el polo negro en talla L?");
        assertThat(respuesta).contains("agotada");
    }

    @Test
    void respondeElPrecioCuandoPreguntanCuantoCuesta() {
        String respuesta = asistenteService.responder("¿cuánto cuesta el polo negro?");
        assertThat(respuesta).contains("49.90");
    }

    @Test
    void noInventaProductosQueNoExistenEnElCatalogo() {
        String respuesta = asistenteService.responder("¿tienen zapatillas de fútbol?");
        assertThat(respuesta.toLowerCase()).doesNotContain("zapatilla");
    }
}
