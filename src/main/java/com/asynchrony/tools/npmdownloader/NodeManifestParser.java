package com.asynchrony.tools.npmdownloader;

import com.asynchrony.tools.npmdownloader.model.NodePackageParser;
import com.asynchrony.tools.npmdownloader.model.NodePackageSpec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class NodeManifestParser {
    static final ObjectMapper JSON = new ObjectMapper();

    public static final Set<NodePackageSpec> parserPackageJson(File packageJson) {
        JsonNode parsed = null;
        try {
            parsed = JSON.readTree(packageJson);
            return NodePackageParser.extractDependencies(parsed, "dependencies");

        } catch (IOException e) {
            e.printStackTrace();
        }

        return Sets.newHashSet();
    }
}
