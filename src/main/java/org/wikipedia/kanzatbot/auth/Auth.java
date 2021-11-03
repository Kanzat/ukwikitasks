package org.wikipedia.kanzatbot.auth;

import org.fastily.jwiki.core.Wiki;

import java.io.FileInputStream;
import java.util.Properties;

public class Auth {

    public static String credentialsFile;
    private static String user;
    private static String password;
    private static boolean initialized = false;

    public static void auth(Wiki wiki) {
        if (!initialized) {
            try {
                Properties prop = new Properties();
                prop.load(new FileInputStream(credentialsFile));
                user = prop.getProperty("user");
                password = prop.getProperty("password");
                initialized = true;
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
        final boolean success = wiki.login(user, password);
        if (!success) {
            throw new IllegalStateException("Failed to login into " + wiki.toString() + " as user " + user);
        }
    }

}
