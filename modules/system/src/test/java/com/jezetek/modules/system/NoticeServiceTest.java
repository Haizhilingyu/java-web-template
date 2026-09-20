package com.jezetek.modules.system;

import com.jezetek.modules.system.model.Notice;
import com.jezetek.modules.system.service.NoticeService;
import com.jezetek.modules.system.service.dto.NoticeInput;
import com.jezetek.modules.system.service.dto.NoticeSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class NoticeServiceTest {

    @Autowired
    private NoticeService noticeService;

    @BeforeEach
    void login() {
        TestLogin.loginAs(TestLogin.ADMIN);
    }

    private NoticeInput input(String title, String type, String content, boolean enabled) {
        NoticeInput input = new NoticeInput();
        input.setNoticeTitle(title);
        input.setNoticeType(type);
        input.setContent(content);
        input.setEnabled(enabled);
        return input;
    }

    @Test
    void 新增公告并回查() {
        Notice saved = noticeService.saveNotice(input("维护通知", "1", "本周六凌晨停机维护", true));

        assertTrue(saved.id() > 0);
        assertEquals("维护通知", saved.noticeTitle());
        assertEquals("1", saved.noticeType());
        assertNotNull(saved.createdTime());
    }

    @Test
    void 按类型过滤与关键字查询() {
        noticeService.saveNotice(input("维护通知", "1", "停机维护", true));
        noticeService.saveNotice(input("版本上线", "2", "新版本上线", true));

        NoticeSpecification byType = new NoticeSpecification();
        byType.setNoticeType("2");
        Page<Notice> notices = noticeService.findNoticesBySuperQBE(0, 10, "id asc", byType);
        assertEquals(1, notices.getTotalElements());
        assertEquals("版本上线", notices.getContent().get(0).noticeTitle());

        NoticeSpecification byKeyword = new NoticeSpecification();
        byKeyword.setKeyword("维护");
        Page<Notice> hit = noticeService.findNoticesBySuperQBE(0, 10, "id asc", byKeyword);
        assertEquals(1, hit.getTotalElements());
    }

    @Test
    void 更新公告内容() {
        Notice saved = noticeService.saveNotice(input("维护通知", "1", "停机维护", true));

        NoticeInput update = input("维护通知", "1", "改期至下周", true);
        update.setId(saved.id());
        Notice updated = noticeService.saveNotice(update);

        assertEquals(saved.id(), updated.id());
        assertEquals("改期至下周", updated.content());
    }

    @Test
    void 删除公告() {
        Notice saved = noticeService.saveNotice(input("临时公告", "2", "临时", true));

        noticeService.deleteNotice(saved.id());

        Page<Notice> page = noticeService.findNoticesBySuperQBE(0, 10, "id asc", new NoticeSpecification());
        assertTrue(page.getContent().stream().noneMatch(n -> n.id() == saved.id()));
    }
}
