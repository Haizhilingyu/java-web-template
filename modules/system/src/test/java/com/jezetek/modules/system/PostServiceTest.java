package com.jezetek.modules.system;

import com.jezetek.core.runtime.BusinessException;
import com.jezetek.modules.system.model.Post;
import com.jezetek.modules.system.model.User;
import com.jezetek.modules.system.model.UserDraft;
import com.jezetek.modules.system.repository.UserRepository;
import com.jezetek.modules.system.service.PostService;
import com.jezetek.modules.system.service.dto.PostInput;
import com.jezetek.modules.system.service.dto.PostSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void login() {
        TestLogin.loginAs(TestLogin.ADMIN);
    }

    private PostInput input(String code, String name, int sortOrder) {
        PostInput input = new PostInput();
        input.setCode(code);
        input.setName(name);
        input.setSortOrder(sortOrder);
        input.setEnabled(true);
        return input;
    }

    @Test
    void 分页查询返回全部岗位() {
        Page<Post> page = postService.findPostsBySuperQBE(0, 10, "sortOrder asc", new PostSpecification());

        assertEquals(4, page.getTotalElements());
        assertEquals("董事长", page.getContent().get(0).name());
        assertEquals("普通员工", page.getContent().get(3).name());
    }

    @Test
    void 按关键字模糊查询() {
        PostSpecification spec = new PostSpecification();
        spec.setKeyword("项目");

        Page<Post> page = postService.findPostsBySuperQBE(0, 10, "sortOrder asc", spec);

        assertEquals(1, page.getTotalElements());
        assertEquals("PM", page.getContent().get(0).code());
    }

    @Test
    void 新增岗位() {
        Post saved = postService.savePost(input("TEST", "测试岗位", 9));

        assertTrue(saved.id() > 0);
        assertEquals("TEST", saved.code());
        assertNotNull(saved.createdTime());
    }

    @Test
    void 更新岗位() {
        PostInput input = input("PM", "项目经理", 5);
        input.setId(2L);

        Post saved = postService.savePost(input);

        assertEquals(2L, saved.id());
        assertEquals(5, saved.sortOrder());
    }

    @Test
    void 岗位被用户绑定禁删() {
        Post bound = postService.savePost(input("BOUND", "被绑定岗位", 8));
        // admin(1) 绑定该岗位
        User draft = UserDraft.$.produce(d -> {
            d.setId(1L);
            d.setUsername("admin");
            d.setPosts(List.of(bound));
        });
        userRepository.save(draft);

        BusinessException e = assertThrows(BusinessException.class, () -> postService.deletePost(bound.id()));
        assertTrue(e.getMessage().contains("绑定"));
    }

    @Test
    void 删除未绑定岗位成功() {
        Post saved = postService.savePost(input("TEMP", "临时岗位", 9));

        postService.deletePost(saved.id());

        assertNull(postService.findPost(saved.id()));
    }
}
