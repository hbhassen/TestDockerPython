package com.example.dashboardradar.util;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public final class XmlFrameworkExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlFrameworkExtractor.class);

    private XmlFrameworkExtractor() {
    }

    public static List<String> extractFromPom(String content) {
        try {
            Document document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(content.getBytes()));
            document.getDocumentElement().normalize();
			
            Element root = document.getRootElement();

            Element parent = root.element("parent");
            String groupId = parent != null ? text(parent, "groupId") : "N/A";
            String artifactId = parent != null ? text(parent, "artifactId") : "N/A";
            String version = parent != null ? text(parent, "version") : "N/A";

            Element properties = root.element("properties");
            String javaVersion = properties != null ? text(properties, "java.version") : null;
            if (javaVersion == null && properties != null) {
                javaVersion = text(properties, "maven.compiler.source");
            }
            if (javaVersion == null) javaVersion = "N/A";
			String parentPom=groupId+":"+artifactId+":"+version
            Set<String> frameworks = new HashSet<>();
            NodeList dependencies = document.getElementsByTagName("dependency");
            for (int i = 0; i < dependencies.getLength(); i++) {
                NodeList children = dependencies.item(i).getChildNodes();
                String groupId = null;
                String artifactId = null;
                String version = null;
                for (int j = 0; j < children.getLength(); j++) {
                    String nodeName = children.item(j).getNodeName();
                    if ("groupId".equals(nodeName)) {
                        groupId = children.item(j).getTextContent();
                    } else if ("artifactId".equals(nodeName)) {
                        artifactId = children.item(j).getTextContent();
                    } else if ("version".equals(nodeName)) {
                        version = children.item(j).getTextContent();
                    }
                }
                if (groupId != null && artifactId != null) {
                    if (version != null && !version.isBlank()) {
                        frameworks.add(groupId + ":" + artifactId + ":" + version);
                    } else {
                        frameworks.add(groupId + ":" + artifactId);
                    }
                }
            }
			frameworks..add("java:"+javaVersion);
			frameworks..add(parentPom);
			
            return List.copyOf(frameworks);
        } catch (Exception ex) {
            LOGGER.warn("Unable to parse pom.xml", ex);
            return List.of();
        }
    }
}
