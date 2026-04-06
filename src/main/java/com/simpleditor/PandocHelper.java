package com.simpleditor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PandocHelper {
    private static final int TIMEOUT_SEC = 60;

    public static String prepareMath(String markdown) {
        if (markdown == null)
            return "";

        // Блокові формули \[ ... \] -> $$ ... $$
        String blockOpen = Pattern.quote("\\[");
        String blockClose = Pattern.quote("\\]");
        Pattern blockPattern = Pattern.compile(blockOpen + "(.*?)" + blockClose, Pattern.DOTALL);
        Matcher blockMatcher = blockPattern.matcher(markdown);
        StringBuffer sb = new StringBuffer();
        while (blockMatcher.find()) {
            String content = blockMatcher.group(1).trim();
            blockMatcher.appendReplacement(sb, Matcher.quoteReplacement("$$\n" + content + "\n$$"));
        }
        blockMatcher.appendTail(sb);
        markdown = sb.toString();

        // Інлайн формули \( ... \) -> $ ... $
        String inlineOpen = Pattern.quote("\\(");
        String inlineClose = Pattern.quote("\\)");
        Pattern inlinePattern = Pattern.compile(inlineOpen + "(.*?)" + inlineClose, Pattern.DOTALL);
        Matcher inlineMatcher = inlinePattern.matcher(markdown);
        sb = new StringBuffer();
        while (inlineMatcher.find()) {
            String content = inlineMatcher.group(1).trim();
            inlineMatcher.appendReplacement(sb, Matcher.quoteReplacement("$" + content + "$"));
        }
        inlineMatcher.appendTail(sb);
        return sb.toString();
    }

    public static void toDocx(File mdFile, File outFile, File templateDocx, boolean numberSections, boolean toc)
            throws Exception {
        String content = Files.readString(mdFile.toPath(), StandardCharsets.UTF_8);
        String prepared = prepareMath(content);
        File tempMd = File.createTempFile("temp", ".md");
        tempMd.deleteOnExit();
        Files.writeString(tempMd.toPath(), prepared, StandardCharsets.UTF_8);

        List<String> cmd = new ArrayList<>();
        cmd.add("pandoc");
        cmd.add(tempMd.getAbsolutePath());
        cmd.add("-o");
        cmd.add(outFile.getAbsolutePath());
        cmd.add("--standalone");
        cmd.add("--mathjax");
        if (templateDocx != null && templateDocx.exists()) {
            cmd.add("--reference-doc=" + templateDocx.getAbsolutePath());
        }
        if (numberSections)
            cmd.add("--number-sections");
        if (toc) {
            cmd.add("--toc");
            cmd.add("--toc-title=ЗМІСТ");
        }
        runProcess(cmd, null);
    }

    public static void toPdf(File mdFile, File outFile, boolean numberSections, boolean toc) throws Exception {
        File tempDocx = File.createTempFile("temp", ".docx");
        tempDocx.deleteOnExit();
        toDocx(mdFile, tempDocx, null, numberSections, toc);
        List<String> cmd = new ArrayList<>();
        cmd.add("pandoc");
        cmd.add(tempDocx.getAbsolutePath());
        cmd.add("--pdf-engine=xelatex");
        cmd.add("-V");
        cmd.add("mainfont=DejaVu Serif");
        cmd.add("-V");
        cmd.add("mathfont=DejaVu Serif");
        cmd.add("-V");
        cmd.add("lang=uk");
        cmd.add("-o");
        cmd.add(outFile.getAbsolutePath());
        runProcess(cmd, null);
        Files.deleteIfExists(tempDocx.toPath());
    }

    public static String toHtml(String markdown, boolean numberSections, boolean toc) throws Exception {
        String prepared = prepareMath(markdown);
        List<String> cmd = new ArrayList<>();
        cmd.add("pandoc");
        cmd.add("-f");
        cmd.add("markdown+tex_math_single_backslash+tex_math_double_backslash");
        cmd.add("-t");
        cmd.add("html");
        cmd.add("--mathjax=https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-chtml-full.js");
        cmd.add("--highlight-style=tango");
        cmd.add("--standalone");
        if (numberSections)
            cmd.add("--number-sections");
        if (toc) {
            cmd.add("--toc");
            cmd.add("--metadata=toc-title=ЗМІСТ");
        }
        return runProcess(cmd, prepared);
    }

    private static String runProcess(List<String> command, String input) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        if (input != null) {
            try (OutputStream os = p.getOutputStream()) {
                os.write(input.getBytes(StandardCharsets.UTF_8));
            }
        }
        boolean finished = p.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new RuntimeException("Pandoc timeout");
        }
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (p.exitValue() != 0) {
            throw new RuntimeException("Pandoc error: " + output);
        }
        return output;
    }
}