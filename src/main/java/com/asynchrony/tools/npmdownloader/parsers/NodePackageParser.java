package com.asynchrony.tools.npmdownloader.parsers;

import com.asynchrony.tools.npmdownloader.model.NodePackageSpec;
import com.asynchrony.tools.npmdownloader.model.NodePackageVersion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.yuchi.semver.Range;
import com.github.yuchi.semver.Version;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toSet;

@Slf4j
@Component
@RequiredArgsConstructor
public class NodePackageParser {
    private static final Pattern PACKAGE_SPEC_PATTERN = Pattern.compile("^((@[^@\\/]+)\\/)?([^@\\/]+)(@([^@\\/]+))?$");
    private static final String REGISTRY_URL_STRING = "https://registry.npmjs.com/";

    private final ObjectMapper objectMapper;

    public Optional<NodePackageSpec> parsePackageSpecString(String specString) {
        Matcher match = PACKAGE_SPEC_PATTERN.matcher(specString);
        if (!match.matches()) {
            log.debug("Invalid package specification: " + specString);

            return Optional.empty();
        }

        String scope = match.group(2);
        String name = match.group(3);
        String versionSpecString = match.group(5);

        Range versionSpec = Range.from(versionSpecString, true);

        if(versionSpec == null){
            log.info("PackageSpec has no versionSpec: " + specString + "! Defaulting to '^0.0.0'");

            versionSpec = Range.from("^0.0.0", true);
        }

        return Optional.of(new NodePackageSpec(specString, scope, name, versionSpec));
    }

    public SortedSet<NodePackageVersion> loadVersionsForPackageSpec(final NodePackageSpec nodePackageSpec) {
        final URL packageRegistryUrl = packageRegistryUrl(nodePackageSpec.getScopedName())
                .orElseThrow(IllegalArgumentException::new);

        try (InputStream registryStream = packageRegistryUrl.openStream()) {
            JsonNode parsed = objectMapper.readTree(registryStream);

            JsonNode versionsJson = parsed.path("versions");

            SortedSet<NodePackageVersion> packageVersions = new TreeSet<>();

            versionsJson.fields().forEachRemaining(versionJsonEntry -> {
                NodePackageVersion packageVersion = parseVersionJSON(versionJsonEntry.getValue());

                if (nodePackageSpec.getVersionRange() != null
                        && nodePackageSpec.getVersionRange().test(packageVersion.getVersion())) {
                    packageVersions.add(packageVersion);
                }
            });

            return packageVersions;

        } catch (IOException e) {
            throw new RuntimeException("Error connecting to NPM Registry at URL " + packageRegistryUrl + " for package " + nodePackageSpec, e);
        }
    }

    private NodePackageVersion parseVersionJSON(JsonNode versionObj) {
        String name = versionObj.get("name").asText();
        String scope = null;
        if (name.contains("/")) {
            scope = name.substring(0, name.indexOf('/'));
        }

        String versionString = versionObj.path("version").asText();

        Version version = Version.from(versionString, false);

        String tarballUrlString = versionObj.path("dist").path("tarball").asText();

        URL tarballUrl = null;
        if (tarballUrlString != null && !"".equals(tarballUrlString)) {
            try {
                tarballUrl = new URL(tarballUrlString);

            } catch (MalformedURLException e) {
                log.error("Invalid URL returned from npm registry: " + tarballUrlString, e);
            }
        }

        Set<String> dependencies = new HashSet<>();
        dependencies.addAll(extractDependencies(versionObj, "dependencies"));
        dependencies.addAll(extractDependencies(versionObj, "peerDependencies"));

        return new NodePackageVersion(scope, name, version, tarballUrl, dependencies);
    }

    public Set<String> extractDependencies(JsonNode versionObj, String dependenciesSection) {
        JsonNode dependenciesObj = versionObj.path(dependenciesSection);

        return Lists.newArrayList(dependenciesObj.fields()).stream()
                .map(entry -> entry.getKey() + "@" + entry.getValue().asText())
                .collect(toSet());
    }

    private Optional<URL> packageRegistryUrl(String packageName) {
        try {
            return Optional.of(new URL(REGISTRY_URL_STRING + encodeScopedPackageName(packageName)));

        } catch (MalformedURLException | UnsupportedEncodingException e) {
            log.error("Error constructing registry URL for package " + packageName, e);
        }

        return Optional.empty();
    }

    private String encodeScopedPackageName(String packageName) throws UnsupportedEncodingException {
        return packageName.replace("/", URLEncoder.encode("/", "UTF-8"));
    }
}
