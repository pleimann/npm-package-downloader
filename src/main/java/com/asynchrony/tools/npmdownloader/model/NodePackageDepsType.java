package com.asynchrony.tools.npmdownloader.model;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum NodePackageDepsType {
    MAIN("dependencies"), PEER("peerDependencies"), DEV("devDependencies");

    private final String key;
}
