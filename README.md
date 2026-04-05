# Document Editor

A sleek, minimal Markdown editor built with JavaFX and powered by Pandoc. Write in clean Markdown, preview in real-time, and export to professional formats with zero friction.

![App Mockup](docs/images/mockup.png)
*Place for your app screenshot*

## Features

- **Markdown-First Experience:** A focused editor using RichTextFX for responsive syntax highlighting.
- **Live Preview:** Instant HTML rendering via JavaFX WebView to see your document take shape.
- **Pandoc Integration:** Robust export engine that converts your Markdown to `.docx` and `.pdf`.
- **Smart Metadata:** Each document is stored as a standard `.md` file with a companion `.json` file for metadata (title, timestamps).
- **Local-First Storage:** Documents are saved to `~/.doceditor/documents/` by default.

## Prerequisites

To use the export features, you must have **Pandoc** installed on your system.

- **Linux:** `sudo apt install pandoc`
- **macOS:** `brew install pandoc`
- **Windows:** `winget install pandoc`

## Getting Started

### Build and Run

This project uses Maven and the JavaFX Maven Plugin.

1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/document-editor.git
   cd document-editor
   ```

2. **Build and Run:**
   ```bash
   mvn clean javafx:run
   ```

3. **Package as a Native App:**
   ```bash
   mvn clean javafx:jpackage
   ```

## Architecture

The project follows a clean, decoupled architecture:

- **`com.doceditor.document`**: Core domain model (`Document`).
- **`com.doceditor.storage`**: Handles persistence for Markdown and `DocumentMeta` records.
- **`com.doceditor.export`**: Orchestrates the Pandoc CLI pipeline with dedicated math formatting.
- **`com.doceditor.ui`**: Reactive JavaFX frontend using the ViewModel pattern.

For more details, see [ARCHITECTURE.md](ARCHITECTURE.md).

## Technology Stack

| Component | Technology |
|-----------|------------|
| GUI Framework | JavaFX 21 |
| Editor Component | RichTextFX (`CodeArea`) |
| Conversion Engine| **Pandoc CLI** |
| JSON Handling | Jackson |
| Build Tool | Maven |
| Language | Java 21 |

## License

MIT License - see [LICENSE](LICENSE) for details.
