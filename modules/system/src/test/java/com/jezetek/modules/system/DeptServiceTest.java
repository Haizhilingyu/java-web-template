package com.jezetek.modules.system;

import com.jezetek.core.runtime.BusinessException;
import com.jezetek.modules.system.model.Dept;
import com.jezetek.modules.system.service.DeptService;
import com.jezetek.modules.system.service.dto.DeptInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DeptServiceTest {

    @Autowired
    private DeptService deptService;

    @BeforeEach
    void login() {
        TestLogin.loginAs(TestLogin.ADMIN);
    }

    private DeptInput input(String name, Long parentId, int sortOrder) {
        DeptInput input = new DeptInput();
        input.setName(name);
        input.setParentId(parentId);
        input.setSortOrder(sortOrder);
        input.setEnabled(true);
        return input;
    }

    @Test
    void 树查询返回种子部门树() {
        List<Dept> tree = deptService.findDepts();

        assertEquals(1, tree.size());
        Dept root = tree.get(0);
        assertEquals("总公司", root.name());
        assertEquals(3, root.children().size());
        // 子部门按 sortOrder 升序
        assertEquals("研发部", root.children().get(0).name());
        assertEquals("市场部", root.children().get(1).name());
        assertEquals("财务部", root.children().get(2).name());
    }

    @Test
    void 新增根部门与子部门() {
        Dept root = deptService.saveDept(input("分公司", null, 2));
        Dept child = deptService.saveDept(input("销售部", root.id(), 1));

        assertTrue(root.id() > 0);
        assertTrue(child.id() > 0);
        assertNotNull(child.createdTime());

        List<Dept> tree = deptService.findDepts();
        assertEquals(2, tree.size());
        Dept savedRoot = tree.stream().filter(d -> d.name().equals("分公司")).findFirst().orElseThrow();
        assertEquals(1, savedRoot.children().size());
        assertEquals("销售部", savedRoot.children().get(0).name());
    }

    @Test
    void 更新部门属性() {
        DeptInput input = input("研发部", 1L, 9);
        input.setId(2L);
        input.setLeader("新负责人");
        input.setPhone("13800000000");

        Dept saved = deptService.saveDept(input);

        assertEquals(2L, saved.id());
        assertEquals("新负责人", saved.leader());
        assertEquals("13800000000", saved.phone());
        assertEquals(9, saved.sortOrder());
    }

    @Test
    void 同父同名未带id时按业务键幂等更新() {
        // 不带 id，但 (parent=总公司, name=研发部) 已存在 → upsert 命中更新而非新增
        Dept saved = deptService.saveDept(input("研发部", 1L, 8));

        assertEquals(2L, saved.id());
        assertEquals(8, saved.sortOrder());
        assertEquals(3, deptService.findDepts().get(0).children().size());
    }

    @Test
    void 有子部门禁删() {
        BusinessException e = assertThrows(BusinessException.class, () -> deptService.deleteDept(1L));
        assertTrue(e.getMessage().contains("子部门"));
    }

    @Test
    void 部门下有用户禁删() {
        // 种子数据 demo/frozen 挂在研发部(2)
        BusinessException e = assertThrows(BusinessException.class, () -> deptService.deleteDept(2L));
        assertTrue(e.getMessage().contains("用户"));
    }

    @Test
    void 删除叶子空部门成功() {
        Dept leaf = deptService.saveDept(input("临时部门", 1L, 9));

        deptService.deleteDept(leaf.id());

        assertNull(deptService.findDept(leaf.id()));
    }
}
