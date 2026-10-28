package com.mdframe.forge.plugin.generator.service.task;

import com.mdframe.forge.plugin.generator.codegen.LowcodeStaticCodegenContributor;
import com.mdframe.forge.plugin.generator.domain.entity.AiCrudConfig;
import com.mdframe.forge.plugin.generator.util.LowcodeCodegenOptionUtils;
import lombok.RequiredArgsConstructor;
import org.apache.velocity.VelocityContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Downloaded clients call the installed public owner; they never copy task tables or callback code. */
@Component
@RequiredArgsConstructor
public class TaskBindingCodegen implements LowcodeStaticCodegenContributor {
    private final TaskBindingSnapshots snapshots;

    @Override
    public String capabilityId() { return "public-business-task"; }

    @Override
    public boolean supports(AiCrudConfig config) {
        return "PUBLISHED".equals(config.getPublishStatus())
                && snapshots.tree(config.getOptions()).path(TaskBindingSnapshots.OPTION).isArray()
                && !snapshots.tree(config.getOptions()).path(TaskBindingSnapshots.OPTION).isEmpty()
                && !snapshots.bindings(snapshots.latest(config.getConfigKey())).isEmpty();
    }

    @Override
    public void contributeFiles(AiCrudConfig config, VelocityContext context, Map<String, String> files) {
        var version = snapshots.latest(config.getConfigKey());
        var bindings = snapshots.bindings(version);
        String client = """
                import { request } from '@/utils'
                // Requires the published Foundation config and public task owner in this deployment.
                const configKey = %s
                export const taskBindings = %s
                const path = (bindingKey, recordId) => '/ai/crud/' + encodeURIComponent(configKey)
                  + '/tasks/' + encodeURIComponent(bindingKey) + '/' + encodeURIComponent(recordId)
                export const generateTask = (bindingKey, recordId) => request.post(path(bindingKey, recordId))
                export const sourceTask = (bindingKey, recordId) => request.get(path(bindingKey, recordId))
                export const startTask = id => request.post('/api/task/' + encodeURIComponent(id) + '/start')
                export const completeTask = (id, requestKey, result) => request.post(
                  '/api/task/' + encodeURIComponent(id) + '/complete', { requestKey, resultJson: JSON.stringify(result) })
                """.formatted(snapshots.write(config.getConfigKey()), snapshots.write(bindings));
        String apiPath = context.get("frontendApiBasePath") instanceof String path ? path
                : LowcodeCodegenOptionUtils.DEFAULT_FRONTEND_API_BASE_PATH;
        files.put(apiPath + "/public-task/" + config.getId() + ".js", client);
        files.put("public-capabilities/task-" + config.getId() + ".json", snapshots.write(Map.of(
                "capability", capabilityId(), "configKey", config.getConfigKey(), "publishedVersionId", version.getId(),
                "sourceType", LowcodeTaskSource.SOURCE_TYPE, "bindings", bindings)));
    }
}
