package com.doceditor.storage;

import com.doceditor.document.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalMarkdownStorage {
    private final Path docsDir;
    private final ObjectMapper mapper;

    public LocalMarkdownStorage() {
        this(Path.of(System.getProperty("user.home"), ".doceditor", "documents"));
    }

    public LocalMarkdownStorage(Path docsDir) {
        this.docsDir = docsDir;
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        ensureDirectoryExists();
    }

    public void save(Document doc) {
        try {
            Files.writeString(docsDir.resolve(doc.getId() + ".md"), doc.getMarkdownContent());
            DocumentMeta meta = DocumentMeta.fromDocument(doc);
            mapper.writeValue(docsDir.resolve(doc.getId() + ".meta.json").toFile(), meta);
        } catch (IOException e) {
            throw new StorageException("Failed to save document " + doc.getId(), e);
        }
    }

    public Document load(UUID id) {
        try {
            Path contentPath = docsDir.resolve(id + ".md");
            Path metaPath = docsDir.resolve(id + ".meta.json");
            if (!Files.exists(contentPath) || !Files.exists(metaPath)) return null;
            String content = Files.readString(contentPath, StandardCharsets.UTF_8);
            DocumentMeta meta = mapper.readValue(metaPath.toFile(), DocumentMeta.class);
            return meta.toDocument(content);
        } catch (IOException e) {
            return null;
        }
    }

    public void delete(UUID id) {
        try {
            Files.deleteIfExists(docsDir.resolve(id + ".md"));
            Files.deleteIfExists(docsDir.resolve(id + ".meta.json"));
        } catch (IOException e) {
            throw new StorageException("Failed to delete document " + id, e);
        }
    }

    public List<Document> listAll() {
        try (Stream<Path> stream = Files.list(docsDir)) {
            return stream.filter(p -> p.toString().endsWith(".meta.json"))
                    .map(p -> {
                        String idStr = p.getFileName().toString().replace(".meta.json", "");
                        return load(UUID.fromString(idStr));
                    })
                    .filter(d -> d != null)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(docsDir);
        } catch (IOException e) {
            throw new StorageException("Cannot create storage directory: " + docsDir, e);
        }
    }
}
