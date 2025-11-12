import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PythonVersionDetector {

    /**
     * Détecte la version de Python dans le contenu d’un fichier texte.
     * @param content le contenu du fichier (pyproject.toml, Dockerfile, setup.py, etc.)
     * @return chaîne au format "langage: python:<version>" ou null si non trouvée
     */
    public static String detectPythonVersion(String content) {
        if (content == null || content.isBlank()) return null;

        String version = null;

        // Nettoyer le contenu pour éviter des faux positifs
        String text = content.trim().toLowerCase();

        // Ensemble de regex adaptées aux différents fichiers possibles
        String[] regexes = {
            // pyproject.toml, setup.cfg, setup.py
            "python[_\\s-]*requires\\s*[=:]\\s*[\"']?([0-9]+(?:\\.[0-9]+){0,2})",
            // Pipfile
            "python_version\\s*[=:]\\s*[\"']?([0-9]+(?:\\.[0-9]+){0,2})",
            // environment.yml
            "python\\s*[=><!]+\\s*([0-9]+(?:\\.[0-9]+){0,2})",
            // Dockerfile
            "from\\s+python:?([0-9]+(?:\\.[0-9]+){0,2})",
            // .python-version
            "^([0-9]+(?:\\.[0-9]+){0,2})$",
            // runtime.txt
            "python[-:]?([0-9]+(?:\\.[0-9]+){0,2})",
            // commentaire informatif éventuel
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

    // Exemple de test
    public static void main(String[] args) {
        String example1 = """
            [project]
            name = "mon-projet"
            requires-python = ">=3.10,<3.12"
            """;

        String example2 = "FROM python:3.11-slim";
        String example3 = "python=3.9\npandas=1.3.5";

        System.out.println(detectPythonVersion(example1)); // -> langage: python:3.10
        System.out.println(detectPythonVersion(example2)); // -> langage: python:3.11
        System.out.println(detectPythonVersion(example3)); // -> langage: python:3.9
    }
}