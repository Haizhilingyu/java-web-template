package com.jezetek.modules.system.security;

import com.jezetek.core.runtime.security.SessionRegistry;
import com.jezetek.modules.system.model.User;
import com.jezetek.modules.system.model.UserDraft;
import com.jezetek.modules.system.repository.UserRepository;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Component;

/**
 * 密码变更的统一通道(工单05)：更新密文并作废该用户全部会话。
 *
 * <p>个人中心改密(AuthController.changePassword)与管理员重置密码
 * (UserService.resetPassword)共用，保证"密码一变、全端下线"(ADR-0001)；
 * 手工组 draft 控制加载态——未提交属性(昵称/角色等)不得被写 null</p>
 */
@Component
public class PasswordManager {

    private final UserRepository userRepository;

    private final SessionRegistry sessionRegistry;

    public PasswordManager(UserRepository userRepository, SessionRegistry sessionRegistry) {
        this.userRepository = userRepository;
        this.sessionRegistry = sessionRegistry;
    }

    /** 更新密码(密文)并作废该用户全部会话，返回作废条数 */
    public int updatePasswordAndRevokeSessions(long userId, String encodedPassword) {
        User entity = UserDraft.$.produce(draft -> {
            draft.setId(userId);
            draft.setPassword(encodedPassword);
        });
        userRepository.saveCommand(entity)
                .setMode(SaveMode.NON_IDEMPOTENT_UPSERT)
                .execute();
        return sessionRegistry.removeByUser(userId);
    }
}
