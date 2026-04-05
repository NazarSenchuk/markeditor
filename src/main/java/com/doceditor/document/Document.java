package com.doceditor.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Document {
    private UUID id;
    private String title;
    private String markdownContent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Document() {
        this.id = UUID.randomUUID();
        this.markdownContent = "";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Document(String title) {
        this();
        this.title = title;
    }

    public Document(UUID id, String title, String markdownContent, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.title = title;
        this.markdownContent = markdownContent != null ? markdownContent : "";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMarkdownContent() { return markdownContent; }
    public void setMarkdownContent(String markdownContent) { this.markdownContent = markdownContent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getSummary() {
        if (markdownContent == null || markdownContent.isBlank()) return "(empty)";
        String plain = markdownContent.replaceAll("#+ ", "").replaceAll("\n", " ").strip();
        return plain.length() > 100 ? plain.substring(0, 100) + "…" : plain;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String title;
        private String markdownContent;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder markdownContent(String markdownContent) { this.markdownContent = markdownContent; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Document build() {
            return new Document(id, title, markdownContent, createdAt, updatedAt);
        }
    }
}
