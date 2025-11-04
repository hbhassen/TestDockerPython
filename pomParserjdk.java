import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.util.Optional;

public class PomJdkParser {

    public static Optional<String> extractJdkVersion(File pomFile) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pomFile);
            doc.getDocumentElement().normalize();

            // 1) Check properties
            Optional<String> props = extractFromProperties(doc);
            if (props.isPresent()) return props;

            // 2) Check compiler plugin
            Optional<String> pluginCfg = extractFromCompilerPlugin(doc);
            if (pluginCfg.isPresent()) return pluginCfg;

            // 3) Fallback regex
            return fallbackRegex(pomFile);

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Optional<String> extractFromProperties(Document doc) {
        NodeList propsList = doc.getElementsByTagName("properties");
        if (propsList.getLength() == 0) return Optional.empty();
        Element props = (Element) propsList.item(0);

        return Optional.ofNullable(
                getTag(props, "maven.compiler.release",
                getTag(props, "maven.compiler.source",
                getTag(props, "java.version", null)))
        );
    }

    private static Optional<String> extractFromCompilerPlugin(Document doc) {
        NodeList plugins = doc.getElementsByTagName("plugin");

        for (int i = 0; i < plugins.getLength(); i++) {
            Element plugin = (Element) plugins.item(i);
            if ("maven-compiler-plugin".equals(getTag(plugin, "artifactId", ""))) {
                Element config = (Element) plugin.getElementsByTagName("configuration").item(0);
                if (config == null) continue;

                String version = getTag(config, "release",
                                getTag(config, "source", null));
                return Optional.ofNullable(version);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> fallbackRegex(File pom) throws Exception {
        String xml = Files.readString(pom.toPath());
        var match = java.util.regex.Pattern.compile("<release>(\\d+)</release>").matcher(xml);
        if (match.find()) return Optional.of(match.group(1));
        match = java.util.regex.Pattern.compile("<source>(\\d+)</source>").matcher(xml);
        if (match.find()) return Optional.of(match.group(1));
        return Optional.empty();
    }

    private static String getTag(Element parent, String tag, String defaultValue) {
        NodeList list = parent.getElementsByTagName(tag);
        return list.getLength() > 0 ? list.item(0).getTextContent().trim() : defaultValue;
    }

    public static void main(String[] args) {
        File pom = new File("pom.xml");
        System.out.println("JDK version detected: " +
            extractJdkVersion(pom).orElse("Unknown"));
    }
}