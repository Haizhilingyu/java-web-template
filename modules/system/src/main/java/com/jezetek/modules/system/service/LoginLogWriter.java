package com.jezetek.modules.system.service;

import com.jezetek.modules.system.model.Logininfor;
import com.jezetek.modules.system.model.LogininforDraft;
import com.jezetek.modules.system.repository.LogininforRepository;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Component;

/**
 * 登录日志写入器：登录成功/失败/登出时由 AuthController 调用。
 *
 * <p>独立于 LogininforService(@RestController)：@EnableImplicitApi 会把
 * RestController 的 public 方法当 API 元数据处理，无映射的写入口会令
 * jimmer-apt 编译期 NPE，故写入口放在非 Controller 组件上</p>
 */
@Component
public class LoginLogWriter {

    private final LogininforRepository logininforRepository;

    public LoginLogWriter(LogininforRepository logininforRepository) {
        this.logininforRepository = logininforRepository;
    }

    public void append(String username, String ip, String message) {
        Logininfor log = LogininforDraft.$.produce(draft -> {
            draft.setUsername(username);
            draft.setIp(ip == null || ip.isBlank() ? "unknown" : ip);
            draft.setMessage(message);
        });
        // 日志只追加不更新；实体无业务键，显式 INSERT_ONLY
        logininforRepository
                .saveCommand(log)
                .setMode(SaveMode.INSERT_ONLY)
                .execute();
    }
}
