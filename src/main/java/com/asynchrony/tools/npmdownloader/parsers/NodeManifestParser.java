package com.asynchrony.tools.npmdownloader.parsers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

@UtilityClass
public class NodeManifestParser {
    static final ObjectMapper JSON = new ObjectMapper();

    public static final Set<String> parsePackageJson(InputStream packageJson) {
        try {
            JsonNode parsed = JSON.readTree(packageJson);

            Set<String> dependencies = new HashSet<>();
            dependencies.addAll(NodePackageParser.extractDependencies(parsed, "dependencies"));
            dependencies.addAll(NodePackageParser.extractDependencies(parsed, "devDependencies"));
            dependencies.addAll(NodePackageParser.extractDependencies(parsed, "peerDependencies"));

            return dependencies;

        } catch (IOException e) {
            throw new RuntimeException("Unable to read package.json: " + e.getMessage(), e);
        }
    }
}
