package com.jezetek.modules.system.menu;

import com.jezetek.core.runtime.module.MenuNode;
import com.jezetek.core.runtime.module.ModuleProvider;
import com.jezetek.modules.system.model.Menu;
import com.jezetek.modules.system.model.MenuDraft;
import com.jezetek.modules.system.repository.MenuRepository;
import com.jezetek.modules.system.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 模块菜单同步器：模块化的"引入即出菜单"落地点。
 *
 * <p>启动时收集所有 {@link ModuleProvider}，把声明菜单幂等 upsert 进 sys_menu
 * (业务键 parent+name 命中即更新，否则插入)，再按 roleCodes 用 MERGE 幂等绑定角色。
 * 每次启动以代码声明为准覆盖声明字段的值；模块移除依赖后其菜单不再被同步，
 * 声明是菜单的唯一代码来源(手工在界面上新增的菜单 module_code 为 null，不受影响)</p>
 */
@Service
public class MenuSyncService {

    private static final Logger log = LoggerFactory.getLogger(MenuSyncService.class);

    private final ObjectProvider<ModuleProvider> moduleProviders;

    private final MenuRepository menuRepository;

    private final RoleRepository roleRepository;

    private final JdbcClient jdbcClient;

    public MenuSyncService(
            ObjectProvider<ModuleProvider> moduleProviders,
            MenuRepository menuRepository,
            RoleRepository roleRepository,
            JdbcClient jdbcClient
    ) {
        this.moduleProviders = moduleProviders;
        this.menuRepository = menuRepository;
        this.roleRepository = roleRepository;
        this.jdbcClient = jdbcClient;
    }

    public void sync() {
        List<ModuleProvider> providers = moduleProviders.orderedStream()
                .sorted(Comparator.comparingInt(ModuleProvider::order))
                .toList();
        for (ModuleProvider provider : providers) {
            int[] counter = {0};
            for (MenuNode node : provider.menus()) {
                syncNode(provider, node, null, counter);
            }
            log.info("模块[{}]菜单同步完成，共 {} 个节点", provider.code(), counter[0]);
        }
    }

    private void syncNode(ModuleProvider provider, MenuNode node, Long parentId, int[] counter) {
        // 只带业务键(parent+name)与声明字段，不带 id：命中业务键则更新，否则插入
        Menu saved = menuRepository
                .saveCommand(toEntity(provider, node, parentId))
                .execute()
                .getModifiedEntity();
        counter[0]++;
        bindRoles(saved.id(), node.getRoleCodes());
        for (MenuNode child : node.getChildren()) {
            syncNode(provider, child, saved.id(), counter);
        }
    }

    private static Menu toEntity(ModuleProvider provider, MenuNode node, Long parentId) {
        return MenuDraft.$.produce(draft -> {
            draft.setName(node.getName());
            // 业务键由 name+parent 组成：根菜单必须显式 set null，
            // 未加载的 parent 会让 jimmer 判定业务键不完整而拒绝 upsert
            draft.setParent(parentId == null ? null : MenuDraft.$.produce(ref -> ref.setId(parentId)));
            draft.setType(node.getType().code());
            draft.setPath(node.getPath());
            draft.setComponent(node.getComponent());
            draft.setPerms(node.getPerms());
            draft.setIcon(node.getIcon());
            draft.setVisible(node.isVisible());
            draft.setSortOrder(node.getSortOrder());
            draft.setModuleCode(provider.code());
        });
    }

    /**
     * 角色菜单映射表没有对应实体，用 H2 的 MERGE 保证幂等
     */
    private void bindRoles(long menuId, Iterable<String> roleCodes) {
        for (String roleCode : roleCodes) {
            roleRepository.findByCode(roleCode, null).ifPresent(role ->
                    jdbcClient
                            .sql("merge into sys_role_menu_mapping (role_id, menu_id) key(role_id, menu_id) values (?, ?)")
                            .param(role.id())
                            .param(menuId)
                            .update()
            );
        }
    }
}
