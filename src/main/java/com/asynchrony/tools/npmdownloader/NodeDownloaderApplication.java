package com.asynchrony.tools.npmdownloader;

import com.asynchrony.tools.npmdownloader.model.NodePackageParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Slf4j
@SpringBootApplication
public class NodeDownloaderApplication implements ApplicationRunner {
    private static final List<String> TEST_DATA =
            Arrays.asList(new String[]{
                    "@angular/animations@4.4.5",
                    "@angular/common@4.4.5",
                    "@angular/compiler@4.4.5",
                    "@angular/core@4.4.5",
                    "@angular/forms@4.4.5",
                    "@angular/http@4.4.5",
                    "@angular/platform-browser@4.4.5",
                    "@angular/platform-browser-dynamic@4.4.5",
                    "@angular/platform-server@4.4.5",
                    "@angular/router@4.4.5",
                    "@ngrx/store@4.0.3",
                    "@ngrx/effects@4.0.5",
                    "@ngrx/router-store@4.0.4",
                    "@ngrx/store-devtools@4.0.0",
                    "core-js@2.5.1",
                    "d3@4.11.0",
                    "font-awesome@4.7.0",
                    "hammerjs@2.0.8",
                    "moment@2.18.1",
                    "moment-timezone@0.5.13",
                    "primeng@4.1.0",
                    "rxjs@5.4.3",

                    "@angular/cli@^1.4.5",
                    "@angular/compiler-cli@^4.4.4",
                    "@types/jasmine@2.6.0",
                    "@types/node@~8.0.32",
                    "codelyzer@~3.2.1",
                    "jasmine-core@~2.8.0",
                    "jasmine-spec-reporter@~4.2.1",
                    "karma@~1.7.1",
                    "karma-chrome-launcher@~2.2.0",
                    "karma-cli@~1.0.1",
                    "karma-coverage-istanbul-reporter@^1.3.0",
                    "karma-jasmine@~1.1.0",
                    "karma-jasmine-html-reporter@^0.2.2",
                    "protractor@~5.1.2",
                    "ts-node@~3.3.0",
                    "tslint@~5.7.0",
                    "typescript@~2.3.0"
            });
    public static final String DEFAULT_DESTINATION = "/temp/npm";

    @Value("${application.download.location}")
    private String destination = DEFAULT_DESTINATION;

    @Override
    public void run(ApplicationArguments args) {
        List<String> packages = TEST_DATA;

        if (args.containsOption("package")) {
            packages = args.getOptionValues("package");
        }

        downloadPackages(new File(DEFAULT_DESTINATION), packages);
    }

    static void downloadPackages(File destinationDirectory, List<String> packages) {
        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdirs();
            destinationDirectory.mkdir();
        }

        final NodePackageDownloader downloader = NodePackageDownloader.create(destinationDirectory);

        packages.parallelStream()
                .map(NodePackageParser::parsePackageSpecString)
                .forEach(pkg -> pkg.ifPresent(downloader::queuePackage));

        downloader.printDownloadQueue();
    }

    public static void main(String[] args) {
        SpringApplication.run(NodeDownloaderApplication.class, args);
    }
}
