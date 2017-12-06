package com.asynchrony.tools.npmdownloader.model;

import com.github.yuchi.semver.Version;
import com.google.common.collect.Ordering;
import lombok.*;

import java.net.URL;
import java.util.Set;

@Value
public class NodePackageVersion extends NodePackage implements Comparable<NodePackageVersion> {
    private Version version;
    private URL tarballUrl;
    private Set<String> dependencies;

    public NodePackageVersion(String scope, String name, Version version, URL tarballUrl, Set<String> dependencies) {
        super(scope, name);

        this.version = version;
        this.tarballUrl = tarballUrl;
        this.dependencies = dependencies;
    }

    @Override
    public int compareTo(NodePackageVersion that) {
        return Ordering.natural().nullsLast()
                .onResultOf(NodePackageVersion::getScopedName)
                .thenComparing(NodePackageVersion::getVersion)
                .reversed().compare(this, that);
    }
}
