package com.doceditor.ui.viewmodel;

import com.doceditor.app.DocumentService;
import com.doceditor.document.Document;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.UUID;

public class DocumentViewModel {
    private final ObservableList<Document> documents = FXCollections.observableArrayList();
    private final ObjectProperty<Document> currentDocument = new SimpleObjectProperty<>();
    private final StringProperty markdownContent = new SimpleStringProperty("");
    private final StringProperty documentTitle = new SimpleStringProperty("Untitled");
    private final BooleanProperty isDirty = new SimpleBooleanProperty(false);
    private final StringProperty statusMessage = new SimpleStringProperty("Ready");
    private final BooleanProperty numberSections = new SimpleBooleanProperty(false);
    private final BooleanProperty tableOfContents = new SimpleBooleanProperty(false);
    private final ObservableList<String> templates = FXCollections.observableArrayList();
    private final StringProperty selectedTemplate = new SimpleStringProperty(null);
    private final DocumentService documentService;
    private boolean loading = false;

    public DocumentViewModel(DocumentService documentService) {
        this.documentService = documentService;
        markdownContent.addListener((obs, old, newVal) -> { if (!loading) isDirty.set(true); });
        documentTitle.addListener((obs, old, newVal) -> { if (!loading) isDirty.set(true); });
        numberSections.addListener((obs, old, newVal) -> { if (!loading) isDirty.set(true); });
        tableOfContents.addListener((obs, old, newVal) -> { if (!loading) isDirty.set(true); });
    }

    public void loadAllDocuments() {
        documents.setAll(documentService.listAll());
    }

    public void newDocument(String title) {
        Document doc = documentService.create(title);
        documents.add(0, doc);
        openDocument(doc);
        statusMessage.set("Created: " + title);
    }

    public void openDocument(Document doc) {
        loading = true;
        currentDocument.set(doc);
        documentTitle.set(doc.getTitle());
        markdownContent.set(doc.getMarkdownContent());
        isDirty.set(false);
        loading = false;
        statusMessage.set("Opened: " + doc.getTitle());
    }

    public void saveCurrentDocument() {
        Document doc = currentDocument.get();
        if (doc == null) return;
        doc.setTitle(documentTitle.get());
        doc.setMarkdownContent(markdownContent.get());
        documentService.save(doc);
        isDirty.set(false);
        int idx = documents.indexOf(doc);
        if (idx >= 0) documents.set(idx, doc);
        statusMessage.set("Saved: " + doc.getTitle());
    }

    public void deleteDocument(UUID id) {
        documentService.delete(id);
        documents.removeIf(d -> d.getId().equals(id));
        if (currentDocument.get() != null && currentDocument.get().getId().equals(id)) {
            currentDocument.set(null);
            markdownContent.set("");
            documentTitle.set("");
            isDirty.set(false);
        }
        statusMessage.set("Deleted document");
    }

    public void loadTemplates() {
        java.io.File dir = new java.io.File("templates");
        if (!dir.exists()) dir.mkdirs();
        java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".docx"));
        templates.clear();
        if (files != null) {
            for (java.io.File f : files) templates.add(f.getName());
        }
    }

    public void addTemplate(java.io.File source) {
        try {
            java.io.File target = new java.io.File("templates", source.getName());
            java.nio.file.Files.copy(source.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            loadTemplates();
            selectedTemplate.set(target.getName());
            statusMessage.set("Added template: " + source.getName());
        } catch (java.io.IOException e) {
            statusMessage.set("Failed to add template: " + e.getMessage());
        }
    }

    public ObservableList<Document> documentsProperty() { return documents; }
    public ObjectProperty<Document> currentDocumentProperty() { return currentDocument; }
    public StringProperty markdownContentProperty() { return markdownContent; }
    public StringProperty documentTitleProperty() { return documentTitle; }
    public BooleanProperty isDirtyProperty() { return isDirty; }
    public StringProperty statusMessageProperty() { return statusMessage; }
    public BooleanProperty numberSectionsProperty() { return numberSections; }
    public BooleanProperty tableOfContentsProperty() { return tableOfContents; }
    public ObservableList<String> templatesProperty() { return templates; }
    public StringProperty selectedTemplateProperty() { return selectedTemplate; }
    public Document getCurrentDocument() { return currentDocument.get(); }
    public boolean isDirty() { return isDirty.get(); }
}
