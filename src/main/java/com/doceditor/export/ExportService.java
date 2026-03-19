package com.doceditor.export;

import com.doceditor.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class ExportService {
    private static final Logger log = LoggerFactory.getLogger(ExportService.class);
    private final PandocRunner pandoc;
    private final Path tempDir;

    public ExportService(PandocRunner pandoc) {
        this.pandoc = pandoc;
        this.tempDir = Path.of(System.getProperty("java.io.tmpdir"), "doceditor");
        ensureTempDir();
    }

    public Path exportMarkdown(Document doc, Path outputDir) {
        Path file = outputDir.resolve(safeFilename(doc.getTitle()) + ".md");
        try {
            String content = MarkdownFormatter.formatMath(doc.getMarkdownContent());
            Files.writeString(file, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return file;
        } catch (IOException e) {
            throw new ExportException("Failed to write Markdown file: " + file, e);
        }
    }

    public Path exportDocx(Document doc, Path outputDir, Path templateDocx) {
        Path mdFile = writeTempMd(doc);
        Path outFile = outputDir.resolve(safeFilename(doc.getTitle()) + ".docx");
        pandoc.toDocx(mdFile, outFile, templateDocx);
        return outFile;
    }

    public Path exportPdf(Document doc, Path outputDir) {
        Path mdFile = writeTempMd(doc);
        Path tempDocx = tempDir.resolve(doc.getId() + ".temp.docx");
        pandoc.toDocx(mdFile, tempDocx, null);
        Path outFile = outputDir.resolve(safeFilename(doc.getTitle()) + ".pdf");
        pandoc.fromDocxToPdf(tempDocx, outFile);
        try { Files.deleteIfExists(tempDocx); } catch (IOException ignored) {}
        return outFile;
    }

    private Path writeTempMd(Document doc) {
        Path tmp = tempDir.resolve(doc.getId() + ".md");
        try {
            String content = MarkdownFormatter.formatMath(doc.getMarkdownContent());
            Files.writeString(tmp, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return tmp;
        } catch (IOException e) {
            throw new ExportException("Failed to write temp Markdown file", e);
        }
    }

    private String safeFilename(String title) {
        if (title == null || title.isBlank()) return "document";
        return title.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_").replaceAll("_+", "_");
    }

    private void ensureTempDir() {
        try {
            Files.createDirectories(tempDir);
        } catch (IOException e) {
            throw new ExportException("Cannot create temp directory: " + tempDir, e);
        }
    }
}
