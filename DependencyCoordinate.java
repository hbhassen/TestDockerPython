package com.example.obsolescence.service;

import java.util.Optional;

public record DependencyCoordinate(String group, String artifact, String version) {

    public String ga() {
        return group + ":" + artifact;
    }

    public static Optional<DependencyCoordinate> parse(String s) {
        String[] parts = s.split(":");
        if (parts.length == 3) {
            return Optional.of(new DependencyCoordinate(parts[0], parts[1], parts[2]));
        }
        return Optional.empty();
    }
}
