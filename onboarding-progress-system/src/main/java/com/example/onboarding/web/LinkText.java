package com.example.onboarding.web;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shows plain text with its http/https addresses as links, used as ${@links.linkify(...)} with th:utext.
 * Everything else is HTML-escaped, so tags written in the text do nothing (only the links are real HTML).
 */
@Component("links")
public class LinkText {

    /** Only characters that may appear in an address, so that Japanese text right after it is not included. */
    private static final Pattern URL = Pattern.compile("https?://[A-Za-z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+");

    /** Punctuation at the end usually belongs to the sentence, not to the address (e.g. "see https://a.example."). */
    private static final Pattern TRAILING_PUNCTUATION = Pattern.compile("[.,;:!?']+$");

    public String linkify(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder();
        Matcher matcher = URL.matcher(text);
        int last = 0;
        while (matcher.find()) {
            String url = trimEnd(matcher.group());
            int end = matcher.start() + url.length();
            html.append(HtmlUtils.htmlEscape(text.substring(last, matcher.start())));
            String escaped = HtmlUtils.htmlEscape(url);
            html.append("<a class=\"auto-link\" href=\"").append(escaped)
                    .append("\" target=\"_blank\" rel=\"noopener noreferrer\">").append(escaped).append("</a>");
            last = end;
        }
        html.append(HtmlUtils.htmlEscape(text.substring(last)));
        return html.toString();
    }

    private static String trimEnd(String url) {
        String trimmed = TRAILING_PUNCTUATION.matcher(url).replaceAll("");
        // A closing bracket without an opening one belongs to the sentence: "(https://a.example)"
        while (trimmed.endsWith(")") && count(trimmed, '(') < count(trimmed, ')')) {
            trimmed = TRAILING_PUNCTUATION.matcher(trimmed.substring(0, trimmed.length() - 1)).replaceAll("");
        }
        return trimmed;
    }

    private static long count(String text, char c) {
        return text.chars().filter(ch -> ch == c).count();
    }
}
