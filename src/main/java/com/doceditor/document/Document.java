package com.doceditor.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Document {
    @Builder.Default
    private UUID id = UUID.randomUUID();
    private String title;
    @Builder.Default
    private String markdownContent = "";
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public String getSummary() {
        if (markdownContent == null || markdownContent.isBlank()) return "(empty)";
        String plain = markdownContent.replaceAll("#+ ", "").replaceAll("\n", " ").strip();
        return plain.length() > 100 ? plain.substring(0, 100) + "…" : plain;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
