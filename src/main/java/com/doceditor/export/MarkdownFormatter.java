package com.doceditor.export;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownFormatter {
    public static String formatMath(String content) {
        if (content == null) return "";
        // Remove horizontal rules (---) which can be misinterpreted as triggers or frontmatter
        content = content.replaceAll("(?m)^[\\s]*---[\\s]*$", "");
        
        content = content.replaceAll(Pattern.quote("\\[") + "\\s*", Matcher.quoteReplacement("$$\n"));
        content = content.replaceAll("\\s*" + Pattern.quote("\\]"), Matcher.quoteReplacement("\n$$"));
        Pattern inlineMath = Pattern.compile(Pattern.quote("\\(") + "\\s*(.*?)\\s*" + Pattern.quote("\\)"), Pattern.DOTALL);
        Matcher matcher = inlineMath.matcher(content);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement("$" + matcher.group(1).trim() + "$"));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
