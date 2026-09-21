package com.jezetek.modules.system.service;

import com.jezetek.core.runtime.log.Log;
import com.jezetek.core.runtime.BusinessException;
import com.jezetek.modules.system.model.DictType;
import com.jezetek.modules.system.model.Fetchers;
import com.jezetek.modules.system.repository.DictDataRepository;
import com.jezetek.modules.system.repository.DictTypeRepository;
import com.jezetek.modules.system.service.dto.DictTypeInput;
import com.jezetek.modules.system.service.dto.DictTypeSpecification;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/*
 * 参照 jimmer-sql 示例的做法：富客户端时代 Controller 层意义弱化，
 * 直接把 web 注解放在 service 上，避免模板代码过度分层
 */
@RestController
@RequestMapping("/api/v1/dict/type")
@Transactional
public class DictTypeService implements Fetchers {

    private final DictTypeRepository dictTypeRepository;

    private final DictDataRepository dictDataRepository;

    public DictTypeService(DictTypeRepository dictTypeRepository, DictDataRepository dictDataRepository) {
        this.dictTypeRepository = dictTypeRepository;
        this.dictDataRepository = dictDataRepository;
    }

    @PreAuthorize("@perm.has('system:dict:list')")
    @GetMapping("/list/bySuperQBE")
    public Page<@NotNull @FetchBy("DEFAULT_FETCHER") DictType> findDictTypesBySuperQBE(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "5") int pageSize,
            @RequestParam(defaultValue = "type asc") String sortCode,
            DictTypeSpecification specification
    ) {
        return dictTypeRepository.find(
                PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
                specification,
                DEFAULT_FETCHER
        );
    }

    /**
     * 生命周期约束：字典下存在条目时禁止删除，防止误删整套字典
     */
    @Log(module = "字典管理", action = "删除字典类型")
    @PreAuthorize("@perm.has('system:dict:delete')")
    @DeleteMapping("/{id}")
    public void deleteDictType(@PathVariable("id") long id) {
        if (dictDataRepository.existsByDictTypeId(id)) {
            throw new BusinessException("字典下存在条目，请先删除字典条目");
        }
        dictTypeRepository.deleteById(id);
    }

    @Log(module = "字典管理", action = "保存字典类型")
    @PreAuthorize("@perm.hasAny('system:dict:add', 'system:dict:edit')")
    @PutMapping
    public @FetchBy("DEFAULT_FETCHER") DictType saveDictType(
            @Valid @RequestBody DictTypeInput input
    ) {
        return dictTypeRepository
                .saveCommand(input)
                .execute(DEFAULT_FETCHER)
                .getModifiedEntity();
    }

    /**
     * 默认抓取形状：全部标量属性
     */
    private static final Fetcher<DictType> DEFAULT_FETCHER =
            DICT_TYPE_FETCHER
                    .allScalarFields();
}
