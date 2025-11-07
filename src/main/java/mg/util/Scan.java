package mg.util;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import mg.annotation.Controller;
import mg.annotation.RouteMapping;
import jakarta.servlet.ServletContext;

public class Scan {

    /**
     * 🔹 Scanne le dossier des classes pour trouver les contrôleurs annotés @Controller
     *     et leurs méthodes annotées @RouteMapping.
     * 
     * @param context le ServletContext du projet web (pour accéder à /WEB-INF/classes)
     * @param routeMapping Map des URL → méthodes
     * @param controllerInstances Map des classes → instances
     */
    public static void scanControllers(ServletContext context,
                                       Map<String, Method> routeMapping,
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
                                   Map<String, Method> routeMapping,
                                   Map<Class<?>, Object> controllerInstances) {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                scanFolder(file, packageName + file.getName() + ".", routeMapping, controllerInstances);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + file.getName().replace(".class", "");
                try {
                    // 🧠 Charger la classe avec le classloader du projet web
                    Class<?> clazz = Class.forName(className, false, Thread.currentThread().getContextClassLoader());

                    if (clazz.isAnnotationPresent(Controller.class)) {
                        Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                        controllerInstances.put(clazz, controllerInstance);

                        // 🔍 Chercher les méthodes annotées @RouteMapping
                        for (Method method : clazz.getDeclaredMethods()) {
                            if (method.isAnnotationPresent(RouteMapping.class)) {
                                String url = method.getAnnotation(RouteMapping.class).url();
                                routeMapping.put(url, method);
                                System.out.println("➡️  " + url + " → " + clazz.getSimpleName() + "." + method.getName());
                            }
                        }
                    }

                } catch (ClassNotFoundException e) {
                    // Classe introuvable (souvent normal)
                } catch (Throwable t) {
                    System.err.println("⚠️ Erreur sur la classe : " + className);
                    t.printStackTrace();
                }
            }
        }
    }
}
