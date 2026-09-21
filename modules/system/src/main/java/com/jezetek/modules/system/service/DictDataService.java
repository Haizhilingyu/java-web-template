package com.jezetek.modules.system.service;

import com.jezetek.core.runtime.log.Log;
import com.jezetek.modules.system.model.DictData;
import com.jezetek.modules.system.model.Fetchers;
import com.jezetek.modules.system.repository.DictDataRepository;
import com.jezetek.modules.system.service.dto.DictDataInput;
import com.jezetek.modules.system.service.dto.DictDataSpecification;
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

import java.util.List;

/*
 * 参照 jimmer-sql 示例的做法：富客户端时代 Controller 层意义弱化，
 * 直接把 web 注解放在 service 上，避免模板代码过度分层
 */
@RestController
@RequestMapping("/api/v1/dict/data")
@Transactional
public class DictDataService implements Fetchers {

    private final DictDataRepository dictDataRepository;

    public DictDataService(DictDataRepository dictDataRepository) {
        this.dictDataRepository = dictDataRepository;
    }

    @PreAuthorize("@perm.has('system:dict:list')")
    @GetMapping("/list/bySuperQBE")
    public Page<@NotNull @FetchBy("DEFAULT_FETCHER") DictData> findDictDataBySuperQBE(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "5") int pageSize,
            @RequestParam(defaultValue = "sortOrder asc") String sortCode,
            DictDataSpecification specification,
            // 明细页按主表选中的字典类型过滤
            @RequestParam(required = false) Long dictTypeId
    ) {
        return dictDataRepository.find(
                PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
                specification,
                dictTypeId,
                DEFAULT_FETCHER
        );
    }

    /**
     * 按字典编码取启用条目：前端 useDict 的数据源。
     * 管理端各表单的选择器也要消费，任何登录用户可见(与菜单/部门树一致)；
     * 后端不加缓存——单机 H2 无收益，前端 useDict 已缓存
     */
    @GetMapping("/type/{type}")
    public List<@FetchBy("ITEM_FETCHER") DictData> findEnabledByType(@PathVariable("type") String type) {
        return dictDataRepository.findEnabledByType(type);
    }

    @Log(module = "字典管理", action = "保存字典条目")
    @PreAuthorize("@perm.hasAny('system:dict:add', 'system:dict:edit')")
    @PutMapping
    public @FetchBy("DEFAULT_FETCHER") DictData saveDictData(
            @Valid @RequestBody DictDataInput input
    ) {
        return dictDataRepository
                .saveCommand(input)
                .execute(DEFAULT_FETCHER)
                .getModifiedEntity();
    }

    @Log(module = "字典管理", action = "删除字典条目")
    @PreAuthorize("@perm.has('system:dict:delete')")
    @DeleteMapping("/{id}")
    public void deleteDictData(@PathVariable("id") long id) {
        dictDataRepository.deleteById(id);
    }

    /**
     * 默认抓取形状：全部标量属性(不含字典类型)
     */
    private static final Fetcher<DictData> DEFAULT_FETCHER =
            DICT_DATA_FETCHER
                    .allScalarFields();

    /**
     * 下发条目的抓取形状：全部标量属性
     */
    private static final Fetcher<DictData> ITEM_FETCHER =
            DICT_DATA_FETCHER
                    .allScalarFields();
}
