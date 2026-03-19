# Document Editor — Architecture (v1)

> **Core idea:** A document IS a Markdown file. We edit it, store it, and use Pandoc to export it.
> No custom element classes. No custom template engine. No S3 in v1 — local-only.

---

## How It Actually Works

```
User types in editor
        │
        ▼
   Raw Markdown text              ← The document is just a .md string
        │
   ┌────┴──────────────────┐
   ▼                        ▼
Save as .md file        Pandoc CLI
(local + S3)                │
                            ▼
                   ┌────────┴────────┐
                   ▼        ▼        ▼
                Live HTML  .docx    .pdf
                 Preview
```

**Example — user inserts markdown:**
```markdown
# My Report

This is a paragraph of text.

- Item 1
- Item 2

| Col A | Col B |
|-------|-------|
| 1     | 2     |
```

→ Saved as `<uuid>.md` locally and to S3  
→ Preview rendered in `WebView` using **Pandoc** (md → HTML)  
→ Export: `pandoc input.md --reference-doc=business.docx -o output.docx`

---

## Package Structure

```
com.doceditor/
│
├── document/                        ← What a document IS
│   ├── Document.java                ← id, title, markdownContent, timestamps
│   └── DocumentMetadata.java        ← author, tags, sync status
│
├── storage/                         ← Saving and loading files locally
│   ├── LocalMarkdownStorage.java    ← Stores .md files in ~/.doceditor/documents/
│   ├── DocumentMeta.java            ← Lightweight metadata record
│   └── StorageException.java
│
├── export/                          ← Converting documents to other formats
│   ├── PandocRunner.java            ← Wraps Pandoc CLI
│   ├── ExportService.java           ← Orchestrates MD/DOCX/PDF export
│   └── ExportException.java
│
├── app/                             ← Service layer
│   └── DocumentService.java         ← Simple CRUD operations
│
└── ui/                              ← JavaFX GUI
    ├── App.java                     ← main() entry point
    ├── viewmodel/
    │   └── DocumentViewModel.java   ← StringProperty content, BooleanProperty isDirty
    └── controller/
        ├── MainController.java
        ├── EditorController.java    ← CodeArea (RichTextFX) + WebView preview
        ├── ToolbarController.java   ← Insert heading / list / table shortcuts
        └── SidebarController.java   ← Document list + sync status badges
```

---

## Key Classes (simplified)

### Document — the whole model

```java
public class Document {
    UUID   id;
    String title;
    String markdownContent;    // ← the entire document body
    String author;
    List<String> tags;
    SyncStatus syncStatus;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
```

That's it. No element list, no style map, no template field.

---

### Storage — Markdown files on disk

```
~/.doceditor/
└── documents/
    ├── <uuid>.md           ← document content
    ├── <uuid>.meta.json    ← title, author, tags, syncStatus, timestamps
    └── ...
```

- Content and metadata stored separately — metadata is tiny JSON, content is raw Markdown
- `DocumentRepository` interface allows swapping to a DB later

---

### Export — Pandoc CLI

```java
public class PandocRunner {

    // Export to DOCX, optionally applying a .docx reference template
    public Path toDocx(Path mdFile, Path outputDir, Path referenceDocx) {
        List<String> cmd = new ArrayList<>(List.of(
            "pandoc", mdFile.toString(),
            "-o", outputDir.resolve("output.docx").toString()
        ));
        if (referenceDocx != null) {
            cmd.add("--reference-doc=" + referenceDocx);
        }
        new ProcessBuilder(cmd).start().waitFor();
        return outputDir.resolve("output.docx");
    }

    // Export to PDF (via docx intermediate)
    public Path toPdf(Path mdFile, Path outputDir) {
        // pandoc input.md -o output.pdf  (uses LaTeX or wkhtmltopdf)
    }
}
```

### Templates — just `.docx` reference files

```
src/main/resources/templates/
├── default.docx        ← Pandoc's built-in defaults
├── academic.docx       ← Times New Roman, double-spaced
└── business.docx       ← Clean corporate style
```

No Java `Template` class needed. User picks a `.docx` from a dropdown → passed to `--reference-doc`.

---

### ExportService — orchestrates the pipeline

```java
public class ExportService {

    public Path exportMarkdown(Document doc, Path outputDir) {
        // Write markdownContent to file
    }

    public Path exportDocx(Document doc, Path outputDir, Path referenceTemplate) {
        Path mdFile = exportMarkdown(doc, tempDir);
        return pandocRunner.toDocx(mdFile, outputDir, referenceTemplate);
    }

    public Path exportPdf(Document doc, Path outputDir) {
        Path mdFile = exportMarkdown(doc, tempDir);
        return pandocRunner.toPdf(mdFile, outputDir);
    }
}
```

---

### JavaFX UI — Split Editor + Preview

```
┌──────────────────────────────────────────────────────────┐
│  File | Export | Sync                                    │
├──────────────────────────────────────────────────────────┤
│  [H1] [H2] [H3]  [B] [I]  [List] [Table] [Code]        │  ← inserts Markdown snippets
├───────────────────────────┬──────────────────────────────┤
│  My Report       SYNCED ✓ │                              │
│  Q1 Notes        PENDING… │   # My Report               │
│  Meeting Notes   LOCAL    │                              │
│                           │   This is a paragraph...    │
│  [+ New Document]         │                              │
│                           │   - Item 1                   │
│                           │   - Item 2                   │
└───────────────────────────┴──────────────────────────────┘
  Sidebar                     Editor (CodeArea) | Preview (WebView)
```

- **Editor**: `CodeArea` from RichTextFX — Markdown syntax highlighting (optional: split with live preview via `WebView`)
- **Toolbar**: Buttons that insert Markdown snippets (e.g. `##` for H2, `| | |\n|-|-|` for table)
- **Sidebar**: Document list with `SyncStatus` badges

---

### DocumentViewModel — reactive bridge

```java
public class DocumentViewModel {
    private final StringProperty markdownContent   = new SimpleStringProperty();
    private final StringProperty title             = new SimpleStringProperty();
    private final BooleanProperty isDirty          = new SimpleBooleanProperty();
    private final ObservableList<Document> allDocs = FXCollections.observableArrayList();

    // Controller binds CodeArea.textProperty() → markdownContent
    // Auto-save triggers on isDirty + debounce timer
}
```

---

## Technology Stack

| Concern | Library / Tool | Notes |
|---------|---------------|-------|
| GUI | JavaFX 21 | |
| Markdown editor | RichTextFX `CodeArea` | Syntax highlighting |
| Markdown converter | **Pandoc CLI** | Renders HTML for `WebView` and exports |
| JSON metadata | Jackson 2.17 | Only for `.meta.json` |
| Boilerplate | Lombok | `@Data`, `@Builder` |
| Export | **Pandoc CLI** | `--reference-doc` for templates |
| Logging | SLF4J + Logback | |
| Tests | JUnit 5 + Mockito + AssertJ | |
| Coverage | JaCoCo ≥ 90% | Excludes UI |
| Build | Maven | |

---

## Build Order

```
1. document/          ← Document + DocumentMetadata POJOs
2. storage/           ← read/write .md and .meta.json files
3. export/            ← PandocRunner + ExportService
4. app/               ← DocumentService wires 2+3
5. ui/                ← JavaFX controllers + viewmodel
```

---

## What Is NOT in Scope (v1)

| Feature | When |
|---------|------|
| AWS S3 cloud sync | v2 |
| Element-level formatting | v2 |
| Undo / Redo | v2 (TextArea has it built-in already) |
| Search / Filter | v2 |
| Document metrics | v2 |
| Collaboration | v3 |

---

## Why This is Better

| Before | After |
|--------|-------|
| `Paragraph`, `Heading`, `Table` Java classes | Just a `String markdownContent` |
| Custom `Template` class + style maps | A `.docx` file + Pandoc `--reference-doc` |
| Visitor pattern for export | `PandocRunner` wrapping a CLI call |
| Multi-layer repository/service abstractions | Flattened `App -> Storage` flow |
| ~15 domain classes | ~2 classes (`Document` + `DocumentMeta`) |
