package com.doceditor.app;

import com.doceditor.document.Document;
import com.doceditor.storage.LocalMarkdownStorage;
import java.util.List;
import java.util.UUID;

public class DocumentService {
    private final LocalMarkdownStorage storage;

    public DocumentService(LocalMarkdownStorage storage) {
        this.storage = storage;
    }

    public Document create(String title) {
        Document doc = new Document();
        doc.setTitle(title);
        doc.setMarkdownContent("# " + title + "\n\n");
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
