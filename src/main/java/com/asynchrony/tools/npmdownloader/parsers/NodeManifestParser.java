package com.asynchrony.tools.npmdownloader.parsers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class NodeManifestParser {
    private final ObjectMapper objectMapper;
    private final NodePackageParser nodePackageParser;

    public final Set<String> parsePackageJson(InputStream packageJson) {
        try {
            JsonNode parsed = objectMapper.readTree(packageJson);

            Set<String> dependencies = new HashSet<>();
            dependencies.addAll(this.nodePackageParser.extractDependencies(parsed, "dependencies"));
            dependencies.addAll(this.nodePackageParser.extractDependencies(parsed, "devDependencies"));
            dependencies.addAll(this.nodePackageParser.extractDependencies(parsed, "peerDependencies"));

            return dependencies;

        } catch (IOException e) {
            throw new RuntimeException("Unable to read package.json: " + e.getMessage(), e);
        }
    }
}
