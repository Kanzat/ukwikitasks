package org.wikipedia.kanzatbot.copyvio;

import java.util.List;

public class CopyvioResultDto {
    public String status;
    public ErrorDto error;
    public MetaDto meta;
    public PageDto page;
    public OriginalPageDto originalPage;
    public BestMatchDto best;
    public List<SourceDto> sources;
    public DetailDto detail;

    // Constructors, getters, and setters

    public static class ErrorDto {
        public String code;
        public String info;
    }
    public static class MetaDto {
        public float time;
        public int queries;
        public boolean cached;
        public boolean redirected;
        public String cacheTime;

        // Constructors, getters, and setters
    }

    public static class PageDto {
        public String title;
        public String url;

        // Constructors, getters, and setters
    }

    public static class OriginalPageDto {
        public String title;
        public String url;

        // Constructors, getters, and setters
    }

    public static class BestMatchDto {
        public String url;
        public float confidence;
        public String violation;

        // Constructors, getters, and setters
    }

    public static class SourceDto {
        public String url;
        public float confidence;
        public String violation;
        public boolean skipped;
        public boolean excluded;

        // Constructors, getters, and setters
    }

    public static class DetailDto {
        public String article;
        public String source;

        // Constructors, getters, and setters
    }
}
