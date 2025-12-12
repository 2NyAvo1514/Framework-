package mg.util;

import java.io.File;
import java.lang.reflect.Method;
// import java.net.URL;
import java.util.ArrayList;
// import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mg.annotation.Controller;
import mg.annotation.RouteMapping;
import jakarta.servlet.ServletContext;

public class Scan {

    /**
     * 🔹 Scanne le dossier des classes pour trouver les contrôleurs
     * annotés @Controller
     * et leurs méthodes annotées @RouteMapping.
     * 
     * @param context             le ServletContext du projet web (pour accéder à
     *                            /WEB-INF/classes)
     * @param routeMapping        Map des URL → méthodes
     * @param controllerInstances Map des classes → instances
     */
    public static void scanControllers(ServletContext context,
            Map<String, RouteInfo> routeMapping,
            Map<Class<?>, Object> controllerInstances) {
        try {
            // 📁 Récupérer le chemin absolu vers /WEB-INF/classes
            String basePath = context.getRealPath("/WEB-INF/classes");
            if (basePath == null) {
                System.err.println("❌ Impossible de déterminer le chemin /WEB-INF/classes");
                return;
            }

            File baseDir = new File(basePath);
            if (!baseDir.exists()) {
                System.err.println("❌ Répertoire inexistant : " + baseDir.getAbsolutePath());
                return;
            }

            System.out.println("📦 Démarrage du scan des contrôleurs dans : " + baseDir.getAbsolutePath());

            // 🔁 Scan récursif
            scanFolder(baseDir, "", routeMapping, controllerInstances);

            System.out.println("✅ Scan terminé. Routes détectées : " + routeMapping.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 🔁 Fonction récursive pour parcourir les dossiers et charger les classes.
     */
    private static void scanFolder(File folder,
            String packageName,
            Map<String, RouteInfo> routeMapping,
            Map<Class<?>, Object> controllerInstances) {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                scanFolder(file, packageName + file.getName() + ".", routeMapping, controllerInstances);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + file.getName().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className, false, Thread.currentThread().getContextClassLoader());

                    if (clazz.isAnnotationPresent(Controller.class)) {
                        Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                        controllerInstances.put(clazz, controllerInstance);

                        for (Method method : clazz.getDeclaredMethods()) {
                            // Dans la méthode scanFolder :
                            if (method.isAnnotationPresent(RouteMapping.class)) {
                                String urlPattern = method.getAnnotation(RouteMapping.class).url();

                                // Extraire les noms de paramètres
                                // List<String> paramNames = new ArrayList<>();
                                // java.util.regex.Pattern paramPattern = java.util.regex.Pattern.compile("\\{([^}]+)\\}");
                                // java.util.regex.Matcher matcher = paramPattern.matcher(urlPattern);

                                // while (matcher.find()) {
                                //     paramNames.add(matcher.group(1));
                                // }

                                // Créer l'objet RouteInfo
                                RouteInfo routeInfo = new RouteInfo(urlPattern, method, null);

                                // Stocker dans la Map (changer le type de routeMapping)
                                // Map<String, RouteInfo> routeMapping au lieu de Map<String, Method>
                                routeMapping.put(urlPattern, routeInfo);

                                System.out.println(
                                        "➡️  " + urlPattern + " → " + clazz.getSimpleName() + "." + method.getName());
                            }
                        }
                    }

                } catch (ClassNotFoundException e) {
                    // Classe introuvable
                } catch (Throwable t) {
                    System.err.println("⚠️ Erreur sur la classe : " + className);
                    t.printStackTrace();
                }
            }
        }
    }

    private static List<String> extractParamNames(String pattern) {
        List<String> paramNames = new ArrayList<>();
        Pattern paramPattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = paramPattern.matcher(pattern);

        while (matcher.find()) {
            paramNames.add(matcher.group(1));
        }

        return paramNames;
    }

    private static String convertToRegex(String pattern) {
        // Remplace {param} par ([^/]+) pour capturer la valeur
        String regex = pattern.replaceAll("\\{([^}]+)\\}", "([^/]+)");
        // Échapper les slashes pour la regex
        regex = "^" + regex.replace("/", "\\/") + "$";
        return regex;
    }
}
