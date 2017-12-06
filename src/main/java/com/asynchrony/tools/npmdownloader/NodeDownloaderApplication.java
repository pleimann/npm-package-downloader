package com.asynchrony.tools.npmdownloader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.vavr.jackson.datatype.VavrModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

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
            });

    public static final String DEFAULT_MANIFEST = "package.json";

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new VavrModule());

        return mapper;
    }

    @Autowired
    private NodePackageDownloader downloader;

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

        downloadPackages(packages, manifests);
    }

    void downloadPackages(List<String> packages, List<String> manifests) {
        packages.parallelStream().forEach(this.downloader::queuePackage);

        manifests.parallelStream().forEach(manifest -> {
            try {
                this.downloader.queueManifest(manifest);

            } catch (FileNotFoundException e) {
                log.error("Manifest file not found... " + manifest, e);
            }
        });

        ForkJoinPool.commonPool().awaitQuiescence(10, TimeUnit.MINUTES);

        this.downloader.downloadQueuedPackages();
    }

    public static void main(String[] args) {
        SpringApplication.run(NodeDownloaderApplication.class, args);
    }
}
