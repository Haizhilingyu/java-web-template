package com.jezetek.core.service;

import com.jezetek.core.model.Fetchers;
import com.jezetek.core.model.Menu;
import com.jezetek.core.repository.MenuRepository;
import com.jezetek.core.service.dto.MenuInput;
import com.jezetek.core.service.dto.MenuSpecification;
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
 * 参照 jimmer-sql 示例的做法：富客户端时代 Controller 层意义弱化，
 * 直接把 web 注解放在 service 上，避免模板代码过度分层
 */
@RestController
@RequestMapping("/menu")
@Transactional
public class MenuService implements Fetchers {

    private final MenuRepository menuRepository;

    public MenuService(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    /**
     * 菜单树：只查根节点，子菜单由递归 fetcher 抓取
     */
    @GetMapping("/list")
    public List<@FetchBy("TREE_FETCHER") Menu> findMenus() {
        return menuRepository.findRootMenus(TREE_FETCHER);
    }

    @GetMapping("/list/bySuperQBE")
    public Page<@NotNull @FetchBy("DEFAULT_FETCHER") Menu> findMenusBySuperQBE(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "5") int pageSize,
            @RequestParam(defaultValue = "sortOrder asc") String sortCode,
            MenuSpecification specification
    ) {
        return menuRepository.find(
                PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
                specification,
                DEFAULT_FETCHER
        );
    }

    @GetMapping("/{id}")
    @Nullable
    public @FetchBy("DEFAULT_FETCHER") Menu findMenu(
            @PathVariable("id") long id
    ) {
        return menuRepository.findById(id, DEFAULT_FETCHER);
    }

    @PutMapping
    public @FetchBy("DEFAULT_FETCHER") Menu saveMenu(
            @Valid @RequestBody MenuInput input
    ) {
        return menuRepository
                .saveCommand(input)
                .execute(DEFAULT_FETCHER)
                .getModifiedEntity();
    }

    @DeleteMapping("/{id}")
    public void deleteMenu(@PathVariable("id") long id) {
        menuRepository.deleteById(id);
    }

    /**
     * 菜单树抓取形状：全部标量属性 + 递归的子菜单(子层重复同形状)。
     * 注意 children(true) 只会抓子菜单 id，递归全形状必须用 recursiveChildren()
     */
    private static final Fetcher<Menu> TREE_FETCHER =
            MENU_FETCHER
                    .allScalarFields()
                    .recursiveChildren();

    /**
     * 默认抓取形状：全部标量属性(不含子菜单)
     */
    private static final Fetcher<Menu> DEFAULT_FETCHER =
            MENU_FETCHER
                    .allScalarFields();
}
