package com.jezetek.modules.system;

import com.jezetek.modules.system.model.Config;
import com.jezetek.modules.system.service.ConfigService;
import com.jezetek.modules.system.service.dto.ConfigInput;
import com.jezetek.modules.system.service.dto.ConfigSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ConfigServiceTest {

    @Autowired
    private ConfigService configService;

    @BeforeEach
    void login() {
        TestLogin.loginAs(TestLogin.ADMIN);
    }

    private ConfigInput input(String key, String name, String value) {
        ConfigInput input = new ConfigInput();
        input.setConfigKey(key);
        input.setConfigName(name);
        input.setConfigValue(value);
        return input;
    }

    @Test
    void 新增参数并可按键查询() {
        Config saved = configService.saveConfig(input("site.title", "站点标题", "Demo"));
        assertTrue(saved.id() > 0);
        assertNotNull(saved.createdTime());

        Config byKey = configService.findConfigByKey("site.title");
        assertNotNull(byKey);
        assertEquals("Demo", byKey.configValue());
        assertNull(configService.findConfigByKey("no.such.key"));
    }

    @Test
    void 同键未带id时按业务键幂等更新() {
        Config saved = configService.saveConfig(input("site.title", "站点标题", "Demo"));

        Config updated = configService.saveConfig(input("site.title", "站点标题", "Demo v2"));

        assertEquals(saved.id(), updated.id());
        assertEquals("Demo v2", updated.configValue());
    }

    @Test
    void 按关键字模糊查询() {
        configService.saveConfig(input("site.title", "站点标题", "Demo"));

        ConfigSpecification spec = new ConfigSpecification();
        spec.setKeyword("site");
        Page<Config> page = configService.findConfigsBySuperQBE(0, 10, "configKey asc", spec);

        assertEquals(1, page.getTotalElements());
    }

    @Test
    void 删除参数() {
        Config saved = configService.saveConfig(input("temp.key", "临时参数", "x"));

        configService.deleteConfig(saved.id());

        assertNull(configService.findConfigByKey("temp.key"));
    }
}
