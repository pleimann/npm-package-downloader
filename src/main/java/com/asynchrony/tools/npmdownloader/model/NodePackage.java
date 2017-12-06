package com.asynchrony.tools.npmdownloader.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class NodePackage {
    private String scope;
    private String name;

    public boolean hasScope() {
        return this.getScope() != null && !"".equals(this.getScope());
    }

    public String getScopedName(){
        return this.hasScope() ? this.getScope() + "/" + this.getName() : this.getName();
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public enum NodePackageDepType {
        MAIN("dependencies"), PEER("peerDependencies"), DEV("devDependencies");

        private final String key;
    }
}
