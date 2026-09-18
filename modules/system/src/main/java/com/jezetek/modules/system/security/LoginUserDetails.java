package com.jezetek.modules.system.security;

import com.jezetek.core.runtime.security.LoginUser;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Spring Security 认证过程的 UserDetails 载体：
 * 认证阶段携带密文与启用状态，成功后可取回 {@link LoginUser}
 */
public class LoginUserDetails implements UserDetails {

    private final LoginUser loginUser;

    @Nullable
    private final String password;

    private final boolean enabled;

    public LoginUserDetails(LoginUser loginUser, @Nullable String password, boolean enabled) {
        this.loginUser = loginUser;
        this.password = password;
        this.enabled = enabled;
    }

    public LoginUser loginUser() {
        return loginUser;
    }

    public long userId() {
        return loginUser.id();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return loginUser.authorities();
    }

    @Override
    @Nullable
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return loginUser.username();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
