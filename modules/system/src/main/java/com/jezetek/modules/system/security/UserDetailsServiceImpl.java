package com.jezetek.modules.system.security;

import com.jezetek.core.runtime.security.DataScope;
import com.jezetek.core.runtime.security.LoginUser;
import com.jezetek.core.runtime.security.LoginUserLoader;
import com.jezetek.modules.system.model.Dept;
import com.jezetek.modules.system.model.Fetchers;
import com.jezetek.modules.system.model.Role;
import com.jezetek.modules.system.model.User;
import com.jezetek.modules.system.repository.UserRepository;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 用户凭据与权限的加载器，同时扮演两个角色：
 * <ul>
 *   <li>Spring Security 登录认证(loadUserByUsername)</li>
 *   <li>core 安全契约的 JWT 回库加载(LoginUserLoader)</li>
 * </ul>
 *
 * <p>权限组装规则：ADMIN 角色直通({@code *:*:*})；
 * 普通角色取其绑定菜单(含按钮)的 perms 并集；
 * 数据范围多角色取最大(编码最小)，登录时合并进会话</p>
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService, LoginUserLoader, Fetchers {

    /** 登录/回库统一抓取形状：密文 + 启用状态 + 部门 + 角色(编码/数据范围/自定义部门) + 角色菜单的权限标识 */
    private static final Fetcher<User> AUTH_FETCHER =
            USER_FETCHER
                    .username()
                    .nickname()
                    .password()
                    .enabled()
                    .dept(DEPT_FETCHER)
                    .roles(
                            ROLE_FETCHER
                                    .code()
                                    .dataScope()
                                    .customDepts(DEPT_FETCHER)
                                    .menus(
                                            MENU_FETCHER
                                                    .perms()
                                    )
                    );

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public LoginUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository
                .findByUsername(username, AUTH_FETCHER)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
        return toDetails(user);
    }

    @Override
    @Nullable
    public LoginUser load(long userId) {
        return Optional.ofNullable(userRepository.findById(userId, AUTH_FETCHER))
                .filter(User::enabled)
                .map(UserDetailsServiceImpl::toLoginUser)
                .orElse(null);
    }

    private LoginUserDetails toDetails(User user) {
        return new LoginUserDetails(toLoginUser(user), user.password(), user.enabled());
    }

    private static LoginUser toLoginUser(User user) {
        Set<String> roleCodes = new HashSet<>();
        Set<Long> roleIds = new HashSet<>();
        List<DataScope> roleScopes = new ArrayList<>();
        Long selfDeptId = user.dept() != null ? user.dept().id() : null;
        for (Role role : user.roles()) {
            roleCodes.add(role.code());
            roleIds.add(role.id());
            roleScopes.add(toScope(role, selfDeptId));
        }
        return new LoginUser(
                user.id(),
                user.username(),
                user.nickname(),
                roleCodes,
                roleIds,
                resolvePerms(roleCodes, user),
                DataScope.broadest(roleScopes)
        );
    }

    /** 单个角色的数据范围(自定义=精确勾选集合；本部门及以下的子孙扩展由生效点负责) */
    private static DataScope toScope(Role role, Long selfDeptId) {
        DataScope.Level level = DataScope.Level.ofCode(role.dataScope());
        return switch (level) {
            case ALL -> DataScope.ALL;
            case CUSTOM -> new DataScope(level, Set.copyOf(role.customDepts().stream()
                    .map(Dept::id)
                    .toList()), selfDeptId);
            case DEPT, DEPT_AND_CHILD -> new DataScope(level, Set.of(), selfDeptId);
            case SELF -> new DataScope(level, Set.of(), selfDeptId);
        };
    }

    private static Set<String> resolvePerms(Set<String> roleCodes, User user) {
        Set<String> perms = new HashSet<>();
        if (roleCodes.contains("ADMIN")) {
            perms.add(LoginUser.ALL_PERMS);
            return perms;
        }
        for (Role role : user.roles()) {
            role.menus().stream()
                    .map(menu -> menu.perms())
                    .filter(perm -> perm != null && !perm.isBlank())
                    .forEach(perms::add);
        }
        return perms;
    }
}
