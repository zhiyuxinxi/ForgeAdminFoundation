package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mdframe.forge.plugin.system.entity.SysLoginLog;
import com.mdframe.forge.plugin.system.entity.SysOperationLog;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.mapper.SysLoginLogMapper;
import com.mdframe.forge.plugin.system.mapper.SysOperationLogMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.starter.log.domain.LoginLogInfo;
import com.mdframe.forge.starter.log.domain.OperationLogInfo;
import com.mdframe.forge.starter.log.service.ILogService;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 系统日志服务实现
 * 实现ILogService接口，持久化日志到数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemLogServiceImpl implements ILogService {

    private final SysOperationLogMapper operationLogMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public void saveOperationLog(OperationLogInfo logInfo) {
        try {
            // Only missing display names may be enriched; event identity belongs to the request.
            if (logInfo.getUserId() != null
                    && (logInfo.getUsername() == null || logInfo.getOperatorName() == null)) {
                fillOperationLogUserInfo(logInfo);
            }
            // 转换并保存
            SysOperationLog entity = new SysOperationLog();
            BeanUtil.copyProperties(logInfo, entity);
            TenantContextHolder.executeWithTenant(logInfo.getTenantId(), () -> {
                operationLogMapper.insert(entity);
            });

            log.debug("操作日志保存成功: userId={}, module={}, type={}",
                    logInfo.getUserId(), logInfo.getOperationModule(), logInfo.getOperationType());
        } catch (Exception e) {
            log.error("操作日志保存失败", e);
        }
    }

    @Override
    public void saveLoginLog(LoginLogInfo logInfo) {
        try {
            if (logInfo.getUserId() != null) {
                fillLoginLogUserInfo(logInfo);
            }
            // 转换并保存
            SysLoginLog entity = new SysLoginLog();
            BeanUtil.copyProperties(logInfo, entity);
            entity.setClientCode(logInfo.getClientCode()); // 确保设置客户端代码映射
            loginLogMapper.insert(entity);

            log.debug("登录日志保存成功: userId={}, type={}, status={}, client={}",
                    logInfo.getUserId(), logInfo.getLoginType(), logInfo.getLoginStatus(), logInfo.getClientCode());
        } catch (Exception e) {
            log.error("登录日志保存失败", e);
        }
    }

    private void fillOperationLogUserInfo(OperationLogInfo logInfo) {
        try {
            SysUser sysUser = selectUserIgnoreTenant(logInfo.getUserId());
            if (sysUser == null) {
                log.debug("未查询到操作日志用户信息: userId={}", logInfo.getUserId());
                return;
            }
            if (logInfo.getUsername() == null) {
                logInfo.setUsername(sysUser.getUsername());
            }
            if (logInfo.getOperatorName() == null) {
                logInfo.setOperatorName(resolveOperatorName(sysUser));
            }
        } catch (Exception e) {
            log.debug("从SysUser获取操作日志用户信息失败: userId={}", logInfo.getUserId(), e);
        }
    }

    private void fillLoginLogUserInfo(LoginLogInfo logInfo) {
        try {
            SysUser sysUser = selectUserIgnoreTenant(logInfo.getUserId());
            if (sysUser == null) {
                log.debug("未查询到登录日志用户信息: userId={}", logInfo.getUserId());
                return;
            }
            logInfo.setUsername(sysUser.getUsername());
            if (logInfo.getTenantId() == null) {
                logInfo.setTenantId(sysUser.getTenantId());
            }
        } catch (Exception e) {
            log.debug("从SysUser获取登录日志用户信息失败: userId={}", logInfo.getUserId(), e);
        }
    }

    private SysUser selectUserIgnoreTenant(Long userId) {
        return TenantContextHolder.executeIgnore(() -> sysUserMapper.selectById(userId));
    }

    private String resolveOperatorName(SysUser sysUser) {
        if (sysUser.getRealName() != null && !sysUser.getRealName().isBlank()) {
            return sysUser.getRealName();
        }
        return sysUser.getUsername();
    }
}
