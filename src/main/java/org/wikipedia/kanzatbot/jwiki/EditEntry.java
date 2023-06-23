package org.wikipedia.kanzatbot.jwiki;

import lombok.Getter;

import java.time.Instant;

@Getter
public class EditEntry {
    public String title;
    public String user;
    public Integer userid;
    public Instant timestamp;
    public String redirect;
    public String comment;
    public long revid;
    public long old_revid;
    public String userhidden;
    public String commenthidden;
}
