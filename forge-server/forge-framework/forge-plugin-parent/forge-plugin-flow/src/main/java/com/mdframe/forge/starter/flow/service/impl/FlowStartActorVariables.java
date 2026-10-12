package com.mdframe.forge.starter.flow.service.impl;

import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.flow.service.FlowOrgIntegrationService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/** Resolves display identity and optional organization variables; owns compatibility fallbacks. */
@Slf4j
final class FlowStartActorVariables {

    private final FlowOrgIntegrationService flowOrgIntegrationService;
    private final ISysUserService sysUserService;

    FlowStartActorVariables(FlowOrgIntegrationService flowOrgIntegrationService, ISysUserService sysUserService) {
        this.flowOrgIntegrationService = flowOrgIntegrationService;
        this.sysUserService = sysUserService;
    }

    String resolveUserDisplayName(String userId, String candidateName) {
        String fallback = isBlank(candidateName) ? userId : candidateName.trim();
        if (isBlank(userId)) {
            return fallback;
        }
        if (flowOrgIntegrationService != null) {
            try {
                Map<String, Object> userInfo = flowOrgIntegrationService.getUserInfo(userId);
                if (userInfo != null) {
                    String realName = firstNonBlank(
                            userInfo.get("realName"), userInfo.get("name"), userInfo.get("nickname"));
                    if (!isBlank(realName)) {
                        return realName;
                    }
                }
            } catch (Exception e) {
                log.debug("反查流程发起人姓名失败: userId={}", userId, e);
            }
        }
        if (sysUserService != null) {
            try {
                SysUser user = sysUserService.getById(Long.valueOf(userId));
                if (user != null && !isBlank(user.getRealName())) {
                    return user.getRealName().trim();
                }
            } catch (NumberFormatException ignored) {
                // 外部系统可能使用非数字用户标识，保留调用方传入值。
            } catch (Exception e) {
                log.debug("从用户表反查流程发起人姓名失败: userId={}", userId, e);
            }
        }
        return fallback;
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

    void enrich(String userId, Long tenantId, Map<String, Object> variables) {
        addLeader(userId, tenantId, variables);
        addRegion(userId, tenantId, variables);
        addActiveOrganizationRoles(userId, tenantId, variables);
        addOrganizations(userId, tenantId, variables);
    }

    private void addLeader(String userId, Long tenantId, Map<String, Object> vars) {
        // 自动注入上级领导变量（兼容 BPMN 中直接使用 ${initiatorLeader} 的老式写法）
        if (userId != null && !userId.isEmpty() && flowOrgIntegrationService != null) {
            try {
                String leaderId = flowOrgIntegrationService.getLeaderUserIdByLevel(userId, 1);
                if (leaderId == null) {
                    List<String> leaderIds = flowOrgIntegrationService.getLeaderUserIds(userId);
                    leaderId = leaderIds.isEmpty() ? null : leaderIds.get(0);
                }
                if (leaderId != null) {
                    vars.put("initiatorLeader", leaderId);
                    log.info("自动注入上级领导变量：initiatorLeader={}", leaderId);
                } else {
                    log.warn("未找到发起人的上级领导，initiatorLeader 变量未注入：userId={}", userId);
                }
            } catch (Exception e) {
                log.warn("获取上级领导失败，initiatorLeader 变量未注入：userId={}", userId, e);
            }
        }
    }

    private void addRegion(String userId, Long tenantId, Map<String, Object> vars) {
        // 自动注入行政区划编码（用于按区域查找审批人）
        if (userId != null && !userId.isEmpty() && sysUserService != null) {
            try {
                SysUser user = sysUserService.selectUserById(Long.parseLong(userId));
                if (user != null && user.getRegionCode() != null && !user.getRegionCode().isEmpty()) {
                    vars.put("regionCode", user.getRegionCode());
                    vars.put("startUserRegionCode", user.getRegionCode());
                    log.info("自动注入行政区划编码变量：regionCode={}, startUserRegionCode={}",
                            user.getRegionCode(), user.getRegionCode());
                } else {
                    log.warn("用户行政区划编码为空，regionCode 变量未注入：userId={}", userId);
                }
            } catch (Exception e) {
                log.warn("获取用户行政区划编码失败，regionCode 变量未注入：userId={}", userId, e);
            }
        }
    }

    private void addActiveOrganizationRoles(String userId, Long tenantId, Map<String, Object> vars) {
        // 自动注入用户角色信息
        if (userId != null && !userId.isEmpty() && sysUserService != null) {
            try {
                LoginUser loginUser = SessionHelper.getLoginUser();
                Long activeOrgId = loginUser == null ? null : loginUser.getActiveOrgId();
                Long roleTenantId = loginUser == null ? tenantId : loginUser.getTenantId();
                List<Long> roleIds = activeOrgId == null
                        ? List.of()
                        : sysUserService.selectUserOrgRoleIds(Long.parseLong(userId), activeOrgId, roleTenantId);
                if (roleIds != null && !roleIds.isEmpty()) {
                    // 注入角色ID列表（逗号分隔）
                    String roleIdsStr = roleIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","));
                    vars.put("startUserRoleIds", roleIdsStr);
                    log.info("自动注入用户角色ID变量：startUserRoleIds={}", roleIdsStr);
                }
                if (activeOrgId != null) {
                    vars.put("startUserActiveOrgId", String.valueOf(activeOrgId));
                    log.info("自动注入当前组织变量：startUserActiveOrgId={}", activeOrgId);
                }
            } catch (Exception e) {
                log.warn("获取用户角色失败，startUserRoleIds 变量未注入：userId={}", userId, e);
            }
        }
    }

    private void addOrganizations(String userId, Long tenantId, Map<String, Object> vars) {
        // 自动注入组织ID信息
        if (userId != null && !userId.isEmpty() && sysUserService != null) {
            try {
                List<Long> orgIds = sysUserService.selectUserOrgIds(Long.parseLong(userId));
                if (orgIds != null && !orgIds.isEmpty()) {
                    // 注入组织ID列表（逗号分隔）
                    String orgIdsStr = orgIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","));
                    vars.put("startUserOrgIds", orgIdsStr);
                    log.info("自动注入用户组织ID变量：startUserOrgIds={}", orgIdsStr);
                }
            } catch (Exception e) {
                log.warn("获取用户组织失败，startUserOrgIds 变量未注入：userId={}", userId, e);
            }
        }
    }
}
