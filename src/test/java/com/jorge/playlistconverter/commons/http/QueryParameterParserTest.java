package com.jorge.playlistconverter.commons.http;
import static com.jorge.playlistconverter.commons.http.QueryParameterParser.extractParam;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class QueryParameterParserTest {

    @Test
    void shouldExtractRequestedParameter(){
        String query = "state=test-state&code=test-code&error=access_denied";

        assertEquals("test-state", extractParam(query, "state"));
        assertEquals("test-code", extractParam(query, "code"));
        assertEquals("access_denied", extractParam(query, "error"));
    }

    @Test
    void shouldReturnNullWhenQueryIsAbsentOrBlank(){
        assertNull(extractParam(null, "code"));
        assertNull(extractParam("", "code"));
        assertNull(extractParam("  ", "code"));
    }

    @Test
    void shouldMatchExactParameterName(){
        String query = "code_extra=123&state=test-state";

        assertNull(extractParam(query, "code"));
        assertNull(extractParam(query, "scope"));
    }

    @Test
    void shouldReturnEmptyStringForParameterWithoutValue(){
        assertEquals("", extractParam("code=", "code"));
        assertEquals("", extractParam("code", "code"));
    }

    @Test
    void shouldDecodeNamesAndValuesUsingUtf8(){
        String query = "co%64e=m%C3%BAsica%20de%20teste%2B";

        assertEquals("música de teste+", extractParam(query, "code"));
    }

    @Test
    void shouldDecodePlusAsSpace(){
        assertEquals("test code", extractParam("code=test+code", "code"));
    }

    @Test
    void shouldPreserveEqualsInsideValue(){
        assertEquals("abc=def", extractParam("code=abc=def", "code"));
    }

    @Test
    void shouldReturnFirstValueForRepeatedParameter(){
        assertEquals("first", extractParam("code=first&code=second", "code"));
    }

    @Test
    void shouldRejectMalformedEncoding(){
        assertThrows(IllegalArgumentException.class, () ->
                extractParam("code=%ZZ", "code"));

        assertThrows(IllegalArgumentException.class, () ->
                extractParam("code=%ZZde=value", "code"));
    }
}
