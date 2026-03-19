package com.doceditor.app;

import com.doceditor.document.Document;
import com.doceditor.storage.LocalMarkdownStorage;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class DocumentService {
    private final LocalMarkdownStorage storage;

    public Document create(String title) {
        Document doc = Document.builder()
                .title(title)
                .markdownContent("# " + title + "\n\n")
                .build();
        storage.save(doc);
        return doc;
    }

    public void save(Document document) {
        storage.save(document);
    }

    public Document open(UUID id) {
        return storage.load(id);
    }

    public List<Document> listAll() {
        return storage.listAll();
    }

    public void delete(UUID id) {
        storage.delete(id);
    }

    public void rename(Document document, String newTitle) {
        document.setTitle(newTitle);
        storage.save(document);
    }
}
