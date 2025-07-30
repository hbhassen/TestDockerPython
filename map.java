package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "myapp")
public class MyAppProperties {

    private List<Setting> settings;

    public List<Setting> getSettings() {
        return settings;
    }

    public void setSettings(List<Setting> settings) {
        this.settings = settings;
    }

    // Classe interne pour représenter un dictionnaire
    public static class Setting {
        private String app;
        private String dir;
        private String dirKo;

        public String getApp() { return app; }
        public void setApp(String app) { this.app = app; }

        public String getDir() { return dir; }
        public void setDir(String dir) { this.dir = dir; }

        public String getDirKo() { return dirKo; }
        public void setDirKo(String dirKo) { this.dirKo = dirKo; }
    }
}