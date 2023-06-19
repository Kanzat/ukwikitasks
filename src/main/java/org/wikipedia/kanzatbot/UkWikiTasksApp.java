package org.wikipedia.kanzatbot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class UkWikiTasksApp {

    public static void main(String[] args) {
        log.info("STARTING THE APPLICATION");
        SpringApplication.run(UkWikiTasksApp.class, args);
        log.info("APPLICATION FINISHED");
    }

}
