package com.asynchrony.tools.npmdownloader;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootApplication
public class NpmPackageDownloaderApplication extends JFrame {
    public NpmPackageDownloaderApplication() {
        this.initUserInterface();
    }

    private static final String TEST_DATA = "@angular/animations@4.4.5 @angular/common@4.4.5 @angular/compiler@4.4.5 @angular/core@4.4.5 @angular/forms@4.4.5 @angular/http@4.4.5 @angular/platform-browser@4.4.5 @angular/platform-browser-dynamic@4.4.5 @angular/platform-server@4.4.5 @angular/router@4.4.5 @ngrx/store@4.0.3 @ngrx/effects@4.0.5 @ngrx/router-store@4.0.4 @ngrx/store-devtools@4.0.0 core-js@2.5.1 d3@4.11.0 font-awesome@4.7.0 hammerjs@2.0.8 moment@2.18.1 moment-timezone@0.5.13 primeng@4.1.0 rxjs@5.4.3 zone.js@0.8.18";

    private JTextArea packages;

    private void initUserInterface() {
        JButton downloadButton = new JButton("Download");
        downloadButton.addActionListener(this::downloadAction);

        JButton quitButton = new JButton("Quit");
        quitButton.addActionListener(NpmPackageDownloaderApplication::exit);

        this.packages = new JTextArea(TEST_DATA, 5, 50);
        this.packages.setLineWrap(true);

        createLayout(this.packages, downloadButton, quitButton);

        setTitle("NPM Package Downloader");
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void createLayout(JTextArea packages, JButton... buttons) {
        packages.setAlignmentX(LEFT_ALIGNMENT);

        JScrollPane listScroller = new JScrollPane(packages);
        listScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        listScroller.setAlignmentX(LEFT_ALIGNMENT);

        // Create a container so that we can add a title around the scroll pane.
        JPanel listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
        JLabel label = new JLabel("Packages to download");
        label.setLabelFor(packages);
        listPane.add(label);
        listPane.add(Box.createRigidArea(new Dimension(0, 5)));
        listPane.add(listScroller);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Lay out the buttons from left to right.
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());

        for (JButton button : buttons) {
            buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
            buttonPane.add(button);
        }

        JPanel interfacePane = new JPanel();
        interfacePane.setLayout(new BorderLayout());
        interfacePane.add(listPane, BorderLayout.CENTER);
        interfacePane.add(buttonPane, BorderLayout.PAGE_END);

        JTextPane consoleTextPane = new JTextPane();
        JScrollPane consoleScroller = new JScrollPane(consoleTextPane);
        consoleScroller.setPreferredSize(new Dimension(250, 100));

        MessageConsole mc = new MessageConsole(consoleTextPane);
        mc.redirectOut();
        mc.redirectErr(Color.RED, null);
        mc.setMessageLines(100);

        // Create a split pane with the two scroll panes in it.
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, interfacePane, consoleScroller);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(400);

        // Provide minimum sizes for the two components in the split pane
        Dimension minimumSize = new Dimension(100, 50);
        interfacePane.setMinimumSize(minimumSize);
        consoleTextPane.setMinimumSize(minimumSize);

        Container contentPane = getContentPane();
        contentPane.add(splitPane, BorderLayout.CENTER);
    }

    private static void exit(ActionEvent event) {
        System.exit(0);
    }

    private void downloadAction(ActionEvent event) {
        StringWriter packagesString = new StringWriter();
        try {
            this.packages.write(packagesString);

            String[] packages = packagesString.getBuffer().toString().split("(\\r|\\n|\\r\\n|\\s)+");

            downloadPackages(new File("c:\\temp\\npm"), packages);

        } catch (IOException e) {
            e.printStackTrace();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void downloadPackages(File destinationDirectory, String[] packages) throws IOException, InterruptedException {
        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdirs();
            destinationDirectory.mkdir();
        }

        final ExecutorService packageDownloaderExecutor = Executors.newFixedThreadPool(5);
        for (int i = 0; i < packages.length; i++) {
            String scopedPackageName = packages[i].substring(0, packages[i].lastIndexOf('@'));
            String packageVersion = packages[i].substring(packages[i].lastIndexOf('@') + 1);

            CompletableFuture.runAsync(() -> downloadPackage(destinationDirectory, scopedPackageName, packageVersion), packageDownloaderExecutor)
                    .thenRun(() -> EventQueue.invokeLater(() -> log.info("Package " + scopedPackageName + " complete")));
        }

        packageDownloaderExecutor.shutdown();

        CompletableFuture.runAsync(() -> {
            try {
                packageDownloaderExecutor.awaitTermination(10, TimeUnit.MINUTES);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }, Executors.newSingleThreadExecutor())
                .thenRun(() -> EventQueue.invokeLater(() -> log.info("All Done!")));
    }

    static String downloadPackage(final File destinationDirectory, final String scopedPackageName, final String packageVersion) {
        String packageScope = null, packageName;
        if (scopedPackageName.contains("/")) {
            String[] scopeSplits = scopedPackageName.split("/");
            packageScope = scopeSplits[0];
            packageName = scopeSplits[1];
        } else {
            packageName = scopedPackageName;
        }

        try {
            URL packageURL = new URL("http://registry.npmjs.org/" + scopedPackageName + "/-/" + scopedPackageName + "-" + packageVersion + ".tgz");

            try (InputStream packageStream = packageURL.openStream()) {
                EventQueue.invokeLater(() -> log.debug("Downloading... " + packageURL.toExternalForm()));

                File packageDirectory = destinationDirectory;
                if (packageScope != null) {
                    packageDirectory = new File(destinationDirectory, packageScope);
                    packageDirectory.mkdir();
                }

                File destinationFile = new File(packageDirectory, packageName + "-" + packageVersion + ".tgz");

                try (FileOutputStream packageOutputStream = new FileOutputStream(destinationFile)) {
                    final String fileName = destinationFile.getCanonicalPath();
                    EventQueue.invokeLater(() -> log.debug("Writing file... " + fileName));
                    IOUtils.copy(packageStream, packageOutputStream);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return scopedPackageName;
    }

    private static final JacksonJsonParser parser = new JacksonJsonParser();
    static String extractPackageJson(File packageFile) {
        try (FileInputStream packageFileInputStream = new FileInputStream(packageFile)) {
            TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new GzipCompressorInputStream(packageFileInputStream));

            for (ArchiveEntry entry = tarArchiveInputStream.getNextEntry(); entry != null; entry = tarArchiveInputStream.getNextEntry()) {
                if (entry.getName().endsWith("package.json")) {
                    byte[] content = new byte[(int)entry.getSize()];
                    tarArchiveInputStream.read(content, 0, content.length);

                    String packageJsonContent = new String(content);

                    parser.parseMap(packageJsonContent);

                    break;
                }
            }

        } catch (FileNotFoundException e) {
            log.error("Package archive file " + packageFile.getAbsolutePath() + " not found!", e);

        } catch (IOException e) {
            log.error(e.getClass().getSimpleName() + " reading package archive file " + packageFile.getAbsolutePath(), e);
        }

        return "";
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext applicationContext =
                new SpringApplicationBuilder(NpmPackageDownloaderApplication.class)
                        .headless(false)
                        .run(args);

        EventQueue.invokeLater(() -> {
            NpmPackageDownloaderApplication ex = applicationContext.getBean(NpmPackageDownloaderApplication.class);
            ex.setVisible(true);
        });
    }
}
