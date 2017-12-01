package com.asynchrony.tools.npmdownloader.model;

import com.github.yuchi.semver.Range;
import com.google.common.collect.Ordering;
import lombok.*;
import lombok.experimental.Wither;

import java.util.SortedSet;

@Getter
@Wither
@ToString
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NodePackageSpec implements Comparable<NodePackageSpec> {
    private final String scope;
    private final String name;
    private final Range versionRange;
    private SortedSet<NodePackageVersion> versions;

    public boolean hasScope() {
        return this.scope != null && !"".equals(this.scope);
    }

    public String getScopedName(){
        return this.hasScope() ? this.scope + "/" + this.name : this.name;
    }

    @Override
    public int compareTo(NodePackageSpec that) {
        return Ordering.natural().nullsLast()
                .onResultOf(NodePackageSpec::getName)
                .compare(this, that);
    }
}
