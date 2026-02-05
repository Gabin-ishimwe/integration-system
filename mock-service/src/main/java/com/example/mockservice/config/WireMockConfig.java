package com.example.mockservice.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@Configuration
public class WireMockConfig {

    private static final Logger log = LoggerFactory.getLogger(WireMockConfig.class);

    @Value("${wiremock.server.port:8081}")
    private int wireMockPort;

    @Value("${wiremock.server.verbose:true}")
    private boolean verbose;

    private WireMockServer wireMockServer;
    private Path wireMockRootDir;

    @Bean
    public WireMockServer wireMockServer() throws IOException {
        wireMockRootDir = extractWireMockResources();

        WireMockConfiguration config = options()
            .port(wireMockPort)
            .withRootDirectory(wireMockRootDir.toString())
            .globalTemplating(true)
            .notifier(new ConsoleNotifier(verbose));

        wireMockServer = new WireMockServer(config);
        wireMockServer.start();

        log.info("WireMock server started on port {}", wireMockPort);
        log.info("WireMock root directory: {}", wireMockRootDir);
        log.info("Loaded {} stub mappings", wireMockServer.getStubMappings().size());

        return wireMockServer;
    }

    private Path extractWireMockResources() throws IOException {
        Path tempDir = Files.createTempDirectory("wiremock");
        Path mappingsDir = tempDir.resolve("mappings");
        Path filesDir = tempDir.resolve("__files");

        Files.createDirectories(mappingsDir);
        Files.createDirectories(filesDir);

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        // Extract mapping files
        Resource[] mappings = resolver.getResources("classpath:wiremock/mappings/*.json");
        for (Resource mapping : mappings) {
            String filename = mapping.getFilename();
            if (filename != null) {
                Path targetPath = mappingsDir.resolve(filename);
                try (InputStream is = mapping.getInputStream()) {
                    Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    log.debug("Extracted mapping: {}", filename);
                }
            }
        }
        log.info("Extracted {} WireMock mappings to {}", mappings.length, mappingsDir);

        // Extract __files (response bodies) - recursively
        Resource[] files = resolver.getResources("classpath:wiremock/__files/**/*");
        for (Resource file : files) {
            if (file.isReadable()) {
                String uri = file.getURI().toString();
                int index = uri.indexOf("wiremock/__files/");
                if (index != -1) {
                    String relativePath = uri.substring(index + "wiremock/__files/".length());
                    Path targetPath = filesDir.resolve(relativePath);
                    Files.createDirectories(targetPath.getParent());
                    try (InputStream is = file.getInputStream()) {
                        Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        log.debug("Extracted file: {}", relativePath);
                    }
                }
            }
        }
        log.info("Extracted WireMock __files to {}", filesDir);

        return tempDir;
    }

    @PreDestroy
    public void stopWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
            log.info("WireMock server stopped");
        }
        // Clean up temp directory
        if (wireMockRootDir != null) {
            try {
                Files.walk(wireMockRootDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("Failed to delete temp file: {}", path);
                        }
                    });
                log.info("Cleaned up WireMock temp directory");
            } catch (IOException e) {
                log.warn("Failed to clean up WireMock temp directory: {}", e.getMessage());
            }
        }
    }
}
