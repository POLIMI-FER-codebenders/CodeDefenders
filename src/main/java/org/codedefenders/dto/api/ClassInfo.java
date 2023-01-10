package org.codedefenders.dto.api;

import org.codedefenders.game.AssertionLibrary;
import org.codedefenders.game.TestingFramework;

public class ClassInfo {
    Integer id;
    String name;
    String alias;
    String source;
    TestingFramework testingFramework;
    AssertionLibrary assertionLibrary;

    public ClassInfo(Integer id, String name, String alias, String source, TestingFramework testingFramework, AssertionLibrary assertionLibrary) {
        this.id = id;
        this.name = name;
        this.alias = alias;
        this.source = source;
        this.testingFramework = testingFramework;
        this.assertionLibrary = assertionLibrary;
    }
}
