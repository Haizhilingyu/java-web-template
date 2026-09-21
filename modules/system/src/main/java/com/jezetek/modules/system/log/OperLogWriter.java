package com.jezetek.modules.system.log;

import com.jezetek.modules.system.model.OperLog;
import com.jezetek.modules.system.model.OperLogDraft;
import com.jezetek.modules.system.repository.OperLogRepository;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Component;

/**
 * 操作日志落库器：由切面在后台线程调用(实体已在调用线程组装完毕)。
 * 日志只追加不更新，实体无业务键，显式 INSERT_ONLY
 */
@Component
public class OperLogWriter {

    private final OperLogRepository operLogRepository;

    public OperLogWriter(OperLogRepository operLogRepository) {
        this.operLogRepository = operLogRepository;
    }

    public void write(OperLog log) {
        operLogRepository
                .saveCommand(log)
                .setMode(SaveMode.INSERT_ONLY)
                .execute();
    }
}
