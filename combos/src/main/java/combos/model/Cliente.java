package combos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "usuarios") // Mapea la tabla 'usuarios'
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    // @JsonIgnore: aunque ya está encriptado con BCrypt, el hash de la contraseña
    // nunca debería viajar en una respuesta JSON (por ejemplo, /api/ventas incluye
    // el Cliente completo de cada venta).
    @JsonIgnore
    @Column
    private String password;

    @Column(nullable = false)
    private String rol; // Guarda "ADMIN" o "CLIENTE"

    public Cliente() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}