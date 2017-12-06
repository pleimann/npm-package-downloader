package com.asynchrony.tools.npmdownloader.model;

import com.github.yuchi.semver.Range;
import com.google.common.collect.Ordering;
import lombok.Value;

@Value
public class NodePackageSpec extends NodePackage implements Comparable<NodePackageSpec> {
    private String spec;
    private Range versionRange;

    public NodePackageSpec(String scope, String name, String spec, Range versionRange) {
        super(scope, name);

        this.spec = spec;
        this.versionRange = versionRange;
    }

    @Override
    public int compareTo(NodePackageSpec that) {
        return Ordering.natural().nullsLast()
                .onResultOf(NodePackageSpec::getScopedName)
                .compare(this, that);
    }
}



