package com.mdframe.forge.starter.flow.service.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import org.springframework.lang.Nullable;
import java.util.Map;

/** Resolves display names consistently for task lists, history and form initiators. */
@Slf4j
@Component
public class FlowUserDisplayNames {
    private final FlowOrgIntegrationService flowOrgIntegrationService;
    private final ISysUserService sysUserService;

    public FlowUserDisplayNames(@Nullable FlowOrgIntegrationService flowOrgIntegrationService,
                               ISysUserService sysUserService) {
        this.flowOrgIntegrationService = flowOrgIntegrationService;
        this.sysUserService = sysUserService;
    }

    public String resolve(String userId, String fallback) {
        if (!isBlank(userId) && flowOrgIntegrationService != null) {
            try {
                Map<String, Object> userInfo = flowOrgIntegrationService.getUserInfo(userId.trim());
                if (userInfo != null) {
                    String name = firstNonBlank(
                            userInfo.get("realName"), userInfo.get("name"), userInfo.get("nickname"));
                    if (!isBlank(name)) {
                        return name;
                    }
                }
            } catch (Exception e) {
                log.debug("反查任务用户姓名失败: userId={}", userId, e);
            }
        }
        if (!isBlank(userId) && sysUserService != null) {
            try {
                SysUser user = sysUserService.getById(Long.valueOf(userId.trim()));
                if (user != null && !isBlank(user.getRealName())) {
                    return user.getRealName().trim();
                }
            } catch (NumberFormatException ignored) {
                // 非数字用户标识由外部组织服务负责解析。
            } catch (Exception e) {
                log.debug("从用户表反查任务用户姓名失败: userId={}", userId, e);
            }
        }
        return isBlank(fallback) ? userId : fallback.trim();
    }

    private String firstNonBlank(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
