package com.jezetek.modules.system.service;

import com.jezetek.core.runtime.log.Log;
import com.jezetek.modules.system.model.Config;
import com.jezetek.modules.system.model.Fetchers;
import com.jezetek.modules.system.repository.ConfigRepository;
import com.jezetek.modules.system.service.dto.ConfigInput;
import com.jezetek.modules.system.service.dto.ConfigSpecification;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/*
 * 参照 jimmer-sql 示例的做法：富客户端时代 Controller 层意义弱化，
 * 直接把 web 注解放在 service 上，避免模板代码过度分层
 */
@RestController
@RequestMapping("/api/v1/config")
@Transactional
public class ConfigService implements Fetchers {

    private final ConfigRepository configRepository;

    public ConfigService(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @PreAuthorize("@perm.has('system:config:list')")
    @GetMapping("/list/bySuperQBE")
    public Page<@NotNull @FetchBy("DEFAULT_FETCHER") Config> findConfigsBySuperQBE(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "5") int pageSize,
            @RequestParam(defaultValue = "configKey asc") String sortCode,
            ConfigSpecification specification
    ) {
        return configRepository.find(
                PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
                specification,
                DEFAULT_FETCHER
        );
    }

    /**
     * 按参数键取配置：模板内置消费点尚未出现(决策：不种无人读取的键)，
     * 登录即可访问，供将来前端/业务读取
     */
    @GetMapping("/configKey/{key}")
    @Nullable
    public @FetchBy("DEFAULT_FETCHER") Config findConfigByKey(@PathVariable("key") String key) {
        return configRepository.findByConfigKey(key, DEFAULT_FETCHER).orElse(null);
    }

    @Log(module = "参数配置", action = "保存参数")
    @PreAuthorize("@perm.hasAny('system:config:add', 'system:config:edit')")
    @PutMapping
    public @FetchBy("DEFAULT_FETCHER") Config saveConfig(
            @Valid @RequestBody ConfigInput input
    ) {
        return configRepository
                .saveCommand(input)
                .execute(DEFAULT_FETCHER)
                .getModifiedEntity();
    }

    @Log(module = "参数配置", action = "删除参数")
    @PreAuthorize("@perm.has('system:config:delete')")
    @DeleteMapping("/{id}")
    public void deleteConfig(@PathVariable("id") long id) {
        configRepository.deleteById(id);
    }

    /**
     * 默认抓取形状：全部标量属性
     */
    private static final Fetcher<Config> DEFAULT_FETCHER =
            CONFIG_FETCHER
                    .allScalarFields();
}
