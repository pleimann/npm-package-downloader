package com.asynchrony.tools.npmdownloader;

import com.asynchrony.tools.npmdownloader.parsers.NodeManifestParser;
import com.asynchrony.tools.npmdownloader.parsers.NodePackageParser;
import com.asynchrony.tools.npmdownloader.model.NodePackageSpec;
import com.asynchrony.tools.npmdownloader.model.NodePackageVersion;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor(staticName = "create")
public class NodePackageDownloader {
    private final File destinationDirectory;

    private final Map<String, NodePackageVersion> downloadQueue = Maps.newConcurrentMap();
    private final ForkJoinPool dependencyPool = new ForkJoinPool(5);

    public void queueManifest(String manifest) throws FileNotFoundException {
        URL manifestUrl = ResourceUtils.getURL(manifest);

        try (InputStream manifestStream = manifestUrl.openStream()) {
            Set<String> dependencies = NodeManifestParser.parsePackageJson(manifestStream);

            dependencies.forEach(this::queuePackage);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void queuePackage(String nodePackageSpecString) {
        if (downloadQueue.containsKey(nodePackageSpecString)) {
            log.debug("Skipping, already enqueued... " + nodePackageSpecString);

        } else {
            log.info("Queueing... " + nodePackageSpecString);
            NodePackageParser.parsePackageSpecString(nodePackageSpecString)
                    .ifPresent(this::queuePackage);

        }
    }

    public void queuePackage(NodePackageSpec nodePackageSpec) {
        final SortedSet<NodePackageVersion> packageVersions = NodePackageParser.loadVersionsForPackageSpec(nodePackageSpec);

        if (!packageVersions.isEmpty()) {
            NodePackageVersion packageVersion = packageVersions.first();

            this.downloadQueue.putIfAbsent(nodePackageSpec.getSpec(), packageVersion);

            // Queue dependency downloads
            packageVersion.getDependencies().parallelStream().forEach(this::queuePackage);

        } else {
            log.warn("PackageSpec has no versions: " + nodePackageSpec);
        }
    }

    public void printDownloadQueue() {
        this.dependencyPool.awaitQuiescence(10, TimeUnit.MINUTES);
        this.downloadQueue.values().forEach(System.out::println);
    }

    public void downloadQueuedPackages() {
        Map<String, NodePackageVersion> packageVersions = new HashMap<>(this.downloadQueue);

        packageVersions.values().parallelStream()
                .forEach(this::downloadPackageVersion);
    }

    private void downloadPackageVersion(NodePackageVersion nodePackageVersion) {
        if (nodePackageVersion.hasScope()) {
            new File(this.destinationDirectory, nodePackageVersion.getScope()).mkdir();
        }

        File destinationFile = new File(this.destinationDirectory,
                nodePackageVersion.getName() + "-" + nodePackageVersion.getVersion() + ".tgz");

        if (destinationFile.exists()) {
            log.debug("Skipping, already downloaded... " + nodePackageVersion);

        } else {
            try {
                // Create parent directory if it doesn't exist
                if (destinationFile.getParentFile() != null) {
                    destinationFile.getParentFile().mkdirs();
                }

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
