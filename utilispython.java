package com.example.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PythonVersionUtils {

    private PythonVersionUtils() {
        // Constructeur privé pour empêcher l’instanciation
    }

    /**
     * Détecte la version de Python dans le contenu d’un fichier texte.
     * @param content le contenu du fichier
     * @return chaîne au format "langage: python:<version>" ou null si non trouvée
     */
    public static String detectPythonVersion(String content) {
        if (content == null || content.isBlank()) return null;

        String version = null;
        String text = content.trim().toLowerCase();

        String[] regexes = {
            "python[_\\s-]*requires\\s*[=:]\\s*[\"']?([0-9]+(?:\\.[0-9]+){0,2})",
            "python_version\\s*[=:]\\s*[\"']?([0-9]+(?:\\.[0-9]+){0,2})",
            "python\\s*[=><!]+\\s*([0-9]+(?:\\.[0-9]+){0,2})",
            "from\\s+python:?([0-9]+(?:\\.[0-9]+){0,2})",
            "^([0-9]+(?:\\.[0-9]+){0,2})$",
            "python[-:]?([0-9]+(?:\\.[0-9]+){0,2})",
            "python\\s*([0-9]+(?:\\.[0-9]+){0,2})"
        };

        for (String regex : regexes) {
            Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                version = matcher.group(1);
                break;
            }
        }

        return (version != null) ? "langage: python:" + version : null;
    }
}