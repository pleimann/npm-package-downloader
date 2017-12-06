package com.asynchrony.tools.npmdownloader;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootApplication
public class NodeDownloaderApplication implements ApplicationRunner {
    private static final List<String> TEST_DATA =
            Arrays.asList(new String[]{
//                    "@angular/animations@^5.0.0",
//                    "@angular/common@^5.0.0",
//                    "@angular/compiler@^5.0.0",
//                    "@angular/core@^5.0.0",
//                    "@angular/forms@^5.0.0",
//                    "@angular/http@^5.0.0",
//                    "@angular/platform-browser@^5.0.0",
//                    "@angular/platform-browser-dynamic@^5.0.0",
//                    "@angular/platform-server@^5.0.0",
//                    "@angular/router@^5.0.0",
//                    "@ngrx/store@^4.0.3",
//                    "@ngrx/effects@^4.0.5",
//                    "@ngrx/router-store@^4.0.4",
//                    "@ngrx/store-devtools@^4.0.0",
//                    "core-js@2.5.1",
//                    "font-awesome@^4.7.0",
//                    "hammerjs@2.0.8",
//                    "moment@^2.18.1",
//                    "moment-timezone@^0.5.13",
//                    "primeng@^5.0.2",
//                    "rxjs@^5.5.3",
//
//                    "@angular/cli@^1.4.5",
//                    "@angular/compiler-cli@^4.4.4",
//                    "@types/jasmine@^2.6.0",
//                    "@types/node@~8.0.32",
//                    "codelyzer@~3.2.1",
//                    "jasmine-core@~2.8.0",
//                    "jasmine-spec-reporter@~4.2.1",
//                    "karma@~1.7.1",
//                    "karma-chrome-launcher@~2.2.0",
//                    "karma-cli@~1.0.1",
//                    "karma-coverage-istanbul-reporter@^1.3.0",
//                    "karma-jasmine@~1.1.0",
//                    "karma-jasmine-html-reporter@^0.2.2",
//                    "protractor@~5.1.2",
//                    "ts-node@~3.3.0",
//                    "tslint@~5.7.0",
//                    "typescript@~2.6.0"
            });
    public static final String DEFAULT_DESTINATION = "/temp/npm";

    public static final String DEFAULT_MANIFEST = "package.json";

    @Value("${application.download.location}")
    private String destination = DEFAULT_DESTINATION;

    @Override
    public void run(ApplicationArguments args) {
        List<String> packages = TEST_DATA;

        if (args.containsOption("package")) {
            packages = args.getOptionValues("package");
        }

        List<String> manifests = Lists.newArrayList();
        if (args.containsOption("manifest")) {
            manifests = args.getOptionValues("manifest");

        } else {
            manifests.add(DEFAULT_MANIFEST);
        }

        downloadPackages(new File(DEFAULT_DESTINATION), packages, manifests);
    }

    static void downloadPackages(File destinationDirectory, List<String> packages, List<String> manifests) {
        final NodePackageDownloader downloader = NodePackageDownloader.create(destinationDirectory);

        packages.parallelStream().forEach(downloader::queuePackage);

        manifests.parallelStream().forEach(manifest -> {
            try {
                downloader.queueManifest(manifest);

            } catch (FileNotFoundException e) {
                log.error("Manifest file not found... " + manifest, e);
            }
        });

        ForkJoinPool.commonPool().awaitQuiescence(10, TimeUnit.MINUTES);

        downloader.downloadQueuedPackages();
    }

    public static void main(String[] args) {
        SpringApplication.run(NodeDownloaderApplication.class, args);
    }
}
