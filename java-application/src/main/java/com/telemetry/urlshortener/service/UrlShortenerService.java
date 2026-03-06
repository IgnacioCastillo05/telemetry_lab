package com.telemetry.urlshortener.service;

import com.telemetry.urlshortener.model.UrlMapping;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UrlShortenerService {

    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerService.class);
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 6;

    private final Map<String, UrlMapping> urlStorage = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private final Counter urlShortenedCounter;
    private final Counter urlAccessedCounter;
    private final Counter urlNotFoundCounter;
    private final Counter customCodeConflictCounter;

    public UrlShortenerService(MeterRegistry meterRegistry) {
        logger.info("UrlShortenerService initialized with in-memory storage");
        this.urlShortenedCounter = Counter.builder("urlShortened")
                .description("Counter for shortened URLs")
                .register(meterRegistry);
        this.urlAccessedCounter = Counter.builder("urlAccessed")
                .description("Counter for accessed URLs")
                .register(meterRegistry);
        this.urlNotFoundCounter = Counter.builder("urlNotFound")
                .description("Counter for not found URLs")
                .register(meterRegistry);
        this.customCodeConflictCounter = Counter.builder("customCodeConflict")
                .description("Counter for custom code conflicts")
                .register(meterRegistry);
    }

    private void simulateLatency() {
        int delay = 50 + random.nextInt(450);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.debug("Simulated latency: {} ms", delay);
    }

    /**
     * Acorta una URL y retorna el código corto
     */
    public UrlMapping shortenUrl(String originalUrl, String customCode) {
        simulateLatency();
        String shortCode;
        if (customCode != null && !customCode.isEmpty()) {
            if (urlStorage.containsKey(customCode)) {
                logger.warn("Custom code already exists: {}", customCode);
                throw new IllegalArgumentException("Custom code already exists: " + customCode);
            }
            shortCode = customCode;
        } else {
            shortCode = generateShortCode();
        }

        UrlMapping mapping = new UrlMapping(shortCode, originalUrl);
        urlStorage.put(shortCode, mapping);

        logger.info("URL shortened - Code: {}, Original URL: {}", shortCode, originalUrl);
        return mapping;
    }

    /**
     * Obtiene la URL original a partir del código corto
     */
    public Optional<UrlMapping> getOriginalUrl(String shortCode) {
        simulateLatency();
        UrlMapping mapping = urlStorage.get(shortCode);
        if (mapping != null) {
            logger.info("URL accessed - Code: {}", shortCode);
        } else {
            logger.warn("Short code not found: {}", shortCode);
        }
        return Optional.ofNullable(mapping);
    }

    /**
     * Obtiene todas las URLs almacenadas
     */
    public Map<String, UrlMapping> getAllUrls() {
        simulateLatency();
        logger.debug("Retrieving all URLs - Total: {}", urlStorage.size());
        return Map.copyOf(urlStorage);
    }

    /**
     * Genera un código corto aleatorio único
     */
    private String generateShortCode() {
        String code;
        do {
            code = random.ints(SHORT_CODE_LENGTH, 0, CHARACTERS.length())
                    .mapToObj(CHARACTERS::charAt)
                    .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                    .toString();
        } while (urlStorage.containsKey(code));

        return code;
    }
}
