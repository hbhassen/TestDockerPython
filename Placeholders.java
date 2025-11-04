import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderUtils {

    // Regex: commence par ${ , capture ce qu'il y a dedans, et finit par }
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("^\\$\\{(.+)}$");

    /**
     * Extrait le contenu du placeholder ${text}
     * @param input chaîne entrée
     * @return le texte à l'intérieur si format valide, sinon la chaîne originale
     */
    public static String extractPlaceholder(String input) {
        if (input == null) return null; // gestion basique du null

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(input);
        if (matcher.matches()) {
            return matcher.group(1); // contenu entre ${ et }
        }
        return input; // pas un placeholder valide
    }

    public static void main(String[] args) {
        System.out.println(extractPlaceholder("${name}")); // name
        System.out.println(extractPlaceholder("hello"));   // hello
        System.out.println(extractPlaceholder("${x.y.z}")); // x.y.z
    }
}