package com.doceditor.export;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PandocRunner {
    private static final int TIMEOUT_SECONDS = 60;

    public void toDocx(Path mdFile, Path outputFile, Path templateDocx, boolean numberSections, boolean toc) {
        List<String> cmd = new ArrayList<>(List.of(
                "pandoc", mdFile.toString(), "-o", outputFile.toString(),
                "--standalone", "--mathjax", "--highlight-style=tango"
        ));
        if (templateDocx != null) cmd.add("--reference-doc=" + templateDocx);
        if (numberSections) cmd.add("--number-sections");
        if (toc) {
            cmd.add("--toc");
            cmd.add("--toc-title=ЗМІСТ");
        }
        run(cmd);
    }

    public void fromDocxToPdf(Path docxFile, Path outputFile) {
        run(List.of("pandoc", docxFile.toString(), "--pdf-engine=xelatex",
                "--highlight-style=tango",
                "-V", "mainfont=DejaVu Serif", "-V", "mathfont=DejaVu Serif",
                "-V", "lang=uk", "-o", outputFile.toString()));
    }

    public void toPdf(Path mdFile, Path outputFile, boolean numberSections, boolean toc) {
        List<String> cmd = new ArrayList<>(List.of("pandoc", mdFile.toString(), "--pdf-engine=xelatex",
                "--highlight-style=tango",
                "-V", "mainfont=DejaVu Serif", "-V", "mathfont=DejaVu Serif",
                "-V", "lang=uk", "-o", outputFile.toString()));
        if (numberSections) cmd.add("--number-sections");
        if (toc) {
            cmd.add("--toc");
            cmd.add("--metadata=toc-title:ЗМІСТ");
        }
        run(cmd);
    }

    public String toHtml(String markdown, boolean standalone, boolean numberSections, boolean toc) {
        List<String> cmd = new ArrayList<>(List.of("pandoc",
                "-f", "markdown+tex_math_single_backslash+tex_math_double_backslash",
                "-t", "html", "--mathjax=https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-chtml-full.js",
                "--highlight-style=tango"));
        if (standalone) cmd.add("--standalone");
        if (numberSections) cmd.add("--number-sections");
        if (toc) {
            cmd.add("--toc");
            cmd.add("--metadata=toc-title:ЗМІСТ");
        }
        return runWithCapture(cmd, markdown);
    }

    private void run(List<String> cmd) {
        runWithCapture(cmd, null);
    }

    private String runWithCapture(List<String> cmd, String input) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            if (input == null) pb.redirectErrorStream(true);
            Process proc = pb.start();
            if (input != null) {
                proc.getOutputStream().write(input.getBytes());
                proc.getOutputStream().close();
            }
            String output = new String(proc.getInputStream().readAllBytes());
            String error = input != null ? new String(proc.getErrorStream().readAllBytes()) : "";
            if (!proc.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                proc.destroyForcibly();
                throw new ExportException("Pandoc timed out");
            }
            if (proc.exitValue() != 0) throw new ExportException("Pandoc failed: " + output + " " + error);
            return output;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExportException("Pandoc failed: " + e.getMessage(), e);
        }
    }
}
