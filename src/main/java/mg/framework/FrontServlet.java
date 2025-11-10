package mg.framework;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
// import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.*;
// import java.net.URL;
import java.util.*;
import java.util.logging.*;

// import mg.annotation.Controller;
// import mg.annotation.Route;
// import mg.annotation.RouteMapping;
import mg.util.Scan;

@WebServlet(name = "FrontServlet", urlPatterns = { "/*" }, loadOnStartup = 1)
public class FrontServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(FrontServlet.class.getName());

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();

        Map<String, Method> routeMapping = new HashMap<>();
        Map<Class<?>, Object> controllerInstances = new HashMap<>();

        Scan.scanControllers(context, routeMapping, controllerInstances);

        // Stocker dans le ServletContext pour tout le projet
        context.setAttribute("routeMapping", routeMapping);
        context.setAttribute("controllerInstances", controllerInstances);

        System.out.println("🚀 Framework initialisé avec " + routeMapping.size() + " routes");
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 🔹 Récupérer les maps globales
        Map<String, Method> routeMapping = (Map<String, Method>) getServletContext().getAttribute("routeMapping");
        // Map<Class<?>, Object> controllerInstances = (Map<Class<?>, Object>) getServletContext()
                // .getAttribute("controllerInstances");
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String resourcePath = requestURI.substring(contextPath.length());
        Method mappedMethod = routeMapping.get(resourcePath);
        if (mappedMethod != null) {
            try {
                // Object controller = controllerInstances.get(mappedMethod.getDeclaringClass());
                // Object result = mappedMethod.invoke(controller);
                response.setContentType("text/html;charset=UTF-8");
                PrintWriter out = response.getWriter();
                out.print("Route :<b> "+ requestURI +" </b>");
                out.print("Methode :<b> "+ mappedMethod.getName()+" </b>");
                out.print("Classe :<b>"+ mappedMethod.getDeclaringClass().getSimpleName()+" </b>");
                // out.print(result != null ? result.toString() : "(Aucun contenu)");
                return;
            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println("Erreur dans le contrôleur : " + e.getMessage());
                return;
            }
        }
        // 🔸 Sinon : page non trouvée
        showFrameworkPage(request, response, resourcePath);
    }

    private void showFrameworkPage(HttpServletRequest request, HttpServletResponse response,
            String requestedPath)
            throws IOException {

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html lang='fr'>");
        out.println("<head>");
        out.println("    <meta charset='UTF-8'>");
        out.println("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        out.println("    <title>Framework Java - Page non trouvée</title>");
        out.println("    <style>");
        out.println("        body { font-family: sans-serif; padding: 40px; background: #f0f2f5; color: #333; }");
        out.println(
                "        .container { background: white; padding: 30px; border-radius: 10px; box-shadow: 0 5px 15px rgba(0,0,0,0.1); }");
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");
        out.println("    <div class='container'>");
        out.println("        <h1>Framework Java</h1>");
        out.println("        <h3>Aucune route trouvée pour :</h3>");
        out.println("        <p><code>" + requestedPath + "</code></p>");
        // Map<String, Method> routeMapping = (Map<String, Method>) getServletContext().getAttribute("ROUTE_MAPPING");
        // // Map<Class<?>, Object> controllerInstances = (Map<Class<?>, Object>)
        // // getServletContext()
        // // .getAttribute("CONTROLLER_INSTANCES");
        // String requestURI = request.getRequestURI();
        // String contextPath = request.getContextPath();
        // String resourcePath = requestURI.substring(contextPath.length());
        // out.println("        <p> -------------------->" + routeMapping.get(resourcePath) + "<--------------------</p>");
        out.println("    </div>");
        out.println("</body>");
        out.println("</html>");
    }
}
