package mg.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe qui représente une route avec ses informations complètes
 */
public class RouteInfo {
    private String pattern; // Pattern original, ex: "/str/{str}"
    private Method method; // Méthode à invoquer
    private List<String> paramNames; // Noms des paramètres, ex: ["str"]
    private String regexPattern; // Pattern converti en regex, ex: "^/str/([^/]+)$"

    /**
     * Constructeur pour une route sans paramètres
     */
    public RouteInfo(String pattern, Method method) {
        this.pattern = pattern;
        this.method = method;
        this.paramNames = new ArrayList<>();
        this.regexPattern = convertToRegex(pattern);
    }

    /**
     * Constructeur pour une route avec paramètres
     */
    public RouteInfo(String pattern, Method method, List<String> paramNames) {
        this.pattern = pattern;
        this.method = method;
        this.paramNames = paramNames != null ? paramNames : new ArrayList<>();
        this.regexPattern = convertToRegex(pattern);
    }

    /**
     * Convertit un pattern avec {param} en expression régulière
     */
    private String convertToRegex(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return "^$";
        }

        // D'abord, échapper correctement les slashs
        String escaped = pattern.replace("/", "\\/");

        // Remplacer {param} par ([^/]+) pour capturer tout sauf /
        // IMPORTANT: ne pas échapper les parenthèses ici
        String regex = escaped.replaceAll("\\{([^}]+)\\}", "([^/]+)");

        // Ajouter les ancres de début et fin
        return "^" + regex + "$";
    }

    /**
     * Vérifie si une URL correspond à ce pattern
     */
    public boolean matches(String url) {
        if (url == null || regexPattern == null) {
            return false;
        }
        return url.matches(regexPattern);
    }

    /**
     * Extrait les valeurs des paramètres depuis une URL
     * 
     * @param url L'URL demandée
     * @return Map des noms de paramètres → valeurs
     */
    public java.util.Map<String, String> extractParameters(String url) {
        java.util.Map<String, String> params = new java.util.HashMap<>();

        if (!matches(url) || paramNames.isEmpty()) {
            return params;
        }

        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(url);

        if (matcher.matches()) {
            for (int i = 0; i < paramNames.size(); i++) {
                // Le groupe 0 est la correspondance totale, donc on commence à i+1
                params.put(paramNames.get(i), matcher.group(i + 1));
            }
        }

        return params;
    }

    // Getters et Setters

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
        this.regexPattern = convertToRegex(pattern);
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public List<String> getParamNames() {
        return paramNames;
    }

    public void setParamNames(List<String> paramNames) {
        this.paramNames = paramNames;
        // Recalculer la regex si les paramètres changent
        this.regexPattern = convertToRegex(pattern);
    }

    public String getRegexPattern() {
        return regexPattern;
    }

    /**
     * Ajoute un nom de paramètre
     */
    public void addParamName(String paramName) {
        if (this.paramNames == null) {
            this.paramNames = new ArrayList<>();
        }
        this.paramNames.add(paramName);
    }

    @Override
    public String toString() {
        return String.format("RouteInfo{pattern='%s', method=%s.%s, params=%s, regex=%s}",
                pattern,
                method.getDeclaringClass().getSimpleName(),
                method.getName(),
                paramNames,
                regexPattern);
    }
}