package com.example.onboarding.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Addresses in task descriptions become links; everything else stays plain (escaped) text.
 */
class LinkTextTest {

    private final LinkText links = new LinkText();

    private static String link(String url) {
        return "<a class=\"auto-link\" href=\"" + url + "\" target=\"_blank\" rel=\"noopener noreferrer\">" + url + "</a>";
    }

    @Test
    void addressBecomesLink() {
        assertThat(links.linkify("SSO にログイン: https://sso.example.com/login?app=hr"))
                .isEqualTo("SSO にログイン: " + link("https://sso.example.com/login?app=hr"));
    }

    @Test
    void severalAddressesAndHttp() {
        assertThat(links.linkify("A http://a.example B https://b.example/x"))
                .isEqualTo("A " + link("http://a.example") + " B " + link("https://b.example/x"));
    }

    @Test
    void japaneseRightAfterAddressIsNotPartOfIt() {
        assertThat(links.linkify("https://sso.example.comからログイン"))
                .isEqualTo(link("https://sso.example.com") + "からログイン");
    }

    @Test
    void sentencePunctuationIsNotPartOfAddress() {
        assertThat(links.linkify("See https://a.example/page."))
                .isEqualTo("See " + link("https://a.example/page") + ".");
        assertThat(links.linkify("(https://a.example/page)"))
                .isEqualTo("(" + link("https://a.example/page") + ")");
        assertThat(links.linkify("https://en.wikipedia.org/wiki/Java_(programming_language)"))
                .isEqualTo(link("https://en.wikipedia.org/wiki/Java_(programming_language)"));
    }

    @Test
    void htmlInTextIsEscaped() {
        assertThat(links.linkify("<script>alert('x')</script> & <b>太字</b>"))
                .isEqualTo("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; &lt;b&gt;太字&lt;/b&gt;");
    }

    @Test
    void addressCannotBreakOutOfTheLink() {
        String html = links.linkify("https://a.example/?q=\"><script>alert(1)</script>");
        assertThat(html).doesNotContain("<script>").startsWith(link("https://a.example/?q="));
    }

    @Test
    void ampersandInAddressIsEscaped() {
        assertThat(links.linkify("https://a.example/?a=1&b=2"))
                .isEqualTo(link("https://a.example/?a=1&amp;b=2"));
    }

    @Test
    void otherSchemesAreNotLinked() {
        assertThat(links.linkify("javascript:alert(1) ftp://a.example mailto:a@example.com"))
                .isEqualTo("javascript:alert(1) ftp://a.example mailto:a@example.com")
                .doesNotContain("<a");
    }

    @Test
    void emptyAndNullAreEmpty() {
        assertThat(links.linkify(null)).isEmpty();
        assertThat(links.linkify("")).isEmpty();
        assertThat(links.linkify("リンクなし")).isEqualTo("リンクなし");
    }
}
