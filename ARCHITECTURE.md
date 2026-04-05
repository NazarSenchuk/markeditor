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
(local storage)             │
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

→ Saved as `<uuid>.md` and `<uuid>.meta.json` locally  
→ Preview rendered in `WebView` using **Pandoc** (md → HTML)  
→ Export: `pandoc input.md --reference-doc=business.docx -o output.docx`

---

## Package Structure

```
com.doceditor/
│
├── document/                        ← Core domain
│   └── Document.java                ← id, title, markdownContent, timestamps
│
├── storage/                         ← Persistence layer
│   ├── LocalMarkdownStorage.java    ← Stores .md and .meta.json files
│   ├── DocumentMeta.java            ← Lightweight metadata record (JSON)
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
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    // Helper methods: touch(), getSummary()
}
```

That's it. No element list, no style map, no template field.

---

### Storage — Markdown files on disk

```
~/.doceditor/
└── documents/
    ├── <uuid>.md           ← document content
    └── <uuid>.meta.json    ← id, title, timestamps
```

- Content and metadata stored separately — metadata is tiny JSON, content is raw Markdown
- `DocumentRepository` interface allows swapping to a DB later

---

### Export — Pandoc CLI

```java
public class PandocRunner {
    // Export to HTML for live preview
    public String toHtml(String markdown) {
        // pandoc -f markdown -t html --mathjax
    }

    // Export to DOCX with reference template
    public void toDocx(Path mdFile, Path outputFile, Path templateDocx) {
        // pandoc md -o docx --reference-doc=template.docx
    }

    // Export to PDF via xelatex engine
    public void toPdf(Path mdFile, Path outputFile) {
        // pandoc md -o pdf --pdf-engine=xelatex
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
// Logic moved to ExportService.java classes below
```

1. **Format**: Uses `MarkdownFormatter` to fix LaTeX math delimiters for Pandoc ($ .. $).
2. **Temporary storage**: Saves Markdown to `/tmp/doceditor/` before conversion.
3. **Conversion**: Calls `PandocRunner` with specific flags (standing alone, TOC, section numbering).
4. **Cleanup**: Removes temporary files.

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

- **Editor**: `CodeArea` from RichTextFX — Markdown syntax highlighting.
- **Toolbar**: Buttons that insert Markdown snippets (e.g. `##` for H2, `| | |\n|-|-|` for table).
- **Sidebar**: Document list with local storage status.

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
1. document/          ← Document model
2. storage/           ← LocalMarkdownStorage + DocumentMeta record
3. export/            ← PandocRunner + ExportService + MarkdownFormatter
4. app/               ← DocumentService logic
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
