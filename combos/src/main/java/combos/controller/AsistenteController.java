package combos.controller;

import combos.service.AsistenteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AsistenteController {

    @Autowired
    private AsistenteService asistenteService;

    public record PreguntaRequest(String pregunta) {}
    public record RespuestaDTO(String respuesta) {}

    @PostMapping("/asistente/preguntar")
    public RespuestaDTO preguntar(@RequestBody PreguntaRequest request) {
        String respuesta = asistenteService.responder(request.pregunta());
        return new RespuestaDTO(respuesta);
    }
}
