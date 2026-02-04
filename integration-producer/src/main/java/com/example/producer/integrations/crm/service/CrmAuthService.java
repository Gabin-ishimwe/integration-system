package com.example.producer.integrations.crm.service;

import com.example.producer.common.model.AuthRequest;
import com.example.producer.common.model.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Service
public class CrmAuthService {

    private static final Logger log = LoggerFactory.getLogger(CrmAuthService.class);
    private static final String TOKEN_CACHE_KEY = "crm-service:token";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(55);

    private final RestTemplate restTemplate;
    private final StringRedisTemplate redisTemplate;

    @Value("${mock-service.base-url}")
    private String mockServiceUrl;

    @Value("${mock-service.auth.username:crm_user}")
    private String username;

    @Value("${mock-service.auth.password:crm_password}")
    private String password;

    public CrmAuthService(StringRedisTemplate redisTemplate) {
        this.restTemplate = new RestTemplate();
        this.redisTemplate = redisTemplate;
    }

    public String getToken() {
        String cachedToken = redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
        if (cachedToken != null) {
            log.debug("Using cached CRM token");
            return cachedToken;
        }

        log.info("Fetching new CRM token");
        String token = fetchNewToken();
        redisTemplate.opsForValue().set(TOKEN_CACHE_KEY, token, TOKEN_TTL);
        return token;
    }

    private String fetchNewToken() {
        String authUrl = mockServiceUrl + "/auth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        AuthRequest request = new AuthRequest(username, password);
        HttpEntity<AuthRequest> entity = new HttpEntity<>(request, headers);

        TokenResponse response = restTemplate.postForObject(authUrl, entity, TokenResponse.class);

        if (response == null || response.getAccessToken() == null) {
            throw new RuntimeException("Failed to obtain CRM token");
        }

        return response.getAccessToken();
    }
}
