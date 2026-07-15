package combos.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protección básica contra fuerza bruta en los logins (admin y cliente).
 * Es en memoria (no en BD): suficiente para frenar intentos automatizados
 * simples; si el servidor reinicia, los contadores se reinician también.
 * Para algo más robusto en producción real, se movería a Redis o a una
 * tabla en BD, pero esto ya cierra el hueco más obvio.
 */
@Service
public class LoginAttemptService {

    private static final int MAXIMO_INTENTOS = 5;
    private static final long BLOQUEO_MINUTOS = 5;

    private final ConcurrentHashMap<String, Intentos> registro = new ConcurrentHashMap<>();

    private static class Intentos {
        int fallos = 0;
        Instant bloqueadoHasta = null;
    }

    public boolean estaBloqueado(String email) {
        Intentos i = registro.get(clave(email));
        if (i == null || i.bloqueadoHasta == null) return false;
        if (Instant.now().isAfter(i.bloqueadoHasta)) {
            registro.remove(clave(email)); // ya pasó el bloqueo, empezamos de cero
            return false;
        }
        return true;
    }

    public long minutosRestantesDeBloqueo(String email) {
        Intentos i = registro.get(clave(email));
        if (i == null || i.bloqueadoHasta == null) return 0;
        long segundos = Instant.now().until(i.bloqueadoHasta, java.time.temporal.ChronoUnit.SECONDS);
        return Math.max(1, segundos / 60 + 1);
    }

    public void registrarFallo(String email) {
        Intentos i = registro.computeIfAbsent(clave(email), k -> new Intentos());
        i.fallos++;
        if (i.fallos >= MAXIMO_INTENTOS) {
            i.bloqueadoHasta = Instant.now().plusSeconds(BLOQUEO_MINUTOS * 60);
        }
    }

    public void registrarExito(String email) {
        registro.remove(clave(email));
    }

    private String clave(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
