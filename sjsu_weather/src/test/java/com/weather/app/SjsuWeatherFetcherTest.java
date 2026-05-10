package com.weather.app;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.time.Year;

import static org.junit.jupiter.api.Assertions.*;

class SjsuWeatherFetcherTest {

    @Test
    void resolveYearlyCsvHref_prefersCurrentYearLink() {
        int y = Year.now().getValue();
        Document doc = Jsoup.parse(
                "<html><body>"
                        + "<a href=\"other.csv\">other</a>"
                        + "<a href=\"Roof" + (y - 1) + ".csv\">old</a>"
                        + "<a href=\"Roof" + y + ".csv\">current</a>"
                        + "</body></html>");
        assertEquals("Roof" + y + ".csv", SjsuWeatherFetcher.resolveYearlyCsvHref(doc));
    }

    @Test
    void resolveYearlyCsvHref_fallsBackToAnyCsv() {
        Document doc = Jsoup.parse("<html><body><a href=\"/data/export.csv\">x</a></body></html>");
        assertEquals("/data/export.csv", SjsuWeatherFetcher.resolveYearlyCsvHref(doc));
    }

    @Test
    void resolveYearlyCsvHref_returnsNullWhenNoCsv() {
        Document doc = Jsoup.parse("<html><body><a href=\"page.html\">x</a></body></html>");
        assertNull(SjsuWeatherFetcher.resolveYearlyCsvHref(doc));
    }
}
