package com.asynchrony.tools.npmdownloader.model;

import com.github.yuchi.semver.Version;
import com.google.common.collect.Ordering;
import lombok.*;

import java.net.URL;
import java.util.Set;

@Value
@ToString
@RequiredArgsConstructor
public class NodePackageVersion implements Comparable<NodePackageVersion> {
    private final String scope;
    private final String name;
    private final Version version;
    private final URL tarballUrl;
    private final Set<String> dependencies;

    public boolean hasScope() {
        return this.scope != null && !"".equals(this.scope);
    }

    public String getScopedName(){
        return this.hasScope() ? this.getScope() + "/" + this.getName() : this.getName();
    }

    @Override
    public int compareTo(NodePackageVersion that) {
        return Ordering.natural().nullsLast()
                .onResultOf(NodePackageVersion::getScopedName)
                .thenComparing(NodePackageVersion::getVersion)
                .reversed().compare(this, that);
    }
}
