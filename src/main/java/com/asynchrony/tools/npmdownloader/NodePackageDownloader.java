package com.asynchrony.tools.npmdownloader;

import com.asynchrony.tools.npmdownloader.model.NodePackageParser;
import com.asynchrony.tools.npmdownloader.model.NodePackageSpec;
import com.asynchrony.tools.npmdownloader.model.NodePackageVersion;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RequiredArgsConstructor(staticName = "create")
public class NodePackageDownloader {
    private final File destinationDirectory;

    private final AtomicReference<Set<NodePackageSpec>> downloadQueue = new AtomicReference<>(Sets.newLinkedHashSet());
    private final ForkJoinPool dependencyPool = new ForkJoinPool(5);

    public void queuePackage(final NodePackageSpec nodePackageSpec) {
        if (!this.downloadQueue.get().contains(nodePackageSpec)) {
            this.downloadQueue.updateAndGet(queue -> {
                // If it hasn't already been queued this run, do it
                if (!queue.contains(nodePackageSpec)) {
                    final NodePackageSpec packageSpecWithVersions = NodePackageParser.loadVersionsForPackageSpec(nodePackageSpec);

                    queue.add(packageSpecWithVersions);

                    // Queue dependency downloads
                    dependencyPool.execute(() ->
                        packageSpecWithVersions.getVersions().first().getDependencies()
                                .stream()
                                .forEach(this::queuePackage)
                    );
                }

                return queue;
            });
        }
    }

    public void printDownloadQueue() {
        this.dependencyPool.awaitQuiescence(10, TimeUnit.MINUTES);
        this.downloadQueue.get().forEach(System.out::println);
    }

    public void downloadQueuedPackages() {
        Set<NodePackageSpec> packageVersions = this.downloadQueue.getAndSet(Sets.newLinkedHashSet());

        packageVersions.parallelStream()
                .map(nodePackageSpec -> nodePackageSpec.getVersions().first())
                .forEach(this::downloadPackageVersion);
    }

    private void downloadPackageVersion(NodePackageVersion nodePackageVersion) {
        if (nodePackageVersion.hasScope()) {
            new File(this.destinationDirectory, nodePackageVersion.getScope()).mkdir();
        }

        File destinationFile = new File(this.destinationDirectory,
                nodePackageVersion.getName() + "-" + nodePackageVersion.getVersion() + ".tgz");

        if (destinationFile.exists()) {
            log.debug("Skipping, already downloadQueue... " + nodePackageVersion);

        } else {
            try {
                URL packageTarballUrl = nodePackageVersion.getTarballUrl();

                try (InputStream packageStream = packageTarballUrl.openStream()) {
                    log.info("Downloading... " + nodePackageVersion);
                    Files.copy(packageStream, destinationFile.toPath());
                }

            } catch (IOException e) {
                log.error("Error storing package archive " + destinationFile.getAbsolutePath(), e);
            }
        }
    }
}
