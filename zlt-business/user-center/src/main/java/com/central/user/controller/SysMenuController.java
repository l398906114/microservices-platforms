package com.central.user.controller;

import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.central.common.constant.CommonConstant;
import com.central.common.context.LoginUserContextHolder;
import com.central.common.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import com.central.user.service.ISysMenuService;

/**
 * @author 作者 owen E-mail: 624191343@qq.com
 */
@RestController
@Tag(name = "菜单模块api")
@Slf4j
@RequestMapping("/menus")
public class SysMenuController {
    @Autowired
    private ISysMenuService menuService;

    /**
     * 两层循环实现建树
     *
     * @param sysMenus
     * @return
     */
    public static List<SysMenu> treeBuilder(List<SysMenu> sysMenus) {
        List<SysMenu> menus = new ArrayList<>();
        for (SysMenu sysMenu : sysMenus) {
            if (ObjectUtil.equal(-1L, sysMenu.getParentId())) {
                menus.add(sysMenu);
            }
            for (SysMenu menu : sysMenus) {
                if (menu.getParentId().equals(sysMenu.getId())) {
                    if (sysMenu.getSubMenus() == null) {
                        sysMenu.setSubMenus(new ArrayList<>());
                    }
                    sysMenu.getSubMenus().add(menu);
                }
            }
        }
        return menus;
    }

    /**
     * 删除菜单
     *
     * @param id
     */
    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        try {
            menuService.removeById(id);
            return Result.succeed("操作成功");
        } catch (Exception ex) {
            log.error("memu-delete-error", ex);
            return Result.failed("操作失败");
        }
    }

    @Operation(summary = "根据roleId获取对应的菜单")
    @GetMapping("/{roleId}/menus")
    public List<Map<String, Object>> findMenusByRoleId(@PathVariable Long roleId) {
        Set<Long> roleIds = new HashSet<>();
        roleIds.add(roleId);
        //获取该角色对应的菜单
        List<SysMenu> roleMenus = menuService.findByRoles(roleIds);
        //全部的菜单列表
        List<SysMenu> allMenus = menuService.findAll();
        List<Map<String, Object>> authTrees = new ArrayList<>();

        Map<Long, SysMenu> roleMenusMap = roleMenus.stream().collect(Collectors.toMap(SysMenu::getId, SysMenu -> SysMenu));

        for (SysMenu sysMenu : allMenus) {
            Map<String, Object> authTree = new HashMap<>();
            authTree.put("id", sysMenu.getId());
            authTree.put("name", sysMenu.getName());
            authTree.put("pId", sysMenu.getParentId());
            authTree.put("open", true);
            authTree.put("checked", false);
            if (roleMenusMap.get(sysMenu.getId()) != null) {
                authTree.put("checked", true);
            }
            authTrees.add(authTree);
        }
        return authTrees;
    }

    @Operation(summary = "根据roleCodes获取对应的权限")
    @SuppressWarnings("unchecked")
    @Cacheable(value = "menu", key ="#roleCodes")
    @GetMapping("/{roleCodes}")
    public List<SysMenu> findMenuByRoles(@PathVariable String roleCodes) {
        List<SysMenu> result = null;
        if (StringUtils.isNotEmpty(roleCodes)) {
            Set<String> roleSet = (Set<String>)Convert.toCollection(HashSet.class, String.class, roleCodes);
            result = menuService.findByRoleCodes(roleSet, CommonConstant.PERMISSION);
        }
        return result;
    }

    /**
     * 给角色分配菜单
     */
    @Operation(summary = "角色分配菜单")
    @PostMapping("/granted")
    public Result setMenuToRole(@RequestBody SysMenu sysMenu) {
        menuService.setMenuToRole(sysMenu.getRoleId(), sysMenu.getMenuIds());
        return Result.succeed("操作成功");
    }

    @Operation(summary = "查询所有菜单")
    @GetMapping("/findAlls")
    public PageResult<SysMenu> findAlls() {
        List<SysMenu> list = menuService.findAll();
        return PageResult.<SysMenu>builder().data(list).code(0).count((long) list.size()).build();
    }

    @Operation(summary = "获取菜单以及顶级菜单")
    @GetMapping("/findOnes")
    public PageResult<SysMenu> findOnes() {
        List<SysMenu> list = menuService.findOnes();
        return PageResult.<SysMenu>builder().data(list).code(0).count((long) list.size()).build();
    }

    /**
     * 添加菜单 或者 更新
     *
     * @param menu
     * @return
     */
    @Operation(summary = "新增菜单")
    @PostMapping("saveOrUpdate")
    public Result saveOrUpdate(@RequestBody SysMenu menu) {
        try {
            if (menu.getId() == null) {
                menu.setCreatorId(LoginUserContextHolder.getUser().getId());
            }
            menuService.saveOrUpdate(menu);
            return Result.succeed("操作成功");
        } catch (Exception ex) {
            log.error("memu-saveOrUpdate-error", ex);
            return Result.failed("操作失败");
        }
    }

    /**
     * 当前登录用户的菜单
     *
     * @return
     */
    @GetMapping("/current")
    @Operation(summary = "查询当前用户菜单")
    public List<SysMenu> findMyMenu() {
        LoginAppUser user = LoginUserContextHolder.getUser();
        List<SysMenu> menus = menuService.findByUserId(user.getId(), CommonConstant.MENU);
        return treeBuilder(menus);
    }
}
