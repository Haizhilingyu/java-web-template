import type { EntityDef, ManyToOneProp, OneToManyProp, Schema } from '../schema.js';
import { capitalize, javaType, singleScalarKey, isTree } from '../support.js';

export function genRepository(schema: Schema, entity: EntityDef): string {
    const pkg = schema.project.javaPackage;
    const key = singleScalarKey(entity);
    const tree = isTree(schema, entity);
    const selfRef = entity.props.find((p): p is ManyToOneProp => p.kind === 'manyToOne' && p.target === entity.name);
    const childrenProp = treeChildrenName(entity);

    const treeMethod = tree && selfRef
        ? `
    /**
     * 加载全部根节点，子树用递归 fetcher 抓取，例如：
     * {@code ${entity.name}Fetcher.$.allScalarFields().recursive${capitalize(childrenProp)}()}
     */
    public java.util.List<${entity.name}> findRoots(
            org.jetbrains.annotations.Nullable org.babyfish.jimmer.sql.fetcher.Fetcher<${entity.name}> fetcher
    ) {
        return sql
                .createQuery(table)
                .where(table.${selfRef.name}Id().isNull())
                .orderBy(table.id().asc())
                .select(table.fetch(fetcher))
                .execute();
    }
`
        : '';

    const keyMethod = key
        ? `
    /**
     * 按业务键 ${key.name} 加载，可用 fetcher 一并抓取关联
     */
    public java.util.Optional<${entity.name}> findBy${capitalize(key.name)}(
            ${javaType(key.type, !!key.nullable)} ${key.name},
            org.jetbrains.annotations.Nullable org.babyfish.jimmer.sql.fetcher.Fetcher<${entity.name}> fetcher
    ) {
        return sql
                .createQuery(table)
                .where(table.${key.name}().eq(${key.name}))
                .select(table.fetch(fetcher))
                .fetchOptional();
    }
`
        : '';

    return `package ${pkg}.repository;

import ${pkg}.model.${entity.name};
import ${pkg}.model.${entity.name}Table;
import org.babyfish.jimmer.Specification;
import org.babyfish.jimmer.spring.repo.support.AbstractJavaRepository;
import org.babyfish.jimmer.spring.repository.support.SpringPageFactory;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class ${entity.name}Repository extends AbstractJavaRepository<${entity.name}, Long> {

    private static final ${entity.name}Table table = ${entity.name}Table.$;

    public ${entity.name}Repository(JSqlClient sql) {
        super(sql);
    }

    /**
     * 超级 QBE：Specification 描述动态查询条件，Fetcher 描述动态抓取形状，
     * Pageable 描述分页排序，三者均由调用方按需组装
     */
    public Page<${entity.name}> find(
            Pageable pageable,
            Specification<${entity.name}> specification,
            @Nullable Fetcher<${entity.name}> fetcher
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
${keyMethod}${treeMethod}}
`;
}

function treeChildrenName(entity: EntityDef): string {
    const otm = entity.props.find((p): p is OneToManyProp => p.kind === 'oneToMany');
    return otm?.name ?? 'children';
}
