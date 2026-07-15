package combos.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Le dice a Spring Boot: "Todo lo que empiece con /uploads/, búscalo en la carpeta local 'uploads'"
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Protege TODAS las rutas administrativas en un solo lugar, sin depender
        // de que cada controller repita el mismo chequeo de sesión.
        registry.addInterceptor(new AdminAuthInterceptor())
                .addPathPatterns("/admin/**", "/productos/**", "/ventas/**", "/clientes/**")
                .excludePathPatterns("/admin/login", "/admin/recuperar"); // login y recuperar contraseña tienen que ser públicos
    }
}