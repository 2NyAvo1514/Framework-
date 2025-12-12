package mg.framework;

// import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
// import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import mg.attribute.*;
import mg.util.*;

@WebServlet(name = "FrontServlet", urlPatterns = { "/*" }, loadOnStartup = 1)
public class FrontServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();

        Map<String, RouteInfo> routeMapping = new HashMap<>();
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
        Map<String, RouteInfo> routeMapping = (Map<String, RouteInfo>) getServletContext().getAttribute("routeMapping");
        Map<Class<?>, Object> controllerInstances = (Map<Class<?>, Object>) getServletContext()
                .getAttribute("controllerInstances");

        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();
        String resourcePath = requestURI.substring(contextPath.length());

        System.out.println("🔍 Recherche de route pour: " + resourcePath);

        // 🔹 Trouver la route correspondante (support des paramètres dynamiques)
        RouteInfo matchedRoute = findMatchedRoute(resourcePath, routeMapping);

        if (matchedRoute != null) {
            handleMatchedRoute(request, response, resourcePath, matchedRoute, controllerInstances);
        } else {
            showFrameworkPage(request, response, resourcePath);
        }
    }

    /**
     * Trouve une route correspondante dans la map des routes
     */
    private RouteInfo findMatchedRoute(String resourcePath, Map<String, RouteInfo> routeMapping) {
        // 1. Vérifier d'abord la correspondance exacte
        if (routeMapping.containsKey(resourcePath)) {
            return routeMapping.get(resourcePath);
        }

        // 2. Vérifier les routes avec paramètres dynamiques
        for (RouteInfo routeInfo : routeMapping.values()) {
            if (routeInfo.matches(resourcePath)) {
                return routeInfo;
            }
        }

        return null;
    }

    /**
     * Gère une route trouvée
     */
    private void handleMatchedRoute(HttpServletRequest request, HttpServletResponse response,
            String resourcePath, RouteInfo matchedRoute,
            Map<Class<?>, Object> controllerInstances)
            throws IOException, ServletException {

        try {
            Method mappedMethod = matchedRoute.getMethod();
            Object controller = controllerInstances.get(mappedMethod.getDeclaringClass());

            System.out.println("✅ Route trouvée: " + matchedRoute.getPattern());
            System.out.println("📝 Méthode: " + mappedMethod.getDeclaringClass().getSimpleName()
                    + "." + mappedMethod.getName());

            // 🔹 Extraire les paramètres de l'URL si présents
            Map<String, String> urlParams = matchedRoute.extractParameters(resourcePath);
            if (!urlParams.isEmpty()) {
                System.out.println("📊 Paramètres URL: " + urlParams);
                // Stocker les paramètres dans la requête pour utilisation ultérieure
                request.setAttribute("urlParams", urlParams);
            }

            // 🔹 Préparer les arguments pour la méthode
            Object[] methodArgs = prepareMethodArguments(mappedMethod, request, urlParams);

            // 🔹 Invoquer la méthode du contrôleur
            Object result = mappedMethod.invoke(controller, methodArgs);

            // 🔹 Traiter le résultat selon son type
            handleResult(request, response, result, mappedMethod, resourcePath);

        } catch (Exception e) {
            handleControllerError(response, e);
        }
    }

    /**
     * Prépare les arguments pour l'appel de la méthode
     */
    private Object[] prepareMethodArguments(Method method, HttpServletRequest request,
            Map<String, String> urlParams) {
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] args = new Object[paramTypes.length];

        // Pour l'instant, on supporte seulement les méthodes sans paramètres
        // Vous pourrez étendre cette méthode plus tard pour injecter:
        // - HttpServletRequest
        // - HttpServletResponse
        // - Paramètres d'URL (@Param annotation)
        // - Paramètres de requête
        // - etc.

        return args; // Retourne un tableau vide pour méthodes sans paramètres
    }

    /**
     * Traite le résultat retourné par la méthode du contrôleur
     */
    private void handleResult(HttpServletRequest request, HttpServletResponse response,
            Object result, Method method, String resourcePath)
            throws IOException, ServletException {

        Class<?> returnType = method.getReturnType();

        if (returnType.equals(String.class)) {
            handleStringResult(response, (String) result);

        } else if (returnType.equals(ModelView.class)) {
            handleModelViewResult(request, response, (ModelView) result);

        } else if (result != null) {
            // Pour tout autre type d'objet, afficher sa représentation string
            handleObjectResult(response, result);

        } else {
            // Afficher une page de debug pour les méthodes void ou null
            showDebugPage(response, resourcePath, method);
        }
    }

    /**
     * Gère un résultat de type String
     */
    private void handleStringResult(HttpServletResponse response, String result)
            throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.print(result != null ? result : "(Aucun contenu)");
    }

    /**
     * Gère un résultat de type ModelView
     */
    private void handleModelViewResult(HttpServletRequest request, HttpServletResponse response,
            ModelView mv) throws ServletException, IOException {
        String viewPath = "/WEB-INF/" + mv.getView() + ".jsp";

        // Ajouter les données du modèle à la requête
        // if (mv.getData() != null) {
        //     mv.getData().forEach(request::setAttribute);
        // }

        request.getRequestDispatcher(viewPath).forward(request, response);
    }

    /**
     * Gère un résultat de type Object quelconque
     */
    private void handleObjectResult(HttpServletResponse response, Object result)
            throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.print(result.toString());
    }

    /**
     * Affiche une page de debug pour les méthodes sans retour spécifique
     */
    private void showDebugPage(HttpServletResponse response, String resourcePath, Method method)
            throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        out.println("""
                <!DOCTYPE html>
                <html lang='fr'>
                <head>
                    <meta charset='UTF-8'>
                    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
                    <title>Route détectée</title>
                    <style>
                        body {
                            font-family: "Segoe UI", Arial, sans-serif;
                            background: #f7f8fa;
                            color: #333;
                            margin: 0;
                            padding: 40px;
                        }
                        .container {
                            background: white;
                            max-width: 700px;
                            margin: auto;
                            padding: 30px;
                            border-radius: 12px;
                            box-shadow: 0 8px 25px rgba(0,0,0,0.1);
                        }
                        h1 {
                            color: #2b6cb0;
                            text-align: center;
                        }
                        .info {
                            margin-top: 20px;
                            line-height: 1.8;
                            font-size: 1.1em;
                        }
                        code {
                            background: #edf2f7;
                            padding: 3px 6px;
                            border-radius: 4px;
                            color: #2d3748;
                            font-family: Consolas, monospace;
                        }
                        .footer {
                            text-align: center;
                            margin-top: 30px;
                            font-size: 0.9em;
                            color: #666;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>✅ Route détectée</h1>
                        <div class="info">
                            <p><b>Route :</b> <code>%s</code></p>
                            <p><b>Méthode :</b> <code>%s()</code></p>
                            <p><b>Classe :</b> <code>%s</code></p>
                            <p><b>Type de retour :</b> <code>%s</code></p>
                            <p><i>La méthode a été exécutée avec succès mais ne retourne rien.</i></p>
                        </div>
                        <div class="footer">
                            <p>Framework Java — <i>Debug View</i></p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(resourcePath,
                method.getName(),
                method.getDeclaringClass().getSimpleName(),
                method.getReturnType().getSimpleName()));
    }

    /**
     * Gère les erreurs du contrôleur
     */
    private void handleControllerError(HttpServletResponse response, Exception e)
            throws IOException {
        e.printStackTrace();
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("text/html;charset=UTF-8");

        PrintWriter out = response.getWriter();
        out.println("""
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Erreur du contrôleur</title>
                    <style>
                        body { font-family: sans-serif; padding: 40px; }
                        .error { color: #d00; background: #fee; padding: 20px; border-radius: 5px; }
                    </style>
                </head>
                <body>
                    <h1>❌ Erreur dans le contrôleur</h1>
                    <div class="error">
                        <strong>Message :</strong> %s<br>
                        <strong>Type :</strong> %s
                    </div>
                </body>
                </html>
                """.formatted(e.getMessage(), e.getClass().getSimpleName()));
    }

    /**
     * Affiche la page du framework quand aucune route n'est trouvée
     */
    private void showFrameworkPage(HttpServletRequest request, HttpServletResponse response,
            String requestedPath) throws IOException {

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
        out.println("        .routes { margin-top: 20px; }");
        out.println("        .route-item { padding: 8px; border-bottom: 1px solid #eee; }");
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");
        out.println("    <div class='container'>");
        out.println("        <h1>🚫 Route non trouvée</h1>");
        out.println("        <p>La route demandée n'existe pas :</p>");
        out.println("        <p><code>" + requestedPath + "</code></p>");

        // Afficher les routes disponibles
        Map<String, RouteInfo> routeMapping = (Map<String, RouteInfo>) getServletContext().getAttribute("routeMapping");
        if (routeMapping != null && !routeMapping.isEmpty()) {
            out.println("        <div class='routes'>");
            out.println("            <h3>Routes disponibles :</h3>");
            for (String route : routeMapping.keySet()) {
                out.println("            <div class='route-item'><code>" + route + "</code></div>");
            }
            out.println("        </div>");
        }

        out.println("    </div>");
        out.println("</body>");
        out.println("</html>");
    }
}