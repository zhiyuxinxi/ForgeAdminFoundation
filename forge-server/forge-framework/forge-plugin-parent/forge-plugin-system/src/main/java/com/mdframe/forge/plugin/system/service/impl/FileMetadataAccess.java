package com.mdframe.forge.plugin.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mdframe.forge.plugin.system.entity.SysFileMetadata;
import com.mdframe.forge.starter.core.exception.BusinessException;
import com.mdframe.forge.starter.core.i18n.UiMessages;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;

import java.util.Objects;

/** Access rules shared by the external file and metadata entry points. */
final class FileMetadataAccess {
    private FileMetadataAccess() {
    }

    static LoginUser currentUser() {
        LoginUser user = SessionHelper.getLoginUser();
        if (user == null || user.getUserId() == null || user.getTenantId() == null) {
            throw denied();
        }
        return user;
    }

    static boolean canRead(SysFileMetadata metadata) {
        LoginUser user = currentUser();
        return inTenant(metadata, user) && (Boolean.FALSE.equals(metadata.getIsPrivate())
                || canManage(metadata, user));
    }

    static boolean canWrite(SysFileMetadata metadata) {
        LoginUser user = currentUser();
        return inTenant(metadata, user) && canManage(metadata, user);
    }

    static void requireRead(SysFileMetadata metadata) {
        if (!canRead(metadata)) {
            throw denied();
        }
    }

    static void requireWrite(SysFileMetadata metadata) {
        if (!canWrite(metadata)) {
            throw denied();
        }
    }

    static void applyReadScope(LambdaQueryWrapper<SysFileMetadata> wrapper) {
        LoginUser user = currentUser();
        wrapper.eq(SysFileMetadata::getTenantId, user.getTenantId())
                .eq(SysFileMetadata::getStatus, 1);
        if (!isFileAdministrator(user)) {
            wrapper.and(visible -> visible.eq(SysFileMetadata::getIsPrivate, false)
                    .or().eq(SysFileMetadata::getUploaderId, user.getUserId()));
        }
    }

    private static boolean inTenant(SysFileMetadata metadata, LoginUser user) {
        return metadata != null && Objects.equals(metadata.getStatus(), 1)
                && Objects.equals(metadata.getTenantId(), user.getTenantId());
    }

    private static boolean canManage(SysFileMetadata metadata, LoginUser user) {
        return Objects.equals(metadata.getUploaderId(), user.getUserId()) || isFileAdministrator(user);
    }

    private static boolean isFileAdministrator(LoginUser user) {
        return user.isAdmin() || StpUtil.hasPermission("*:*:*");
    }

    private static BusinessException denied() {
        return new BusinessException(403, UiMessages.message("customer.fileOwnership"));
    }
}
