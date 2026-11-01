package org.wikipedia.kanzatbot;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WikiFactoryTest {

    @Test
    public void linksTheBotUserPage() {
        assertEquals("ukwikitasks (https://uk.wikipedia.org/wiki/User:KanzatBot)", WikiFactory.userAgent("KanzatBot"));
    }

    @Test
    public void encodesNonAsciiUserNames() {
        assertEquals("ukwikitasks (https://uk.wikipedia.org/wiki/User:%D0%91%D0%BE%D1%82_%D0%9A)", WikiFactory.userAgent("Бот К"));
    }

}
