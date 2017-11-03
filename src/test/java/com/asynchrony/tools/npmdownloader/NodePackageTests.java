package com.asynchrony.tools.npmdownloader;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class NodePackageTests {
    @Rule
    public ErrorCollector errors = new ErrorCollector();

    @Test
    public void testCreateSpec(){
        NodePackage pkg = NodePackage.create("@angular/animations@^4.2.4-beta.4");

        errors.checkThat(pkg.getPackageSpec().getName(), is("animations"));
        errors.checkThat(pkg.getPackageSpec().getScope(), is("@angular"));
        errors.checkThat(pkg.getPackageSpec().getVersionSpec(), is("^4.2.4-beta.4"));
    }
}
