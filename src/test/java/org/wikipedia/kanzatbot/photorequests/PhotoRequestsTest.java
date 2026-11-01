package org.wikipedia.kanzatbot.photorequests;

import org.junit.Test;

import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PhotoRequestsTest {

    @Test
    public void recognizesTemplate() {
        assertTrue(PhotoRequests.isCanPhotographTemplate("Можу сфотографувати"));
        assertTrue(PhotoRequests.isCanPhotographTemplate("Шаблон:Можу сфотографувати"));
        assertTrue(PhotoRequests.isCanPhotographTemplate("можу_сфотографувати"));
        assertTrue(PhotoRequests.isCanPhotographTemplate("Шаблон: Можу сфотографувати"));
        assertFalse(PhotoRequests.isCanPhotographTemplate("Можу СФОТОГРАФУВАТИ"));
        assertFalse(PhotoRequests.isCanPhotographTemplate("МОЖУ сфотографувати"));
        assertFalse(PhotoRequests.isCanPhotographTemplate(""));
        assertFalse(PhotoRequests.isCanPhotographTemplate("Можу сфотографувати/документація"));
        assertFalse(PhotoRequests.isCanPhotographTemplate("Користувач Київ"));
    }

    @Test
    public void rendersPage() {
        TreeMap<String, String> userToLocation = new TreeMap<>();
        userToLocation.put("Лемберг", "у Львові");
        userToLocation.put("Alkom-lviv", "у [[Львів|Львові]], [[Броди|Бродах]]");
        TreeSet<String> noLocationUsers = new TreeSet<>();
        noLocationUsers.add("Tedir");
        noLocationUsers.add("DR");

        assertEquals("Перелік користувачів, які можуть зробити фотографію, відповідно до шаблону {{tl|Можу сфотографувати}}. "
                        + "Ця сторінка допоміжна для виконання запитів на зображення (див. [[Вікіпедія:Запити на зображення]]).\n\n"
                        + "{| class=\"wikitable sortable\"\n"
                        + "|+\n"
                        + "! Користувач(ка)\n"
                        + "! Де може сфотографувати\n"
                        + "|-\n"
                        + "|[[User:Alkom-lviv|Alkom-lviv]]\n"
                        + "|у [[Львів|Львові]], [[Броди|Бродах]]\n"
                        + "|-\n"
                        + "|[[User:Лемберг|Лемберг]]\n"
                        + "|у Львові\n"
                        + "|}\n"
                        + "\n"
                        + "Користувачі, які поставили шаблон, але не зазначили місцевість: [[User:DR|DR]], [[User:Tedir|Tedir]]",
                PhotoRequests.renderPage(userToLocation, noLocationUsers));
    }

    @Test
    public void omitsNoLocationLineWhenEveryoneHasLocation() {
        TreeMap<String, String> userToLocation = new TreeMap<>();
        userToLocation.put("Лемберг", "у Львові");

        assertTrue(PhotoRequests.renderPage(userToLocation, new TreeSet<>()).endsWith("|}\n"));
    }

}
