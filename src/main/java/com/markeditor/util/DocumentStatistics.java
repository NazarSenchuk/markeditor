package com.markeditor.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DocumentStatistics {
    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}'-]*");
    private static final Pattern HEADING_PATTERN = Pattern.compile("^#{1,6}\\s+.*$");
    private static final int CHARS_PER_PAGE = 1800;
    private static final int WORDS_PER_PAGE = 300;

    private DocumentStatistics() {
    }

    public static Stats compute(String text) {
        String safeText = text == null ? "" : text;

        int chars = safeText.length();
        int charsNoSpaces = safeText.replaceAll("\\s+", "").length();
        int words = countWords(safeText);
        int paragraphs = countParagraphs(safeText);
        int headings = countHeadings(safeText);
        int pages = estimatePages(chars, words);

        return new Stats(chars, charsNoSpaces, words, paragraphs, headings, pages);
    }

    private static int countWords(String text) {
        Matcher matcher = WORD_PATTERN.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static int countParagraphs(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }
        return trimmed.split("\\R\\s*\\R").length;
    }

    private static int countHeadings(String text) {
        int count = 0;
        for (String line : text.split("\\R", -1)) {
            if (HEADING_PATTERN.matcher(line.trim()).matches()) {
                count++;
            }
        }
        return count;
    }

    private static int estimatePages(int chars, int words) {
        if (chars == 0 && words == 0) {
            return 0;
        }
        int pagesByChars = (int) Math.ceil(chars / (double) CHARS_PER_PAGE);
        int pagesByWords = (int) Math.ceil(words / (double) WORDS_PER_PAGE);
        return Math.max(1, Math.max(pagesByChars, pagesByWords));
    }

    public record Stats(int chars, int charsNoSpaces, int words, int paragraphs, int headings, int pages) {
    }
}
