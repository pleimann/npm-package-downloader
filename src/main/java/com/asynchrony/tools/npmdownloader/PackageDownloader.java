package com.asynchrony.tools.npmdownloader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

import static java.util.stream.Collectors.toList;

@Slf4j
@RequiredArgsConstructor(staticName = "create")
public class PackageDownloader extends RecursiveAction {
    private final File destinationDirectory;
    private final NodePackage nodePackage;

    @Override
    protected void compute() {
        NodePackage.PackageVersion greatestVersion = this.nodePackage.getVersions().first();

        if (greatestVersion.hasScope()) {
            new File(this.destinationDirectory, greatestVersion.getScope()).mkdir();
        }

        File destinationFile = new File(this.destinationDirectory,
                greatestVersion.getName() + "-" + greatestVersion.getVersion() + ".tgz");

        if (!destinationFile.exists()) {
            try {
                URL packageTarballUrl = greatestVersion.getTarballUrl();

                try (InputStream packageStream = packageTarballUrl.openStream()) {
                    log.info("Downloading... " + greatestVersion);
                    Files.copy(packageStream, destinationFile.toPath());
                }

            } catch (IOException e) {
                log.error("Error storing package archive " + destinationFile.getAbsolutePath(), e);
            }
        }

        Set<NodePackage> packageDependencies = greatestVersion.getDependencies();

        ForkJoinTask.invokeAll(
                packageDependencies.stream()
                        .map((dependency) -> new PackageDownloader(this.destinationDirectory, dependency))
                        .collect(toList()));
    }
}
