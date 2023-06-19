package org.wikipedia.kanzatbot.vpvylhelper;

import lombok.AllArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class PageDeletion {
    String location;
    String title;
    PageDeletionStatus status;
    Integer votedFor;
    Integer votedAgainst;
}
