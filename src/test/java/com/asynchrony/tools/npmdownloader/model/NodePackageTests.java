package com.asynchrony.tools.npmdownloader.model;


import com.github.yuchi.semver.Range;
import com.github.yuchi.semver.Version;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static org.hamcrest.Matchers.*;

@RunWith(SpringRunner.class)
public class NodePackageTests {
    @Rule
    public ErrorCollector errors = new ErrorCollector();

    @Test
    public void testExpression() {
        Range.from("^1.0.0-alpha.1", false);
    }

    @Test
    public void testCreateScopedSpec() {
        Optional<NodePackageSpec> pkg = NodePackageParser.parsePackageSpecString("@angular/animations@^4.2.4-beta.4");

        errors.checkThat(pkg.isPresent(), is(true));

        NodePackageSpec packageSpec = pkg.get();
        errors.checkThat(packageSpec.getName(), is("animations"));
        errors.checkThat(packageSpec.getScope(), is("@angular"));
        errors.checkThat(packageSpec.getVersionRange().test(Version.from("4.2.4-beta.4", false)), is(true));
    }

    @Test
    public void testCreateNonScopedSpec() {
        Optional<NodePackageSpec> pkg = NodePackageParser.parsePackageSpecString("@angular/animations@^4.2.4-beta.4");

        errors.checkThat(pkg.isPresent(), is(true));

        NodePackageSpec packageSpec = pkg.get();
        errors.checkThat(packageSpec.getName(), is("animations"));
        errors.checkThat(packageSpec.getScope(), is("@angular"));
        errors.checkThat(packageSpec.getVersionRange().test(Version.from("4.2.4-beta.4", false)), is(true));
    }

    @Test
    public void testCreateSpec1() {
        Optional<NodePackageSpec> pkg = NodePackageParser.parsePackageSpecString("rxjs@^5.0.0-alpha.14");

        errors.checkThat(pkg.isPresent(), is(true));

        NodePackageSpec packageSpec = pkg.get();
        errors.checkThat(packageSpec.getName(), is("rxjs"));
        errors.checkThat(packageSpec.getScope(), isEmptyOrNullString());
        errors.checkThat(packageSpec.getVersionRange().test(Version.from("5.0.0-alpha.14", false)), is(true));
    }
}
