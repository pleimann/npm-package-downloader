package com.asynchrony.tools.npmdownloader;

import com.github.zafarkhaja.semver.Version;
import com.github.zafarkhaja.semver.expr.Expression;
import com.github.zafarkhaja.semver.expr.ExpressionParser;
import com.github.zafarkhaja.semver.expr.LexerException;
import com.google.common.collect.Ordering;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Slf4j
@ToString
public class NodePackage {
    private static final String REGISTRY_URL_STRING = "https://registry.npmjs.com/";
    private static final Pattern PACKAGE_SPEC_PATTERN = Pattern.compile("^((@[^@\\/]+)\\/)?([^@\\/]+)(@([^@\\/]+))?$");

    @Getter
    private final PackageSpec packageSpec;

    @Getter(lazy = true)
    private final SortedSet<PackageVersion> versions = loadVersions();

    private NodePackage(String packageSpecString) {
        this.packageSpec = PackageSpec.parse(packageSpecString);
    }

    public static final NodePackage create(String packageSpecString) {
        return new NodePackage(packageSpecString);
    }

    private SortedSet<PackageVersion> loadVersions() {
        try (InputStream registryStream = this.packageRegistryUrl().openStream()) {
            try {
                Object parsed = new JSONParser(JSONParser.MODE_JSON_SIMPLE).parse(registryStream);

                if (parsed instanceof JSONObject) {
                    JSONObject versionsJson = (JSONObject) ((JSONObject) parsed).get("versions");

                    return versionsJson.values().stream()
                            .map(JSONObject.class::cast)
                            .map(NodePackage::parseVersionJSON)
                            .filter((packageVersion) -> packageVersion.getVersion().satisfies(this.packageSpec.getVersionSpec()))
                            .collect(toCollection(TreeSet::new));
                } else {
                    throw new IllegalStateException("Invalid JSON response response from registry: " + this.packageRegistryUrl());
                }

            } catch (ParseException e) {
                throw new RuntimeException("Error parsing response from " + this.packageRegistryUrl(), e);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error connecting to NPM Registry at URL " + this.packageRegistryUrl(), e);
        }
    }

    static PackageVersion parseVersionJSON(JSONObject versionObj) {
        String name = versionObj.getAsString("name");
        String scope = null;
        if (name.contains("/")) {
            scope = name.substring(0, name.indexOf('/'));
        }

        Version version = Version.valueOf(versionObj.getAsString("version"));

        String tarballUrlString = ((JSONObject)versionObj.get("dist")).getAsString("tarball");

        URL tarballUrl = null;
        if(tarballUrlString != null && !"".equals(tarballUrlString)) {
            try {
                tarballUrl = new URL(tarballUrlString);

            } catch (MalformedURLException e) {
                log.error("Invalid URL returned from npm registry: " + tarballUrlString, e);
            }
        }

        Set<NodePackage> dependencies = extractDependencies(versionObj, "dependencies");

        return PackageVersion.of(scope, name, version, tarballUrl, dependencies);
    }

    private static Set<NodePackage> extractDependencies(JSONObject versionObj, String dependenciesSection) {
        JSONObject dependenciesObj = (JSONObject) versionObj.getOrDefault(dependenciesSection, new JSONObject());
        return dependenciesObj.entrySet().stream()
                .map((entry) -> NodePackage.create(entry.getKey() + "@" + entry.getValue()))
                .collect(toSet());
    }

    URL packageRegistryUrl() {
        String specUrlString = null;

        try {
            specUrlString = REGISTRY_URL_STRING
                    + (this.packageSpec.hasScope() ? this.packageSpec.getScope() + URLEncoder.encode("/" + this.packageSpec.getName(), "UTF-8")
                        : this.packageSpec.getName());

            return new URL(specUrlString);

        } catch (MalformedURLException | UnsupportedEncodingException e) {
            log.error("Error constructing registry URL: " + specUrlString, e);
        }

        return null;
    }

    @ToString
    @Value(staticConstructor = "of")
    static class PackageSpec {
        private String scope;
        private String name;
        private String versionSpec;

        public boolean hasScope() {
            return this.scope != null && !"".equals(this.scope);
        }

        public String getScopedName() {
            return this.hasScope() ? this.scope + "/" + this.name : this.name;
        }

        protected static PackageSpec parse(String specString) {
            Matcher match = PACKAGE_SPEC_PATTERN.matcher(specString);
            if (!match.matches()) {
                throw new IllegalArgumentException("Invalid package specification: " + specString);
            }

            String scope = match.group(2);
            String name = match.group(3);
            String versionSpec = match.group(5);

            return PackageSpec.of(scope, name, versionSpec);
        }
    }

    @ToString
    @Value(staticConstructor = "of")
    static class PackageVersion implements Comparable<PackageVersion> {
        private String scope;
        private String name;
        private Version version;
        private URL tarballUrl;
        private Set<NodePackage> dependencies;

        public boolean hasScope() {
            return this.scope != null;
        }

        @Override
        public int compareTo(PackageVersion that) {
            return Ordering.natural().nullsLast()
                    .onResultOf(PackageVersion::getName)
                    .thenComparing(PackageVersion::getVersion)
                    .reversed().compare(this, that);
        }
    }
}
