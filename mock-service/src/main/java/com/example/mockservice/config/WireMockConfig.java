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

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@Configuration
public class WireMockConfig {

    private static final Logger log = LoggerFactory.getLogger(WireMockConfig.class);

    @Value("${wiremock.server.port:8081}")
    private int wireMockPort;

    @Value("${wiremock.server.verbose:true}")
    private boolean verbose;

    private WireMockServer wireMockServer;

    @Bean
    public WireMockServer wireMockServer() {
        WireMockConfiguration config = options()
            .port(wireMockPort)
            .usingFilesUnderClasspath("wiremock")
            .globalTemplating(true)
            .notifier(new ConsoleNotifier(verbose));

        wireMockServer = new WireMockServer(config);
        wireMockServer.start();

        log.info("WireMock server started on port {}", wireMockPort);
        log.info("Loaded {} stub mappings", wireMockServer.getStubMappings().size());

        return wireMockServer;
    }

    @PreDestroy
    public void stopWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
            log.info("WireMock server stopped");
        }
    }
}
