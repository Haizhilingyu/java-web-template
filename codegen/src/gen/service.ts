import type { EntityDef, OneToManyProp, Schema } from '../schema.js';
import { idViewName } from './entity.js';
import { capitalize, isTree, javaType, owningManyToMany, singleScalarKey } from '../support.js';

/** 生成 <Entity>.dto：Input(含校验注解原文) + Specification(超级 QBE 动态条件) */
export function genDto(schema: Schema, entity: EntityDef): string {
    const targetPkg = `${schema.project.javaPackage}.service.dto`;

    const inputProps: string[] = ['    id\n'];
    for (const p of entity.props) {
        if (p.kind === 'scalar' && !p.key) {
            // 业务键属性与非键属性都进 Input；BaseEntity/TenantAware 的字段由基类提供，不在此列
        }
        if (p.kind === 'scalar') {
            inputProps.push(scalarInputBlock(p));
        } else if (p.kind === 'manyToOne') {
            inputProps.push(`    id(${p.name}) as ${p.name}Id\n`);
        } else if (p.kind === 'manyToMany' && p.joinTable) {
            inputProps.push(`    id(${p.name}) as ${idViewName(p.name)}\n`);
        }
    }

    const search = entity.search;
    const specBody: string[] = [];
    if (search?.keyword?.length) {
        specBody.push(`    like/i(${search.keyword.join(', ')}) as keyword`);
    }
    for (const eq of search?.eq ?? []) {
        specBody.push(`    eq(${eq})`);
    }
    for (const flat of search?.flatLike ?? []) {
        specBody.push(`    flat(${flat.by}) {
        like/i(${flat.prop}) as ${flat.as}
    }`);
    }
    const spec = specBody.length
        ? `\nspecification ${entity.name}Specification {
${specBody.join('\n')}
}
`
        : '';

    return `export ${schema.project.javaPackage}.model.${entity.name}
    -> package ${targetPkg}

input ${entity.name}Input {

${inputProps.join('\n')}
}${spec}`;
}

function scalarInputBlock(p: { name: string; validation?: string[] }): string {
    const annotations = (p.validation ?? [])
        .map(a => `    ${a}`)
        .join('\n');
    return `${annotations ? annotations + '\n' : ''}    ${p.name}
`;
}

/** 生成 <Entity>Service.java */
export function genService(schema: Schema, entity: EntityDef): string {
    const pkg = schema.project.javaPackage;
    const lower = entity.name.toLowerCase();
    const sortCode = entity.defaultSort ?? 'id asc';
    const key = singleScalarKey(entity);
    const tree = isTree(schema, entity);
    const selfRef = entity.props.find(p => p.kind === 'manyToOne' && p.target === entity.name);
    const childrenProp = entity.props.find((p): p is OneToManyProp => p.kind === 'oneToMany')?.name ?? 'children';

    // fetcher 链上的多对多关联(当前实体的 DEFAULT_FETCHER 要抓取的部分)
    const m2mFetcher = owningManyToMany(entity)
        .map(p => {
            const target = schema.entities.find(e => e.name === p.target)!;
            const tenantExclude = target.tenantAware ? `\n                                    .tenant(false)` : '';
            return `
                    .${p.name}(
                            ${p.target.toUpperCase()}_FETCHER
                                    .allScalarFields()${tenantExclude}
                    )`;
        })
        .join('');

    const treeEndpoint = tree && selfRef
        ? `
    /**
     * 树形列表：只查根节点，子树由递归 fetcher 抓取
     */
    @GetMapping("/list")
    public List<@FetchBy("TREE_FETCHER") ${entity.name}> find${entity.name}Tree() {
        return ${lower}Repository.findRoots(TREE_FETCHER);
    }
`
        : '';

    const treeFetcher = tree
        ? `
    /**
     * 树形抓取形状：全部标量属性 + 递归的子节点
     */
    private static final Fetcher<${entity.name}> TREE_FETCHER =
            ${entity.name.toUpperCase()}_FETCHER
                    .allScalarFields()
                    .recursive${capitalize(childrenProp)}();
`
        : '';

    const keyEndpoint = key
        ? `
    @GetMapping("/${key.name}/{${key.name}}")
    @Nullable
    public @FetchBy("DEFAULT_FETCHER") ${entity.name} find${entity.name}By${capitalize(key.name)}(
            @PathVariable("${key.name}") ${javaType(key.type, !!key.nullable)} ${key.name}
    ) {
        return ${lower}Repository
                .findBy${capitalize(key.name)}(${key.name}, DEFAULT_FETCHER)
                .orElse(null);
    }
`
        : '';

    const qbeEndpoint = entity.search
        ? `
    @GetMapping("/list/bySuperQBE")
    public Page<@FetchBy("DEFAULT_FETCHER") ${entity.name}> find${entity.name}sBySuperQBE(
            @RequestParam(defaultValue = "0") int pageIndex,
            @RequestParam(defaultValue = "5") int pageSize,
            @RequestParam(defaultValue = "${sortCode}") String sortCode,
            ${entity.name}Specification specification
    ) {
        return ${lower}Repository.find(
                PageRequest.of(pageIndex, pageSize, SortUtils.toSort(sortCode)),
                specification,
                DEFAULT_FETCHER
        );
    }
` : '';

    return `package ${pkg}.service;

import ${pkg}.model.Fetchers;
import ${pkg}.model.${entity.name};
import ${pkg}.repository.${entity.name}Repository;
import ${pkg}.service.dto.${entity.name}Input;${entity.search ? `
import ${pkg}.service.dto.${entity.name}Specification;` : '' }
import org.babyfish.jimmer.client.FetchBy;
import org.babyfish.jimmer.spring.model.SortUtils;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/${lower}")
@Transactional
public class ${entity.name}Service implements Fetchers {

    private final ${entity.name}Repository ${lower}Repository;

    public ${entity.name}Service(${entity.name}Repository ${lower}Repository) {
        this.${lower}Repository = ${lower}Repository;
    }
${treeEndpoint}${qbeEndpoint}
    @GetMapping("/{id}")
    @Nullable
    public @FetchBy("DEFAULT_FETCHER") ${entity.name} find${entity.name}(
            @PathVariable("id") long id
    ) {
        return ${lower}Repository.findById(id, DEFAULT_FETCHER);
    }
${keyEndpoint}
    @PutMapping
    public @FetchBy("DEFAULT_FETCHER") ${entity.name} save${entity.name}(
            @Valid @RequestBody ${entity.name}Input input
    ) {
        return ${lower}Repository
                .saveCommand(input)
                .execute(DEFAULT_FETCHER)
                .getModifiedEntity();
    }

    @DeleteMapping("/{id}")
    public void delete${entity.name}(@PathVariable("id") long id) {
        ${lower}Repository.deleteById(id);
    }
${treeFetcher}
    /**
     * 默认抓取形状：全部标量属性${entity.tenantAware ? '(不含 tenant)' : ''}${owningManyToMany(entity).length ? ' + 关联对象标量属性' : ''}
     */
    private static final Fetcher<${entity.name}> DEFAULT_FETCHER =
            ${entity.name.toUpperCase()}_FETCHER
                    .allScalarFields()${entity.tenantAware ? `\n                    .tenant(false)` : ''}${m2mFetcher};
}
`;
}

export function genDtoGeneration(pkg: string): string {
    return `package ${pkg}.service;

import org.babyfish.jimmer.sql.EnableDtoGeneration;

/**
 * 实体接口定义在 model 模块，本模块没有 @Entity 类型，
 * jimmer-apt 需要该注解手动启用 src/main/dto 下 DTO 文件的编译
 */
@EnableDtoGeneration
public interface DtoGeneration {
}
`;
}
