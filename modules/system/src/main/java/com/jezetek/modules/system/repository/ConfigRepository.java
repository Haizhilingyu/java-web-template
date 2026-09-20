package com.jezetek.modules.system.repository;

import com.jezetek.modules.system.model.Config;
import com.jezetek.modules.system.model.ConfigTable;
import org.babyfish.jimmer.Specification;
import org.babyfish.jimmer.spring.repo.support.AbstractJavaRepository;
import org.babyfish.jimmer.spring.repository.support.SpringPageFactory;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ConfigRepository extends AbstractJavaRepository<Config, Long> {

    private static final ConfigTable table = ConfigTable.$;

    public ConfigRepository(JSqlClient sql) {
        super(sql);
    }

    /**
     * 超级 QBE：Specification 描述动态查询条件，Fetcher 描述动态抓取形状，
     * Pageable 描述分页排序，三者均由调用方按需组装
     */
    public Page<@NotNull Config> find(
            Pageable pageable,
            Specification<Config> specification,
            @Nullable Fetcher<Config> fetcher
    ) {
        return sql
                .createQuery(table)
                .where(specification)
                .select(table.fetch(fetcher))
                .fetchPage(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        SpringPageFactory.getInstance()
                );
    }

    /**
     * 按参数键加载配置：configKey 开放接口的数据源
     */
    public Optional<Config> findByConfigKey(String configKey, @Nullable Fetcher<Config> fetcher) {
        return sql
                .createQuery(table)
                .where(table.configKey().eq(configKey))
                .select(table.fetch(fetcher))
                .fetchOptional();
    }
}
