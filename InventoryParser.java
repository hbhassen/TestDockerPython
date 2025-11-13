package com.example.obsolescence.service;

import com.example.obsolescence.domain.LanguageInfo;
import com.example.obsolescence.domain.ProjectInventory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryParser {

    public LanguageInfo detectLanguage(ProjectInventory inventory) {
        List<String> list = inventory.getCollectedEntries();
        for (int i = 0; i < list.size(); i++) {
            String entry = list.get(i);
            if (entry.startsWith("langage:")) {
                String[] parts = entry.split(":");
                if (parts.length >= 3) {
                    String langCode = parts[1];
                    String runtimeName = parts[2];

                    String version = null;

                    if (i + 1 < list.size()) {
                        String next = list.get(i + 1);
                        String[] vparts = next.split(":");
                        if (vparts.length >= 2 && vparts[0].equalsIgnoreCase(langCode)) {
                            version = vparts[1];
                        }
                    }

                    return new LanguageInfo(langCode, runtimeName, version);
                }
            }
        }
        return null;
    }
}
