package com.doceditor.storage;

import com.doceditor.document.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DocumentMeta(
        UUID id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DocumentMeta fromDocument(Document d) {
        return new DocumentMeta(d.getId(), d.getTitle(), d.getCreatedAt(), d.getUpdatedAt());
    }

    public Document toDocument(String content) {
        return Document.builder()
                .id(id)
                .title(title)
                .markdownContent(content)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
