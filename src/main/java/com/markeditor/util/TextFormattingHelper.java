package com.markeditor.util;

public final class TextFormattingHelper {
    private TextFormattingHelper() {
    }

    public static TextEdit wrapSelection(String text, int selectionStart, int selectionEnd, String prefix, String suffix) {
        String safeText = text == null ? "" : text;
        int start = clamp(selectionStart, 0, safeText.length());
        int end = clamp(selectionEnd, 0, safeText.length());
        if (start > end) {
            int temp = start;
            start = end;
            end = temp;
        }

        String selected = safeText.substring(start, end);
        String replacement = prefix + selected + suffix;
        String updatedText = safeText.substring(0, start) + replacement + safeText.substring(end);
        int caretStart = start + prefix.length();
        int caretEnd = selected.isEmpty() ? caretStart : caretStart + selected.length();

        return new TextEdit(updatedText, caretStart, caretEnd);
    }

    public static TextEdit insertAtLineStart(String text, int selectionStart, int selectionEnd, String marker) {
        String safeText = text == null ? "" : text;
        int start = clamp(selectionStart, 0, safeText.length());
        int end = clamp(selectionEnd, 0, safeText.length());
        if (start > end) {
            int temp = start;
            start = end;
            end = temp;
        }

        int blockStart = findLineStart(safeText, start);
        int adjustedEnd = end;
        if (end > start && end > 0 && safeText.charAt(end - 1) == '\n') {
            adjustedEnd--;
        }
        int blockEnd = findLineEndExclusive(safeText, adjustedEnd);

        String block = safeText.substring(blockStart, blockEnd);
        String[] lines = block.split("\\R", -1);
        String lineSeparator = detectLineSeparator(block);
        StringBuilder replaced = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                replaced.append(lineSeparator);
            }
            replaced.append(marker).append(lines[i]);
        }

        String updatedText = safeText.substring(0, blockStart) + replaced + safeText.substring(blockEnd);
        return new TextEdit(updatedText, blockStart, blockStart + replaced.length());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int findLineStart(String text, int index) {
        return text.lastIndexOf('\n', Math.max(0, index - 1)) + 1;
    }

    private static int findLineEndExclusive(String text, int index) {
        int newlineIndex = text.indexOf('\n', Math.min(index, text.length()));
        return newlineIndex >= 0 ? newlineIndex : text.length();
    }

    private static String detectLineSeparator(String block) {
        return block.contains("\r\n") ? "\r\n" : "\n";
    }

    public record TextEdit(String text, int selectionStart, int selectionEnd) {
    }
}
