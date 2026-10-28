package com.mdframe.forge.starter.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JacksonLocalDateCompatibilityTest {
    private ObjectMapper mapper() {
        Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
        new JacksonConfig().customizer().customize(builder);
        return builder.build();
    }

    @Test
    void acceptsStandardDateAndDateTimeText() throws Exception {
        ObjectMapper mapper = mapper();
        LocalDate expected = LocalDate.of(2026, 10, 15);
        assertThat(mapper.readValue("\"2026-10-15\"", LocalDate.class)).isEqualTo(expected);
        assertThat(mapper.readValue("\"2026-10-15T13:45:00\"", LocalDate.class)).isEqualTo(expected);
        assertThat(mapper.readValue("\"2026-10-15 13:45:00\"", LocalDate.class)).isEqualTo(expected);
    }

    @Test
    void acceptsDatePickerMillisecondsInTheConfiguredSystemZone() throws Exception {
        LocalDate expected = LocalDate.of(2026, 10, 15);
        long millis = expected.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertThat(mapper().readValue(Long.toString(millis), LocalDate.class)).isEqualTo(expected);
    }

    @Test
    void keepsNullAndRejectsInvalidInput() throws Exception {
        ObjectMapper mapper = mapper();
        assertThat(mapper.readValue("null", LocalDate.class)).isNull();
        assertThat(mapper.readValue("\"\"", LocalDate.class)).isNull();
        assertThatThrownBy(() -> mapper.readValue("\"invalid-date\"", LocalDate.class))
                .isInstanceOf(java.io.IOException.class);
    }
}
