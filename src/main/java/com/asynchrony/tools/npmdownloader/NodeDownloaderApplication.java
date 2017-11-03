package com.asynchrony.tools.npmdownloader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.Provider;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootApplication
public class NodeDownloaderApplication implements ApplicationRunner {
    private static final List<String> TEST_DATA = Arrays.asList("@angular/animations@4.4.5 @angular/common@4.4.5 @angular/compiler@4.4.5 @angular/core@4.4.5 @angular/forms@4.4.5 @angular/http@4.4.5 @angular/platform-browser@4.4.5 @angular/platform-browser-dynamic@4.4.5 @angular/platform-server@4.4.5 @angular/router@4.4.5 @ngrx/store@4.0.3 @ngrx/effects@4.0.5 @ngrx/router-store@4.0.4 @ngrx/store-devtools@4.0.0 core-js@2.5.1 d3@4.11.0 font-awesome@4.7.0 hammerjs@2.0.8 moment@2.18.1 moment-timezone@0.5.13 primeng@4.1.0 rxjs@5.4.3 zone.js@0.8.18".split(" "));
    public static final String DEFAULT_DESTINATION = "c:\\temp\\npm";

    @Value("${application.download.location}")
    private String destination = DEFAULT_DESTINATION;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<String> packages = TEST_DATA;

        if(args.containsOption("package")){
            packages = args.getOptionValues("package");
        }

        downloadPackages(new File(DEFAULT_DESTINATION), packages);
    }

    static void downloadPackages(File destinationDirectory, List<String> packages) {
        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdirs();
            destinationDirectory.mkdir();
        }

        ForkJoinPool packageDownloaderFJP = new ForkJoinPool(5);
        packages.parallelStream()
                .map(NodePackage::create)
                .peek(pkg -> log.info(pkg.toString()))
                .forEach(pkg -> packageDownloaderFJP.invoke(PackageDownloader.create(destinationDirectory, pkg)));

        CompletableFuture.runAsync(() -> {
            try {
                log.info("Awaiting termination...");
                packageDownloaderFJP.awaitTermination(10, TimeUnit.MINUTES);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }, Executors.newSingleThreadExecutor())
                .thenRun(() -> {
                    log.info("All Done!");

                    System.exit(0);
                });
    }

    public static void main(String[] args) {
        SpringApplication.run(NodeDownloaderApplication.class, args);
    }
}
