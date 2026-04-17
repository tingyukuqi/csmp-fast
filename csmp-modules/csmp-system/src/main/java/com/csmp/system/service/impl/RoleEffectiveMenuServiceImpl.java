package com.csmp.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csmp.system.domain.SysRole;
import com.csmp.system.domain.SysRoleEffectiveMenu;
import com.csmp.system.domain.SysRoleHiddenMenu;
import com.csmp.system.domain.SysRoleMenu;
import com.csmp.system.mapper.SysRoleEffectiveMenuMapper;
import com.csmp.system.mapper.SysRoleHiddenMenuMapper;
import com.csmp.system.mapper.SysRoleMapper;
import com.csmp.system.mapper.SysRoleMenuMapper;
import com.csmp.system.service.IRoleEffectiveMenuService;
import com.csmp.system.service.IRoleHierarchyService;
import com.csmp.system.service.ISysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 角色有效菜单管理服务实现
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RoleEffectiveMenuServiceImpl implements IRoleEffectiveMenuService {

    private final SysRoleEffectiveMenuMapper effectiveMenuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleHiddenMenuMapper hiddenMenuMapper;
    private final SysRoleMapper roleMapper;
    private final IRoleHierarchyService hierarchyService;
    private final ISysRoleService roleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshEffectiveMenu(Long roleId) {
        // 1. 获取祖先链（从顶级到当前）
        List<Long> ancestorChain = hierarchyService.getAncestorChain(roleId);

        // 2. 按链顺序合并菜单
        Set<Long> effectiveMenuIds = new LinkedHashSet<>();
        List<SysRoleEffectiveMenu> effectiveRecords = new ArrayList<>();

        for (Long chainRoleId : ancestorChain) {
            // 获取该角色自有菜单
            List<Long> ownMenuIds = roleMenuMapper.selectList(
                    new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, chainRoleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .toList();

            // 获取该角色隐藏的继承菜单
            List<Long> hiddenIds = hiddenMenuMapper.selectHiddenMenuIdsByRoleId(chainRoleId);
            Set<Long> hiddenMenuIds = new HashSet<>(hiddenIds);

            // 合并自有菜单
            for (Long menuId : ownMenuIds) {
                if (effectiveMenuIds.add(menuId)) {
                    SysRoleEffectiveMenu record = new SysRoleEffectiveMenu();
                    record.setRoleId(roleId);
                    record.setMenuId(menuId);
                    record.setSource(chainRoleId.equals(roleId) ? "OWN" : "INHERITED");
                    record.setInheritFromRoleId(chainRoleId.equals(roleId) ? null : chainRoleId);
                    effectiveRecords.add(record);
                }
            }

            // 应用隐藏：移除被隐藏的菜单
            if (!hiddenMenuIds.isEmpty()) {
                effectiveMenuIds.removeAll(hiddenMenuIds);
                effectiveRecords.removeIf(r -> hiddenMenuIds.contains(r.getMenuId()));
            }
        }

        // 3. 删除旧的有效菜单
        effectiveMenuMapper.delete(new LambdaQueryWrapper<SysRoleEffectiveMenu>()
            .eq(SysRoleEffectiveMenu::getRoleId, roleId));

        // 4. 批量插入新的有效菜单
        if (CollUtil.isNotEmpty(effectiveRecords)) {
            effectiveMenuMapper.insertBatch(effectiveRecords);
        }

        log.info("角色有效菜单刷新完成, roleId={}, 有效菜单数={}", roleId, effectiveRecords.size());
    }

    @Override
    public void refreshEffectiveMenuCascade(Long roleId) {
        List<Long> descendantIds = hierarchyService.getDescendantRoleIds(roleId);
        for (Long descendantId : descendantIds) {
            try {
                refreshEffectiveMenu(descendantId);
            } catch (Exception e) {
                log.error("刷新角色有效菜单失败, roleId={}", descendantId, e);
            }
        }
        // 刷新完成后清除受影响角色关联的在线用户
        for (Long descendantId : descendantIds) {
            roleService.cleanOnlineUserByRole(descendantId);
        }
    }

    @Override
    public void refreshAllEffectiveMenus() {
        List<SysRole> allRoles = roleMapper.selectList(
            new LambdaQueryWrapper<SysRole>()
                .select(SysRole::getRoleId));
        for (SysRole role : allRoles) {
            try {
                refreshEffectiveMenu(role.getRoleId());
            } catch (Exception e) {
                log.error("全量刷新角色有效菜单失败, roleId={}", role.getRoleId(), e);
            }
        }
        log.info("全量角色有效菜单刷新完成, 共处理{}个角色", allRoles.size());
    }

    @Override
    public Set<Long> getEffectiveMenuIds(Long roleId) {
        List<Long> ids = effectiveMenuMapper.selectEffectiveMenuIdsByRoleId(roleId);
        return new HashSet<>(ids);
    }
}
