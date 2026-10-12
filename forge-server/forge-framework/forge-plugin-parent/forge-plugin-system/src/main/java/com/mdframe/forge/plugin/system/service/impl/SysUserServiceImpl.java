package com.mdframe.forge.plugin.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.plugin.system.dto.BatchUserRoleBindDTO;
import com.mdframe.forge.plugin.system.dto.BatchUserTenantBindDTO;
import com.mdframe.forge.plugin.system.dto.SysUserDTO;
import com.mdframe.forge.plugin.system.dto.SysUserQuery;
import com.mdframe.forge.plugin.system.entity.SysUser;
import com.mdframe.forge.plugin.system.mapper.SysOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysPostMapper;
import com.mdframe.forge.plugin.system.mapper.SysRegionMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserOrgRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserPostMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserRoleMapper;
import com.mdframe.forge.plugin.system.mapper.SysUserTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysTenantMapper;
import com.mdframe.forge.plugin.system.mapper.SysRoleMapper;
import com.mdframe.forge.plugin.system.service.ISysUserService;
import com.mdframe.forge.plugin.system.service.IUserLoadService;
import com.mdframe.forge.plugin.system.dto.UserTenantBindDTO;
import com.mdframe.forge.plugin.system.vo.SysUserTenantVO;
import com.mdframe.forge.plugin.system.vo.UserOrgBindingVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 用户管理公开入口：保留事务与接口契约，按职责委派账户、资料和关系操作。 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    private final UserAccessPolicy access;
    private final UserProfileOperations profile;
    private final TenantMembershipOperations memberships;
    private final UserAssignmentQueries queries;
    private final UserOrganizationAssignments organizations;
    private final UserRoleAssignments roles;
    private final UserAccountOperations accounts;

    public SysUserServiceImpl(SysUserMapper userMapper,
                              SysUserRoleMapper userRoleMapper,
                              SysUserOrgRoleMapper userOrgRoleMapper,
                              SysUserOrgMapper userOrgMapper,
                              SysUserPostMapper userPostMapper,
                              SysUserTenantMapper userTenantMapper,
                              SysTenantMapper tenantMapper,
                              SysRoleMapper roleMapper,
                              SysRoleOrgMapper roleOrgMapper,
                              SysOrgMapper orgMapper,
                              SysPostMapper postMapper,
                              SysRegionMapper regionMapper,
                              IUserLoadService userLoadService) {
        this.access = new UserAccessPolicy(userMapper, userTenantMapper, tenantMapper);
        this.profile = new UserProfileOperations(userMapper, orgMapper, regionMapper, userLoadService, access);
        this.memberships = new TenantMembershipOperations(userMapper, userRoleMapper, userOrgRoleMapper,
                userOrgMapper, userPostMapper, userTenantMapper, access);
        UserRolePolicy rolePolicy = new UserRolePolicy(userOrgMapper, roleMapper, roleOrgMapper, access);
        this.queries = new UserAssignmentQueries(userRoleMapper, userOrgRoleMapper, userOrgMapper,
                userPostMapper, orgMapper, access, rolePolicy);
        this.organizations = new UserOrganizationAssignments(userMapper, userOrgRoleMapper, userOrgMapper,
                userPostMapper, orgMapper, postMapper, access, profile, memberships);
        this.roles = new UserRoleAssignments(userMapper, userRoleMapper, userOrgRoleMapper, userOrgMapper,
                access, profile, memberships, rolePolicy, queries);
        this.accounts = new UserAccountOperations(userMapper, userTenantMapper, access, profile,
                memberships, organizations, roles);
    }

    @Override
    public IPage<SysUser> selectUserPage(SysUserQuery query) {
        return accounts.selectUserPage(query);
    }

    @Override
    public List<SysUser> selectExportList(SysUserQuery query) {
        return accounts.selectExportList(query);
    }

    @Override
    public SysUser selectUserById(Long id) {
        return accounts.selectUserById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertUser(SysUserDTO dto) {
        return accounts.insertUser(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(SysUserDTO dto) {
        return accounts.updateUser(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUserById(Long id) {
        return accounts.deleteUserById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUserByIds(Long[] ids) {
        return accounts.deleteUserByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserRoles(Long userId, Long[] roleIds) {
        access.assertUserActionAllowed("system:user:edit");
        return roles.bindUserRoles(userId, roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserRoles(Long userId, Long[] roleIds, Long tenantId) {
        access.assertUserActionAllowed("system:user:edit");
        return roles.bindUserRoles(userId, roleIds, tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchBindUserRoles(BatchUserRoleBindDTO dto) {
        access.assertUserActionAllowed("system:user:edit");
        return roles.batchBindUserRoles(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unbindUserRoles(Long userId, Long[] roleIds) {
        return roles.unbindUserRoles(userId, roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserOrg(Long userId, Long orgId, Integer isMain) {
        return organizations.bindUserOrg(userId, orgId, isMain);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unbindUserOrg(Long userId, Long orgId) {
        return organizations.unbindUserOrg(userId, orgId);
    }

    @Override
    public List<Long> selectUserRoleIds(Long userId) {
        return queries.selectUserRoleIds(userId);
    }

    @Override
    public List<Long> selectUserRoleIds(Long userId, Long tenantId) {
        return queries.selectUserRoleIds(userId, tenantId);
    }

    @Override
    public List<Long> selectUserOrgIds(Long userId) {
        return queries.selectUserOrgIds(userId);
    }

    @Override
    public List<Long> selectUserOrgIds(Long userId, Long tenantId) {
        return queries.selectUserOrgIds(userId, tenantId);
    }

    @Override
    public List<UserOrgBindingVO> selectUserOrgBindings(Long userId, Long tenantId) {
        return queries.selectUserOrgBindings(userId, tenantId);
    }

    @Override
    public List<Long> selectUserOrgRoleIds(Long userId, Long orgId, Long tenantId) {
        return queries.selectUserOrgRoleIds(userId, orgId, tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserOrgRoles(Long userId, Long orgId, List<Long> roleIds, Long tenantId) {
        access.assertUserActionAllowed("system:user:edit");
        return roles.bindUserOrgRoles(userId, orgId, roleIds, tenantId);
    }

    @Override
    public List<SysUserTenantVO> selectUserTenants(Long userId) {
        return memberships.selectUserTenants(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserTenants(Long userId, UserTenantBindDTO dto) {
        return memberships.bindUserTenants(userId, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchBindUserTenant(BatchUserTenantBindDTO dto) {
        return memberships.batchBindUserTenant(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserOrgs(Long userId, List<Long> orgIds, Long mainOrgId) {
        access.assertUserActionAllowed("system:user:edit");
        return organizations.bindUserOrgs(userId, orgIds, mainOrgId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserOrgs(Long userId, List<Long> orgIds, Long mainOrgId, Long requestedTenantId) {
        access.assertUserActionAllowed("system:user:edit");
        return organizations.bindUserOrgs(userId, orgIds, mainOrgId, requestedTenantId);
    }

    @Override
    public void doUntieDisable(Long userId) {
        accounts.doUntieDisable(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(Long userId, String newPassword) {
        return accounts.resetPassword(userId, newPassword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserStatus(Long userId, Integer status) {
        return accounts.updateUserStatus(userId, status);
    }

    @Override
    public boolean updateUserProfile(SysUserDTO dto) {
        return profile.updateUserProfile(dto);
    }

    @Override
    public List<Long> selectUserPostIds(Long userId) {
        return queries.selectUserPostIds(userId);
    }

    @Override
    public List<Long> selectUserPostIds(Long userId, Long tenantId) {
        return queries.selectUserPostIds(userId, tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserPosts(Long userId, List<Long> postIds, Long mainPostId) {
        return organizations.bindUserPosts(userId, postIds, mainPostId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bindUserPosts(Long userId, List<Long> postIds, Long mainPostId, Long requestedTenantId) {
        return organizations.bindUserPosts(userId, postIds, mainPostId, requestedTenantId);
    }
}
