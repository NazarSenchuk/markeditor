package com.markeditor.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class SearchHelper {
    private SearchHelper() {
    }

    public static Optional<SearchMatch> findNext(String text, String query, int caretPosition, boolean matchCase) {
        String safeText = text == null ? "" : text;
        String safeQuery = query == null ? "" : query;
        if (safeQuery.isBlank()) {
            return Optional.empty();
        }

        String source = normalize(safeText, matchCase);
        String target = normalize(safeQuery, matchCase);
        int fromIndex = clamp(caretPosition, 0, safeText.length());

        int foundIndex = source.indexOf(target, fromIndex);
        if (foundIndex < 0 && fromIndex > 0) {
            foundIndex = source.indexOf(target);
        }

        return foundIndex >= 0
                ? Optional.of(new SearchMatch(foundIndex, foundIndex + safeQuery.length()))
                : Optional.empty();
    }

    public static Optional<SearchMatch> findPrevious(String text, String query, int caretPosition, boolean matchCase) {
        String safeText = text == null ? "" : text;
        String safeQuery = query == null ? "" : query;
        if (safeQuery.isBlank()) {
            return Optional.empty();
        }

        String source = normalize(safeText, matchCase);
        String target = normalize(safeQuery, matchCase);
        int startIndex = clamp(caretPosition, 0, safeText.length());

        int foundIndex = startIndex > 0 ? source.lastIndexOf(target, startIndex - 1) : -1;
        if (foundIndex < 0) {
            foundIndex = source.lastIndexOf(target);
        }

        return foundIndex >= 0
                ? Optional.of(new SearchMatch(foundIndex, foundIndex + safeQuery.length()))
                : Optional.empty();
    }

    public static List<SearchMatch> findAll(String text, String query, boolean matchCase) {
        String safeText = text == null ? "" : text;
        String safeQuery = query == null ? "" : query;
        if (safeQuery.isBlank()) {
            return Collections.emptyList();
        }

        String source = normalize(safeText, matchCase);
        String target = normalize(safeQuery, matchCase);
        List<SearchMatch> matches = new ArrayList<>();

        int index = 0;
        while ((index = source.indexOf(target, index)) >= 0) {
            matches.add(new SearchMatch(index, index + safeQuery.length()));
            index += safeQuery.length();
        }

        return matches;
    }

    private static String normalize(String value, boolean matchCase) {
        return matchCase ? value : value.toLowerCase();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record SearchMatch(int start, int end) {
    }
}
