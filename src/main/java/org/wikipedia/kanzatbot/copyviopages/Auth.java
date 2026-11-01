/*
 * Decompiled with CFR 0.152.
 */
package org.wikipedia.kanzatbot.copyviopages;

import java.io.FileInputStream;
import java.util.Properties;
import org.fastily.jwiki.core.Wiki;

public class Auth {
    public static String credentialsFile;
    private static String user;
    private static String password;
    private static boolean initialized;

    public static void auth(Wiki wiki) {
        boolean success;
        if (!initialized) {
            try {
                Properties prop = new Properties();
                prop.load(new FileInputStream(credentialsFile));
                user = prop.getProperty("user");
                password = prop.getProperty("password");
                initialized = true;
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (!(success = wiki.login(user, password))) {
            throw new IllegalStateException("Failed to login into " + wiki.toString() + " as user " + user);
        }
    }

    static {
        initialized = false;
    }
}

