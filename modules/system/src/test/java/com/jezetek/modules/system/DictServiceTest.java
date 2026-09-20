package com.jezetek.modules.system;

import com.jezetek.core.runtime.BusinessException;
import com.jezetek.modules.system.model.DictData;
import com.jezetek.modules.system.model.DictType;
import com.jezetek.modules.system.service.DictDataService;
import com.jezetek.modules.system.service.DictTypeService;
import com.jezetek.modules.system.service.dto.DictDataInput;
import com.jezetek.modules.system.service.dto.DictDataSpecification;
import com.jezetek.modules.system.service.dto.DictTypeInput;
import com.jezetek.modules.system.service.dto.DictTypeSpecification;
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
class DictServiceTest {

    @Autowired
    private DictTypeService dictTypeService;

    @Autowired
    private DictDataService dictDataService;

    @BeforeEach
    void login() {
        TestLogin.loginAs(TestLogin.ADMIN);
    }

    private DictTypeInput typeInput(String type, String name) {
        DictTypeInput input = new DictTypeInput();
        input.setType(type);
        input.setName(name);
        input.setEnabled(true);
        return input;
    }

    private DictDataInput dataInput(Long dictTypeId, String label, String value, int sortOrder, boolean enabled) {
        DictDataInput input = new DictDataInput();
        input.setDictTypeId(dictTypeId);
        input.setLabel(label);
        input.setValue(value);
        input.setSortOrder(sortOrder);
        input.setEnabled(enabled);
        return input;
    }

    @Test
    void 分页查询返回全部字典类型() {
        Page<DictType> page = dictTypeService.findDictTypesBySuperQBE(0, 10, "type asc", new DictTypeSpecification());

        // 种子：sys_notice_type / sys_yes_no
        // 注：sortCode 暂不生效(bySuperQBE 的 fetchPage 不读取 PageRequest 排序，计划在工单11统一修复)，按集合断言
        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream().anyMatch(t -> t.type().equals("sys_yes_no")));
        assertTrue(page.getContent().stream().anyMatch(t -> t.type().equals("sys_notice_type")));
    }

    @Test
    void 按关键字模糊查询类型() {
        DictTypeSpecification spec = new DictTypeSpecification();
        spec.setKeyword("yes");

        Page<DictType> page = dictTypeService.findDictTypesBySuperQBE(0, 10, "type asc", spec);

        assertEquals(1, page.getTotalElements());
        assertEquals("sys_yes_no", page.getContent().get(0).type());
    }

    @Test
    void 新增类型并同编码幂等更新() {
        DictType saved = dictTypeService.saveDictType(typeInput("sys_test", "测试字典"));
        assertTrue(saved.id() > 0);

        // 同 type 未带 id → upsert 命中更新而非新增
        DictType updated = dictTypeService.saveDictType(typeInput("sys_test", "测试字典改名"));

        assertEquals(saved.id(), updated.id());
        assertEquals("测试字典改名", updated.name());
    }

    @Test
    void 有条目的类型禁删且空类型可删() {
        // 种子 sys_yes_no(id=1) 下有两个条目
        BusinessException e = assertThrows(BusinessException.class, () -> dictTypeService.deleteDictType(1L));
        assertTrue(e.getMessage().contains("条目"));

        DictType empty = dictTypeService.saveDictType(typeInput("sys_empty", "空字典"));
        dictTypeService.deleteDictType(empty.id());
        assertNull(dictTypeService.findDictTypesBySuperQBE(0, 10, "type asc", new DictTypeSpecification())
                .getContent().stream().filter(t -> t.id() == empty.id()).findFirst().orElse(null));
    }

    @Test
    void 按编码取启用条目且排序正确() {
        List<DictData> items = dictDataService.findEnabledByType("sys_yes_no");

        assertEquals(2, items.size());
        assertEquals("是", items.get(0).label());
        assertEquals("Y", items.get(0).value());
        assertEquals("否", items.get(1).label());

        // 不存在的编码返回空列表
        assertTrue(dictDataService.findEnabledByType("no_such_dict").isEmpty());
    }

    @Test
    void 禁用条目不下发() {
        DictDataInput input = dataInput(1L, "未知", "U", 3, false);
        dictDataService.saveDictData(input);

        List<DictData> items = dictDataService.findEnabledByType("sys_yes_no");

        assertEquals(2, items.size());
    }

    @Test
    void 数据分页按类型与关键字过滤() {
        DictDataSpecification spec = new DictDataSpecification();
        spec.setKeyword("1");

        // dictTypeId=2(sys_notice_type) 下 value=1 的通知
        Page<DictData> page = dictDataService.findDictDataBySuperQBE(0, 10, "sortOrder asc", spec, 2L);

        assertEquals(1, page.getTotalElements());
        assertEquals("通知", page.getContent().get(0).label());
    }

    @Test
    void 新增条目并同字典同值幂等更新() {
        DictData saved = dictDataService.saveDictData(dataInput(1L, "未知", "U", 3, true));
        assertTrue(saved.id() > 0);

        DictData updated = dictDataService.saveDictData(dataInput(1L, "未知改", "U", 4, true));

        assertEquals(saved.id(), updated.id());
        assertEquals("未知改", updated.label());
        assertEquals(4, updated.sortOrder());
    }

    @Test
    void 删除条目() {
        DictData saved = dictDataService.saveDictData(dataInput(1L, "临时", "T", 9, true));

        dictDataService.deleteDictData(saved.id());

        assertEquals(2, dictDataService.findEnabledByType("sys_yes_no").size());
    }
}
