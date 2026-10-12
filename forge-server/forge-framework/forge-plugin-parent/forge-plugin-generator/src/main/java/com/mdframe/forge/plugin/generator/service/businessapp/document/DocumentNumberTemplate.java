package com.mdframe.forge.plugin.generator.service.businessapp.document;

import com.mdframe.forge.plugin.generator.dto.businessapp.BusinessDocumentNoRulePreviewDTO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentNoRulePreviewVO;
import com.mdframe.forge.plugin.generator.vo.businessapp.BusinessDocumentNoRuleTokenVO;
import com.mdframe.forge.starter.core.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Document number grammar, preview and validation; never allocates a sequence. */
public final class DocumentNumberTemplate {

    private static final Pattern NO_RULE_TOKEN_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern LEGACY_NO_RULE_TOKEN_PATTERN = Pattern.compile("(?<!\\$)\\{([^{}]+)}");
    private static final Pattern LEGACY_SEQ_TOKEN_PATTERN = Pattern.compile("seq(\\d+)");

    public List<BusinessDocumentNoRuleTokenVO> listNoRuleTokens() {
        List<BusinessDocumentNoRuleTokenVO> tokens = new ArrayList<>();
        tokens.add(token("${yyyy}", "年份", "日期时间", "当前年份，四位数字", "2026", "2026"));
        tokens.add(token("${yyyyMM}", "年月", "日期时间", "当前年月，六位数字", "202606", "202606"));
        tokens.add(token("${yyyyMMdd}", "年月日", "日期时间", "当前日期，八位数字", "20260602", "20260602"));
        tokens.add(token("${HHmmss}", "时分秒", "日期时间", "当前时间，六位数字", "203405", "203405"));
        tokens.add(token("${seq}", "流水号", "序列", "预览使用样例序号，不占用真实序列", "1", "1"));
        tokens.add(token("${seq:4}", "四位流水号", "序列", "流水号左侧补零到指定长度", "0001", "0001"));
        tokens.add(token("${suiteCode}", "套件编码", "上下文", "当前业务套件编码", "CRM", "CRM"));
        tokens.add(token("${objectCode}", "对象编码", "上下文", "当前业务对象编码", "OPPORTUNITY", "OPPORTUNITY"));
        tokens.add(token("${starter}", "发起人", "上下文", "当前发起人用户名或用户编码", "zhangsan", "zhangsan"));
        tokens.add(token("${deptCode}", "部门编码", "上下文", "当前发起人部门编码", "SALES", "SALES"));
        tokens.add(token("${field:<fieldCode>}", "单据字段", "业务字段", "从样例数据读取业务字段值，例如 ${field:customerName}", "ACME", "ACME"));
        return tokens;
    }

    public BusinessDocumentNoRulePreviewVO previewNoRule(BusinessDocumentNoRulePreviewDTO dto) {
        BusinessDocumentNoRulePreviewDTO source = dto == null ? new BusinessDocumentNoRulePreviewDTO() : dto;
        String template = StringUtils.defaultIfBlank(normalizeNoRuleTemplate(source.getTemplate()), "DOC-${yyyyMMdd}-${seq:4}");
        return renderNoRulePreview(
                template,
                StringUtils.defaultIfBlank(source.getSuiteCode(), "SUITE"),
                StringUtils.defaultIfBlank(source.getObjectCode(), "OBJECT"),
                StringUtils.defaultIfBlank(source.getStarter(), "starter"),
                StringUtils.defaultIfBlank(source.getDeptCode(), "DEPT"),
                source.getSampleData(),
                source.getSequence() == null ? 1L : source.getSequence().longValue()
        );
    }

    private BusinessDocumentNoRuleTokenVO token(String insertText, String label, String groupName,
                                                String description, String example, String sampleValue) {
        BusinessDocumentNoRuleTokenVO vo = new BusinessDocumentNoRuleTokenVO();
        vo.setToken(insertText);
        vo.setInsertText(insertText);
        vo.setLabel(label);
        vo.setGroupName(groupName);
        vo.setDescription(description);
        vo.setExample(example);
        vo.setSampleValue(sampleValue);
        return vo;
    }

    public BusinessDocumentNoRulePreviewVO renderNoRulePreview(String template,
                                                                String suiteCode,
                                                                String objectCode,
                                                                String starter,
                                                                String deptCode,
                                                                Map<String, Object> sampleData,
                                                                Long sequence) {
        BusinessDocumentNoRulePreviewVO vo = new BusinessDocumentNoRulePreviewVO();
        vo.setTemplate(template);
        StringBuilder result = new StringBuilder();
        Matcher matcher = NO_RULE_TOKEN_PATTERN.matcher(template);
        int lastIndex = 0;
        LocalDateTime now = LocalDateTime.now();
        while (matcher.find()) {
            result.append(template, lastIndex, matcher.start());
            String token = matcher.group(1);
            vo.getUsedTokens().add("${" + token + "}");
            String replacement = resolveNoRuleToken(token, suiteCode, objectCode, starter, deptCode, sampleData, sequence, now, vo);
            result.append(replacement);
            lastIndex = matcher.end();
        }
        result.append(template.substring(lastIndex));
        String previewNo = result.toString();
        vo.setPreviewNo(previewNo);
        if (!previewNo.matches("[A-Za-z0-9_\\-./]+")) {
            vo.getWarnings().add(issue(null, "编号包含空格或特殊字符，可能不适合作为对外单据号", "建议只使用字母、数字、短横线、下划线、点和斜线"));
        }
        if (previewNo.length() > 64) {
            vo.getWarnings().add(issue(null, "编号长度超过 64 个字符", "建议缩短固定前缀或字段变量内容"));
        }
        vo.setValid(vo.getErrors().isEmpty());
        return vo;
    }

    private String resolveNoRuleToken(String token,
                                      String suiteCode,
                                      String objectCode,
                                      String starter,
                                      String deptCode,
                                      Map<String, Object> sampleData,
                                      Long sequence,
                                      LocalDateTime now,
                                      BusinessDocumentNoRulePreviewVO vo) {
        if ("yyyy".equals(token)) {
            return now.format(DateTimeFormatter.ofPattern("yyyy"));
        }
        if ("yyyyMM".equals(token)) {
            return now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        }
        if ("yyyyMMdd".equals(token)) {
            return now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        if ("HHmmss".equals(token)) {
            return now.format(DateTimeFormatter.ofPattern("HHmmss"));
        }
        if ("seq".equals(token)) {
            return String.valueOf(Math.max(sequence == null ? 1L : sequence, 1L));
        }
        if (token.startsWith("seq:")) {
            return renderSequenceToken(token, sequence, vo);
        }
        if ("suiteCode".equals(token)) {
            return suiteCode;
        }
        if ("objectCode".equals(token)) {
            return objectCode;
        }
        if ("starter".equals(token)) {
            return starter;
        }
        if ("deptCode".equals(token)) {
            return deptCode;
        }
        if (token.startsWith("field:")) {
            String fieldCode = token.substring("field:".length()).trim();
            if (StringUtils.isBlank(fieldCode)) {
                vo.getErrors().add(issue("${" + token + "}", "字段变量缺少字段编码", "改为 ${field:字段编码}"));
                return "";
            }
            Object value = readSampleValue(sampleData, fieldCode);
            if (value == null) {
                vo.getWarnings().add(issue("${" + token + "}", "样例数据中没有字段 " + fieldCode, "预览时可填入 sampleData，保存后运行态会读取真实字段"));
                return fieldCode;
            }
            return String.valueOf(value);
        }
        vo.getErrors().add(issue("${" + token + "}", "未知编号变量: ${" + token + "}", "从内置变量列表选择，或使用 ${field:<fieldCode>} 引用业务字段"));
        return "";
    }

    private String renderSequenceToken(String token, Long sequence, BusinessDocumentNoRulePreviewVO vo) {
        String lengthText = token.substring("seq:".length()).trim();
        int length;
        try {
            length = Integer.parseInt(lengthText);
        } catch (Exception e) {
            vo.getErrors().add(issue("${" + token + "}", "流水号长度必须是数字", "例如 ${seq:4}"));
            return "";
        }
        if (length <= 0 || length > 12) {
            vo.getErrors().add(issue("${" + token + "}", "流水号长度建议在 1-12 之间", "例如 ${seq:4}"));
            return "";
        }
        String seqText = String.valueOf(Math.max(sequence == null ? 1L : sequence, 1L));
        return StringUtils.leftPad(seqText, length, '0');
    }

    private Object readSampleValue(Map<String, Object> sampleData, String fieldCode) {
        if (sampleData == null || StringUtils.isBlank(fieldCode)) {
            return null;
        }
        if (sampleData.containsKey(fieldCode)) {
            return sampleData.get(fieldCode);
        }
        String camel = DocumentFieldNames.snakeToCamel(fieldCode);
        if (sampleData.containsKey(camel)) {
            return sampleData.get(camel);
        }
        String snake = DocumentFieldNames.camelToSnake(fieldCode);
        return sampleData.get(snake);
    }

    private BusinessDocumentNoRulePreviewVO.PreviewIssueVO issue(String token, String message, String suggestion) {
        BusinessDocumentNoRulePreviewVO.PreviewIssueVO vo = new BusinessDocumentNoRulePreviewVO.PreviewIssueVO();
        vo.setToken(token);
        vo.setMessage(message);
        vo.setSuggestion(suggestion);
        return vo;
    }

    public void validateNoRuleTemplate(String template) {
        BusinessDocumentNoRulePreviewVO preview = renderNoRulePreview(normalizeNoRuleTemplate(template), "SUITE", "OBJECT", "starter", "DEPT",
                Map.of("fieldCode", "SAMPLE"), 1L);
        if (!preview.getErrors().isEmpty()) {
            String message = preview.getErrors().stream()
                    .map(BusinessDocumentNoRulePreviewVO.PreviewIssueVO::getMessage)
                    .findFirst()
                    .orElse("编号规则不正确");
            throw new BusinessException(message);
        }
    }

    public String normalizeNoRuleTemplate(String template) {
        String value = StringUtils.trimToNull(template);
        if (value == null) {
            return null;
        }
        Matcher matcher = LEGACY_NO_RULE_TOKEN_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("${" + normalizeNoRuleToken(matcher.group(1)) + "}"));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String normalizeNoRuleToken(String token) {
        String value = StringUtils.defaultString(token).trim();
        Matcher seqMatcher = LEGACY_SEQ_TOKEN_PATTERN.matcher(value);
        if (seqMatcher.matches()) {
            return "seq:" + seqMatcher.group(1);
        }
        return value;
    }
}
