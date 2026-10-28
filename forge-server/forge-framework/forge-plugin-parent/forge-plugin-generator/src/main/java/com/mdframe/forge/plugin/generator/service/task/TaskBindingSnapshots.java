package com.mdframe.forge.plugin.generator.service.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfigVersion;
import com.mdframe.forge.plugin.generator.mapper.AiCrudConfigVersionMapper;
import com.mdframe.forge.plugin.generator.service.AiCrudConfigService;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static com.mdframe.forge.plugin.generator.service.task.TaskBindingSpec.require;

/** Only server-persisted published versions can supply runtime task mappings. */
@Component
@RequiredArgsConstructor
public class TaskBindingSnapshots {
    static final String OPTION = "publicTaskBindings";
    static final String LINK_FIELDS = "publicTaskLinkFields";
    private final ObjectMapper json;
    private final AiCrudConfigService configs;
    private final AiCrudConfigVersionMapper versions;

    public static long tenant() {
        LoginUser user = SessionHelper.getLoginUser();
        require(user != null && user.getUserId() != null && user.getUserId() > 0
                && user.getTenantId() != null && user.getTenantId() > 0
                && user.getTenantId().equals(TenantContextHolder.getTenantId()));
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes request) {
            require(!"1".equals(request.getRequest().getParameter("designPreview")));
        }
        return user.getTenantId();
    }

    public AiCrudConfig current(String configKey) {
        long tenant = tenant();
        AiCrudConfig config = configs.getByConfigKey(configKey);
        require(config != null && Long.valueOf(tenant).equals(config.getTenantId())
                && "PUBLISHED".equals(config.getPublishStatus()) && !"1".equals(config.getStatus())
                && "CONFIG".equals(config.getMode()) && config.getPublishedVersion() != null
                && config.getPublishedVersion() > 0);
        return config;
    }

    public AiCrudConfigVersion latest(String configKey) {
        AiCrudConfig config = current(configKey);
        AiCrudConfigVersion version = versions.selectVersionByNo(tenant(), config.getId(), config.getPublishedVersion());
        validateVersion(version);
        return version;
    }

    JsonNode previousOptions(AiCrudConfig config) {
        if (config.getPublishedVersion() == null || config.getPublishedVersion() <= 0) { return tree(null); }
        AiCrudConfigVersion previous = versions.selectVersionByNo(tenant(), config.getId(), config.getPublishedVersion());
        require(previous != null && Long.valueOf(tenant()).equals(previous.getTenantId()));
        return tree(previous.getOptions());
    }

    AiCrudConfigVersion version(long id) {
        AiCrudConfigVersion version = versions.selectById(id);
        validateVersion(version);
        AiCrudConfig current = current(version.getConfigKey());
        require(current.getId().equals(version.getConfigId()));
        return version;
    }

    private void validateVersion(AiCrudConfigVersion version) {
        require(version != null && Long.valueOf(tenant()).equals(version.getTenantId())
                && version.getRuntimeDatasourceId() == null
                && (version.getRuntimeDatasourceCode() == null || version.getRuntimeDatasourceCode().isBlank()));
    }

    public List<PublishedTaskBinding> bindings(AiCrudConfigVersion version) {
        JsonNode node = tree(version.getOptions()).path(OPTION);
        if (node.isMissingNode()) { return List.of(); }
        require(node.isArray() && node.size() <= 32);
        return json.convertValue(node, json.getTypeFactory().constructCollectionType(List.class, PublishedTaskBinding.class));
    }

    PublishedTaskBinding binding(AiCrudConfigVersion version, long id) {
        return bindings(version).stream().filter(binding -> binding.id() == id).findFirst()
                .orElseThrow(TaskBindingSpec::invalid);
    }

    public JsonNode tree(String value) {
        try {
            JsonNode node = json.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY).readTree(value == null ? "{}" : value);
            require(node != null && node.isObject());
            return node;
        } catch (JsonProcessingException exception) { throw TaskBindingSpec.invalid(); }
    }

    TaskBindingSpec spec(String value) {
        try { return json.treeToValue(tree(value), TaskBindingSpec.class); }
        catch (JsonProcessingException | IllegalArgumentException exception) { throw TaskBindingSpec.invalid(); }
    }

    String write(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw TaskBindingSpec.invalid(); }
    }
}
