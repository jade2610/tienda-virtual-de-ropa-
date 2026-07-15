package combos.config;

import combos.model.Cliente;
import combos.repository.ClienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Antes el login de admin eran 2 strings quemados en el código
 * (admin@jvxf.com / admin123), sin pasar por la base de datos.
 *
 * Ahora el admin es un registro real en la tabla "usuarios" (rol = ADMIN)
 * con la contraseña encriptada. Como no hay ninguna herramienta de
 * migración (Flyway/Liquibase) para insertar ese primer registro, este
 * CommandLineRunner lo crea automáticamente la primera vez que arranca
 * el proyecto, si todavía no existe ningún admin.
 */
@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    @Autowired private ClienteRepository clienteRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (clienteRepository.existsByRol("ADMIN")) {
            return; // Ya hay un admin, no hacemos nada
        }

        Cliente admin = new Cliente();
        admin.setNombre("Administrador");
        admin.setEmail("admin@jvxf.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRol("ADMIN");
        clienteRepository.save(admin);

        log.warn("========================================================");
        log.warn(" Se creó un usuario ADMIN por defecto:");
        log.warn("   email: admin@jvxf.com");
        log.warn("   password: admin123");
        log.warn(" Por seguridad, entra y cambia esta contraseña cuanto antes.");
        log.warn("========================================================");
    }
}
