package com.mdframe.forge.plugin.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mdframe.forge.plugin.system.dto.SysUserDTO;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.entity.SysOrg;
import com.mdframe.forge.plugin.system.entity.SysRegion;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysRegionMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.starter.core.session.LoginUser;
import com.mdframe.forge.starter.core.session.SessionHelper;
import com.mdframe.forge.starter.tenant.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

/** 个人资料与当前会话资料同步，以及主组织产生的区域资料同步。 */
@RequiredArgsConstructor
final class UserProfileOperations {

    private final SysUserMapper userMapper;
    private final SysOrgMapper orgMapper;
    private final SysRegionMapper regionMapper;
    private final IUserLoadService userLoadService;
    private final UserAccessPolicy access;

    boolean updateUserProfile(SysUserDTO dto) {
        Long currentUserId = SessionHelper.getUserId();
        if (currentUserId == null) {
            throw new RuntimeException("用户未登录");
        }

        SysUser user = new SysUser();
        user.setId(currentUserId);
        user.setUsername(dto.getUsername());
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());

        boolean updated = TenantContextHolder.executeIgnore(() -> userMapper.updateById(user) > 0);

        // 同步更新 Session 中的 LoginUser，确保 /auth/userInfo 返回最新数据
        if (updated) {
            syncSessionUserProfile(dto);
        }

        return updated;
    }

    boolean updateCurrentUserFromManagement(SysUserDTO dto) {
        SysUser user = new SysUser();
        user.setId(access.requireLoginUser().getUserId());
        user.setUsername(dto.getUsername());
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setIdCard(dto.getIdCard());
        user.setGender(dto.getGender());
        user.setAvatar(dto.getAvatar());
        user.setRegionCode(dto.getRegionCode());
        user.setRemark(dto.getRemark());

        boolean updated = TenantContextHolder.executeIgnore(() -> userMapper.updateById(user) > 0);
        if (updated) {
            syncSessionUserProfile(dto);
        }
        return updated;
    }

    void syncSessionUserProfile(SysUserDTO dto) {
        com.mdframe.forge.starter.core.session.LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null) {
            return;
        }
        if (dto.getUsername() != null) loginUser.setUsername(dto.getUsername());
        if (dto.getRealName() != null) loginUser.setRealName(dto.getRealName());
        if (dto.getPhone() != null) loginUser.setPhone(dto.getPhone());
        if (dto.getEmail() != null) loginUser.setEmail(dto.getEmail());
        if (dto.getAvatar() != null) loginUser.setAvatar(dto.getAvatar());
        SessionHelper.setLoginUser(loginUser);
    }

    void syncUserRegionFromMainOrg(Long userId, Long mainOrgId) {
        if (userId == null || mainOrgId == null) {
            return;
        }
        SysOrg org = TenantContextHolder.executeIgnore(() -> orgMapper.selectById(mainOrgId));
        if (org == null || StrUtil.isBlank(org.getRegionCode())) {
            return;
        }
        SysUser user = new SysUser();
        user.setId(userId);
        user.setRegionCode(org.getRegionCode());
        TenantContextHolder.executeIgnore(() -> userMapper.updateById(user));
    }

    void syncCurrentUserOrgSession(Long userId, Long tenantId) {
        LoginUser loginUser = SessionHelper.getLoginUser();
        if (loginUser == null
                || !Objects.equals(loginUser.getUserId(), userId)
                || !Objects.equals(loginUser.getTenantId(), tenantId)) {
            return;
        }

        LoginUser fresh = userLoadService.loadUserByUserId(userId, tenantId, loginUser.getActiveOrgId());
        fresh.setLoginTime(loginUser.getLoginTime());
        fresh.setLoginIp(loginUser.getLoginIp());
        fresh.setUserClient(loginUser.getUserClient());
        SessionHelper.setLoginUser(fresh);
    }

    void applyRegionToSession(LoginUser loginUser, String regionCode) {
        if (loginUser == null || StrUtil.isBlank(regionCode)) {
            return;
        }
        SysRegion region = TenantContextHolder.executeIgnore(() -> regionMapper.selectById(regionCode));
        if (region == null) {
            return;
        }
        loginUser.setRegionCode(region.getCode());
        loginUser.setRegionName(region.getName());
        loginUser.setRegionLevel(region.getLevel());
        loginUser.setRegionFullName(region.getFullName());
        loginUser.setRegionAncestors(buildRegionAncestors(region));
    }

    String buildRegionAncestors(SysRegion region) {
        if (region == null || StrUtil.isBlank(region.getCode())) {
            return null;
        }
        StringBuilder ancestors = new StringBuilder();
        String currentCode = region.getCode();
        while (StrUtil.isNotBlank(currentCode)) {
            String lookupCode = currentCode;
            SysRegion currentRegion = TenantContextHolder.executeIgnore(() -> regionMapper.selectById(lookupCode));
            if (currentRegion == null) {
                break;
            }
            if (ancestors.length() > 0) {
                ancestors.insert(0, ",");
            }
            ancestors.insert(0, currentCode);
            currentCode = currentRegion.getParentCode();
        }
        return ancestors.toString();
    }
}
