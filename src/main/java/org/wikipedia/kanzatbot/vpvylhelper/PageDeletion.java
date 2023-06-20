package org.wikipedia.kanzatbot.vpvylhelper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

import java.util.List;

@ToString
@Builder
@AllArgsConstructor
public class PageDeletion {
    String location;
    String title;
    PageDeletionStatus status;
    List<String> votedApprove;
    List<String> votedReject;
    String nominatedBy;
    String contestedSummaryAdmin;
    String finalSummaryAdmin;
    PageDeletionCompletedStatus contestedSummaryStatus;
    PageDeletionCompletedStatus finalSummaryStatus;
}
