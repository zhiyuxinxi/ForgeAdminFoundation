package com.mdframe.forge.starter.core.config;

import com.mdframe.forge.starter.core.i18n.UiMessages;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.TimeZone;

/**
 * jackson 配置
 *
 * @author Lion Li
 */
@Slf4j
@AutoConfiguration(before = JacksonAutoConfiguration.class)
public class JacksonConfig {

    /**
     * 支持多种日期格式的 LocalDateTime 反序列化器
     */
    public static class MultiFormatLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

        private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            if (value == null || value.isEmpty()) {
                return null;
            }

            // 去除前后空格
            value = value.trim();

            // 尝试多种格式解析
            try {
                // 先尝试默认格式
                return LocalDateTime.parse(value, DEFAULT_FORMATTER);
            } catch (DateTimeParseException e1) {
                try {
                    // 处理带 Z 后缀的 UTC 时间
                    if (value.endsWith("Z")) {
                        String withoutZ = value.substring(0, value.length() - 1);
                        // 使用 ISO_LOCAL_DATE_TIME 解析
                        return LocalDateTime.parse(withoutZ, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    }
                    // 尝试 ISO 格式
                    return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
                } catch (DateTimeParseException e2) {
                    try {
                        // 最后尝试直接解析
                        return LocalDateTime.parse(value);
                    } catch (DateTimeParseException e3) {
                        log.warn("无法解析日期时间: {}", value);
                        throw new IOException(UiMessages.translate("无法解析日期时间: ") + value, e1);
                    }
                }
            }
        }
    }

    /**
     * 支持多种日期格式的 LocalDate 反序列化器。
     * 兼容前端 n-date-picker 回传的毫秒时间戳（number）与 "yyyy-MM-dd" / ISO 字符串。
     */
    public static class MultiFormatLocalDateDeserializer extends JsonDeserializer<LocalDate> {

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            // 数字：前端日期选择器回传毫秒时间戳，按系统默认时区换算为日期
            if (p.getCurrentToken() == JsonToken.VALUE_NUMBER_INT) {
                long millis = p.getLongValue();
                return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
            }
            String value = p.getValueAsString();
            if (value == null || value.isEmpty()) {
                return null;
            }
            value = value.trim();
            try {
                return LocalDate.parse(value, FORMATTER);
            } catch (DateTimeParseException e1) {
                // 兼容 "yyyy-MM-ddTHH:mm:ss" / "yyyy-MM-dd HH:mm:ss" 等，截取前 10 位日期
                if (value.length() >= 10) {
                    try {
                        return LocalDate.parse(value.substring(0, 10));
                    } catch (DateTimeParseException ignored) {
                        // 落入统一报错
                    }
                }
                log.warn("无法解析日期: {}", value);
                throw new IOException("无法解析日期: " + value, e1);
            }
        }
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> {
            // 全局配置序列化返回 JSON 处理
            JavaTimeModule javaTimeModule = new JavaTimeModule();
            javaTimeModule.addSerializer(Long.class, BigNumberSerializer.INSTANCE);
            javaTimeModule.addSerializer(Long.TYPE, BigNumberSerializer.INSTANCE);
            javaTimeModule.addSerializer(BigInteger.class, BigNumberSerializer.INSTANCE);
            javaTimeModule.addSerializer(BigDecimal.class, ToStringSerializer.instance);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
            // 使用支持多种格式的反序列化器
            javaTimeModule.addDeserializer(LocalDateTime.class, new MultiFormatLocalDateTimeDeserializer());
            javaTimeModule.addDeserializer(LocalDate.class, new MultiFormatLocalDateDeserializer());
            builder.modules(javaTimeModule);
            builder.timeZone(TimeZone.getDefault());
            log.info("初始化 jackson 配置");
        };
    }

}
