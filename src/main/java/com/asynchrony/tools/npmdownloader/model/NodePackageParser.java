package com.asynchrony.tools.npmdownloader.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.yuchi.semver.Range;
import com.github.yuchi.semver.Version;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.MoreCollectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

@Slf4j
@UtilityClass
public class NodePackageParser {
    static final Pattern PACKAGE_SPEC_PATTERN = Pattern.compile("^((@[^@\\/]+)\\/)?([^@\\/]+)(@([^@\\/]+))?$");
    static final String REGISTRY_URL_STRING = "https://registry.npmjs.com/";
    static final ObjectMapper JSON = new ObjectMapper();

    public static Optional<NodePackageSpec> parsePackageSpecString(String specString) {
        Matcher match = PACKAGE_SPEC_PATTERN.matcher(specString);
        if (!match.matches()) {
            log.debug("Invalid package specification: " + specString);

            return Optional.empty();
        }

        String scope = match.group(2);
        String name = match.group(3);
        String versionSpecString = match.group(5);

        Range versionSpec = Range.from(versionSpecString, true);

        return Optional.of(new NodePackageSpec(scope, name, versionSpec));
    }

    public static NodePackageSpec loadVersionsForPackageSpec(final NodePackageSpec nodePackageSpec) {
        if(nodePackageSpec.getVersions() == null) {
            final URL packageRegistryUrl = packageRegistryUrl(nodePackageSpec.getScopedName()).orElseThrow(IllegalArgumentException::new);

            try (InputStream registryStream = packageRegistryUrl.openStream()) {
                JsonNode parsed = JSON.readTree(registryStream);

                JsonNode versionsJson = parsed.path("versions");

                SortedSet<NodePackageVersion> packageVersions = new TreeSet<>();

                versionsJson.fields().forEachRemaining(versionJsonEntry -> {
                    NodePackageVersion packageVersion = parseVersionJSON(versionJsonEntry.getValue());

                    if (nodePackageSpec.getVersionRange().test(packageVersion.getVersion())) {
                        packageVersions.add(packageVersion);
                    }
                });

                return nodePackageSpec.withVersions(packageVersions);

            } catch (IOException e) {
                throw new RuntimeException("Error connecting to NPM Registry at URL " + packageRegistryUrl + " for package " + nodePackageSpec, e);
            }

        } else {
            return nodePackageSpec;
        }
    }

    static NodePackageVersion parseVersionJSON(JsonNode versionObj) {
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

        Set<NodePackageSpec> dependencies = new HashSet<>();
        dependencies.addAll(extractDependencies(versionObj, "dependencies"));
        dependencies.addAll(extractDependencies(versionObj, "peerDependencies"));

        return new NodePackageVersion(scope, name, version, tarballUrl, dependencies);
    }

    public static Set<NodePackageSpec> extractDependencies(JsonNode versionObj, String dependenciesSection) {
        JsonNode dependenciesObj = versionObj.path(dependenciesSection);

        return Lists.newArrayList(dependenciesObj.fields()).stream()
                .map(entry -> parsePackageSpecString(entry.getKey() + "@" + entry.getValue().asText()))
                .filter(p -> p.isPresent())
                .map(Optional::get)
                .collect(toSet());
    }

    static Optional<URL> packageRegistryUrl(String packageName) {
        try {
            return Optional.of(new URL(REGISTRY_URL_STRING + encodeScopedPackageName(packageName)));

        } catch (MalformedURLException | UnsupportedEncodingException e) {
            log.error("Error constructing registry URL for package " + packageName, e);
        }

        return Optional.empty();
    }

    private static String encodeScopedPackageName(String packageName) throws UnsupportedEncodingException {
        return packageName.replace("/", URLEncoder.encode("/", "UTF-8"));
    }
}
