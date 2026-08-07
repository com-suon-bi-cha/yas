package com.yas.commonlibrary.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeUtilsTest {

    @Test
    void format_withDefaultPattern_shouldReturnFormattedString() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 9, 5, 3);
        String result = DateTimeUtils.format(dateTime);
        assertEquals("15-06-2024_09-05-03", result);
    }

    @Test
    void format_withCustomPattern_shouldApplyPattern() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 7, 14, 30, 0);
        String result = DateTimeUtils.format(dateTime, "yyyy/MM/dd HH:mm");
        assertEquals("2024/01/07 14:30", result);
    }

    @Test
    void format_withNullDateTime_shouldThrowException() {
        assertThrows(NullPointerException.class, () -> DateTimeUtils.format(null));
    }

    @Test
    void format_withInvalidPattern_shouldThrowException() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        assertThrows(IllegalArgumentException.class,
                () -> DateTimeUtils.format(dateTime, "INVALID_PATTERN_QQQ"));
    }
}
