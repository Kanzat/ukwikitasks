package org.wikipedia.kanzatbot.auth;

import org.fastily.jwiki.core.Wiki;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Auth {

    // On Toolforge these come from `toolforge envvars`, locally from environment variables or --BOT_USER=... arguments
    @Value("${BOT_USER}")
    private String user;
    @Value("${BOT_PASSWORD}")
    private String password;

    public void auth(Wiki wiki) {
        final boolean success = wiki.login(user, password);
        if (!success) {
            throw new IllegalStateException("Failed to login into " + wiki.toString() + " as user " + user);
        }
    }

}
