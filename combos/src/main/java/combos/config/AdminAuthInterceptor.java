package combos.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Antes cada @Controller de admin repetía manualmente:
 *   if (session.getAttribute("usuarioLogueado") == null) return "redirect:/admin/login";
 * al inicio de cada método. Funcionaba, pero bastaba con olvidar copiar esa
 * línea en un controller nuevo para dejar una ruta admin abierta sin querer.
 *
 * Este interceptor revisa la sesión UNA sola vez, de forma centralizada,
 * para cualquier ruta protegida. Los controllers pueden dejar sus propios
 * checks como respaldo (no está de más), pero ya no dependen de que nadie
 * se olvide de ponerlos.
 */
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Object usuario = request.getSession().getAttribute("usuarioLogueado");
        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/admin/login");
            return false;
        }
        return true;
    }
}
