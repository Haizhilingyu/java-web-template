package com.jezetek.modules.system;

import com.jezetek.modules.system.model.OperLog;
import com.jezetek.modules.system.model.OperLogDraft;
import com.jezetek.modules.system.model.User;
import com.jezetek.modules.system.repository.OperLogRepository;
import com.jezetek.modules.system.service.OperLogService;
import com.jezetek.modules.system.service.PostService;
import com.jezetek.modules.system.service.UserService;
import com.jezetek.modules.system.service.dto.PostInput;
import com.jezetek.modules.system.service.dto.PostSpecification;
import com.jezetek.modules.system.service.dto.UserInput;
import com.jezetek.modules.system.service.dto.UserSpecification;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 工单12：jimmer fetchPage 不读 Pageable 的 Sort，sortCode 必须由
 * Repository 显式翻译为 orderBy。乱序种子下断言分页内容顺序而非集合
 */
@SpringBootTest
@Transactional
class PaginationSortTest {

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private OperLogService operLogService;

    @Autowired
    private OperLogRepository operLogRepository;

    @BeforeEach
    void login() {
        TestLogin.loginAs(TestLogin.ADMIN);
    }

    private void saveUser(String username) {
        UserInput input = new UserInput();
        input.setUsername(username);
        input.setPassword("123456");
        input.setEnabled(true);
        userService.saveUser(input);
    }

    @Test
    void 用户分页按sortCode真实排序() {
        // 插入序刻意与字典序相反
        saveUser("zzz_sort");
        saveUser("aaa_sort");

        Page<User> asc = userService.findUsersBySuperQBE(0, 20, "username asc", new UserSpecification(), null);
        assertEquals("aaa_sort", asc.getContent().get(0).username());
        assertEquals("admin", asc.getContent().get(1).username());
        assertEquals("demo", asc.getContent().get(2).username());
        assertEquals("frozen", asc.getContent().get(3).username());
        assertEquals("zzz_sort", asc.getContent().get(4).username());

        Page<User> desc = userService.findUsersBySuperQBE(0, 20, "username desc", new UserSpecification(), null);
        assertEquals("zzz_sort", desc.getContent().get(0).username());
        assertEquals("aaa_sort", desc.getContent().get(4).username());
    }

    @Test
    void 用户分页非法sortCode回退id排序() {
        saveUser("zzz_sort");

        Page<User> page = userService.findUsersBySuperQBE(0, 20, "hack_field desc", new UserSpecification(), null);
        // 白名单外属性回退 id：种子 id 1/2/3 = admin/demo/frozen，新用户在后
        assertEquals("admin", page.getContent().get(0).username());
        assertEquals("demo", page.getContent().get(1).username());
        assertEquals("frozen", page.getContent().get(2).username());
        assertEquals("zzz_sort", page.getContent().get(3).username());
    }

    @Test
    void 岗位分页按sortOrder真实排序() {
        PostInput input = new PostInput();
        input.setCode("AAAA");
        input.setName("排序测试岗");
        input.setSortOrder(0);
        input.setEnabled(true);
        postService.savePost(input);

        Page<com.jezetek.modules.system.model.Post> asc =
                postService.findPostsBySuperQBE(0, 20, "sortOrder asc", new PostSpecification());
        assertEquals("AAAA", asc.getContent().get(0).code());
        assertEquals(1, asc.getContent().get(1).sortOrder());
        assertEquals(4, asc.getContent().get(4).sortOrder());
    }

    private void saveOperLog(String operator) {
        OperLog log = OperLogDraft.$.produce(draft -> {
            draft.setModule("测试模块");
            draft.setAction("测试动作");
            draft.setOperator(operator);
            draft.setCostMs(1);
            draft.setSuccess(true);
        });
        operLogRepository.saveCommand(log).setMode(SaveMode.INSERT_ONLY).execute();
    }

    @Test
    void 操作日志分页按sortCode真实排序且默认id倒序() {
        // 插入序刻意与字典序相反
        saveOperLog("zou");
        saveOperLog("li");
        saveOperLog("wang");

        Page<OperLog> asc = operLogService.findOperLogsBySuperQBE(0, 20, "operator asc", null);
        assertEquals(3, asc.getTotalElements());
        assertEquals("li", asc.getContent().get(0).operator());
        assertEquals("wang", asc.getContent().get(1).operator());
        assertEquals("zou", asc.getContent().get(2).operator());

        Page<OperLog> desc = operLogService.findOperLogsBySuperQBE(0, 20, "id desc", null);
        assertEquals("wang", desc.getContent().get(0).operator());
        assertEquals("li", desc.getContent().get(1).operator());
        assertEquals("zou", desc.getContent().get(2).operator());
    }
}
