package com.tecspace.portal.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Stops the app with a clear message if the secrets are missing, and warns if they look like the old defaults. */
@Component
public class SecretsCheck {

    private static final Logger log = LoggerFactory.getLogger(SecretsCheck.class);

    @Value("${tecspace.jwt.secret:}")
    private String jwtSecret;

    @Value("${tecspace.admin.password:}")
    private String adminPassword;

    @PostConstruct
    void check() {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                    "The JWT secret is missing or too short (it needs at least 32 characters). "
                            + "Set the JWT_SECRET environment variable, or put tecspace.jwt.secret in application-local.properties "
                            + "and start with the 'local' profile. See README-PHASE8.md.");
        }
        if (jwtSecret.contains("ChangeThisSecret")) {
            log.warn("The JWT secret is still the example value from the tutorial. Change it before sharing the project.");
        }
        if ("Admin@12345".equals(adminPassword)) {
            log.warn("The admin password is still the example value. Change it before sharing the project.");
        }
    }
}
