package org.wikipedia.kanzatbot.auth;

import lombok.extern.slf4j.Slf4j;
import org.fastily.jwiki.core.Wiki;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Slf4j
@Component
public class Auth {

    @Value("${org.kanzatbot.credentials.location}")
    private String credentialsFile;
    private String user;
    private String password;

    @PostConstruct
    public void initialize() throws IOException {
        Properties prop = new Properties();
        log.debug("Reading credentials from {}", credentialsFile);
        try (FileInputStream stream = new FileInputStream(credentialsFile)) {
            prop.load(stream);
            user = prop.getProperty("user");
            password = prop.getProperty("password");
        }
    }

    public void auth(Wiki wiki) {
        final boolean success = wiki.login(user, password);
        if (!success) {
            throw new IllegalStateException("Failed to login into " + wiki.toString() + " as user " + user);
        }
    }

}
