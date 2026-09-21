/**
 * 业务模块骨架生成器：按 modules/system 与 modules/job 的实际结构，
 * 产出可被 app 的 scanBasePackages("com.jezetek") 直接装配的完整模块：
 *
 * pom.xml(jimmer-apt 执行级覆盖) + model/repository/service/ModuleProvider
 * + src/main/dto + sql/{code}-schema.sql、{code}-data.sql
 *
 * 路径与权限遵循 AGENTS.md 约定：/api/v1/{code}/** 与 {code}:{实体}:{动作}
 */
import type { EntityDef, ManyToOneProp, Schema, ScalarProp } from '../schema.js';
import { capitalize, h2Type, javaType, keyColumns, singleScalarKey, snake } from '../support.js';

export function modulePackage(schema: Schema): string {
    return `com.jezetek.modules.${schema.project.moduleCode}`;
}

/** 实体中文显示名：label 优先，缺省用实体名 */
export function entityLabel(schema: Schema, entity: EntityDef): string {
    return entity.label ?? entity.name;
}

export function genModuleFiles(schema: Schema): Record<string, string> {
    const pkg = modulePackage(schema);
    const srcMain = `src/main/java/${pkg.replace(/\./g, '/')}`;
    const files: Record<string, string> = {
        'pom.xml': genModulePom(schema),
        [`${srcMain}/service/DtoGeneration.java`]: genDtoGeneration(schema),
    };
    for (const entity of schema.entities) {
        files[`${srcMain}/model/${entity.name}.java`] = genEntity(schema, entity);
        files[`${srcMain}/repository/${entity.name}Repository.java`] = genRepository(schema, entity);
        files[`${srcMain}/service/${entity.name}Service.java`] = genService(schema, entity);
        files[`src/main/dto/${entity.name}.dto`] = genDto(schema, entity);
    }
    files[`${srcMain}/module/${capitalize(schema.project.moduleCode)}ModuleProvider.java`] = genModuleProvider(schema);
    files[`src/main/resources/sql/${schema.project.moduleCode}-schema.sql`] = genSchemaSql(schema);
    const data = genDataSql(schema);
    if (data) {
        files[`src/main/resources/sql/${schema.project.moduleCode}-data.sql`] = data;
    }
    return files;
}

// ---------------------------------------------------------------- pom

export function genModulePom(schema: Schema): string {
    const { rootProject, jimmerVersion } = schema.project;
    return `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>${rootProject.groupId}</groupId>
        <artifactId>${rootProject.artifactId}</artifactId>
        <version>${rootProject.version}</version>
    </parent>

    <groupId>${schema.project.groupId}</groupId>
    <artifactId>${schema.project.moduleCode}</artifactId>

    <properties>
        <maven.compiler.source>${schema.project.javaVersion}</maven.compiler.source>
        <maven.compiler.target>${schema.project.javaVersion}</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- 框架底座：基础实体基类 + 租户设施 + 安全契约(ModuleProvider/JWT/@perm) -->
        <dependency>
            <groupId>com.jezetek.core</groupId>
            <artifactId>runtime</artifactId>
            <version>${schema.project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.babyfish.jimmer</groupId>
            <artifactId>jimmer-spring-boot-starter</artifactId>
            <version>${jimmerVersion}</version>
        </dependency>
        <!-- Input DTO 的 Bean Validation 校验(@Valid/@NotBlank 等) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
                <executions>
                    <!-- 根 pom 在 default-compile/default-testCompile 执行级配置了 lombok-only 的
                         annotationProcessorPaths，会遮蔽插件级配置，必须在相同执行 id 上覆盖为 jimmer-apt。
                         jimmer-apt 负责编译 src/main/dto 下的 DTO 文件 -->
                    <execution>
                        <id>default-compile</id>
                        <configuration>
                            <annotationProcessorPaths>
                                <path>
                                    <groupId>org.babyfish.jimmer</groupId>
                                    <artifactId>jimmer-apt</artifactId>
                                    <version>${jimmerVersion}</version>
                                </path>
                            </annotationProcessorPaths>
                        </configuration>
                    </execution>
                    <execution>
                        <id>default-testCompile</id>
                        <configuration>
                            <annotationProcessorPaths>
                                <path>
                                    <groupId>org.babyfish.jimmer</groupId>
                                    <artifactId>jimmer-apt</artifactId>
                                    <version>${jimmerVersion}</version>
                                </path>
                            </annotationProcessorPaths>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
`;
}

// ---------------------------------------------------------------- entity

const KEY_UNIQUE = `@KeyUniqueConstraint(
        // Only for mysql
        noMoreUniqueConstraints = true,
        // Only for postgres
        isNullNotDistinct = true
)`;

function genEntity(schema: Schema, entity: EntityDef): string {
    const usesTenant = !!entity.tenantAware;
    const hasKey = entity.props.some(p => (p.kind === 'scalar' || p.kind === 'manyToOne') && p.key);
    const needNullable = entity.props.some(p => (p.kind === 'scalar' || p.kind === 'manyToOne') && p.nullable);
    const needList = entity.props.some(p => p.kind === 'oneToMany' || p.kind === 'manyToMany');

    const imports = [
        needNullable ? 'import org.jetbrains.annotations.Nullable;' : '',
        needList ? '\nimport java.util.List;' : '',
    ].filter(Boolean).join('\n');

    const baseIfaces = [entity.baseEntity !== false ? 'BaseEntity' : '', usesTenant ? 'TenantAware' : '']
        .filter(Boolean).join(', ');
    const classDoc = entity.comment ? `/**\n * ${entity.comment}\n */\n` : '';
    const body = entity.props.map(genProp).join('\n\n');

    return `package ${modulePackage(schema)}.model;

import org.babyfish.jimmer.sql.*;
import com.jezetek.core.model.common.BaseEntity;${usesTenant ? `\nimport com.jezetek.core.model.common.TenantAware;` : ''}${imports ? '\n' + imports : ''}
${classDoc}@Entity
${entity.table ? `@Table(name = "${entity.table}")\n` : ''}${hasKey ? KEY_UNIQUE + '\n' : ''}public interface ${entity.name}${baseIfaces ? ' extends ' + baseIfaces : ''} {

${body}
}
`;
}

function doc(comment?: string): string {
    return comment ? `    /**\n     * ${comment}\n     */\n` : '';
}

function genProp(p: EntityDef['props'][number]): string {
    switch (p.kind) {
        case 'id':
            return `${doc(p.comment)}    @Id\n    @GeneratedValue(strategy = GenerationType.IDENTITY)\n    long id();`;
        case 'scalar': {
            const annos = [p.key ? '    @Key' : '', p.nullable ? '    @Nullable' : '']
                .filter(Boolean).join('\n');
            return `${doc(p.comment)}${annos ? annos + '\n' : ''}    ${javaType(p.type, !!p.nullable)} ${p.name}();`;
        }
        case 'manyToOne': {
            const annos = [p.key ? '    @Key' : '', p.nullable ? '    @Nullable' : '']
                .filter(Boolean).join('\n');
            const dissociate = p.onDissociate ? `\n    @OnDissociate(DissociateAction.${p.onDissociate})` : '';
            return `${doc(p.comment)}${annos ? annos + '\n' : ''}    @ManyToOne${dissociate}\n    ${p.target} ${p.name}();`;
        }
        case 'oneToMany':
            return `${doc(p.comment)}    @OneToMany(mappedBy = "${p.mappedBy}"${p.orderedBy ? `, orderedProps = @OrderedProp("${p.orderedBy}")` : ''})\n    List<${p.target}> ${p.name}();`;
        case 'manyToMany': {
            if (p.joinTable) {
                return `${doc(p.comment)}    @ManyToMany\n    @JoinTable(\n            name = "${p.joinTable.name}",\n            joinColumnName = "${p.joinTable.joinColumnName}",\n            inverseJoinColumnName = "${p.joinTable.inverseJoinColumnName}"\n    )\n    List<${p.target}> ${p.name}();`;
            }
            return `${doc(p.comment)}    @ManyToMany(mappedBy = "${p.mappedBy}")\n    List<${p.target}> ${p.name}();`;
        }
    }
}

// ---------------------------------------------------------------- repository

function genRepository(schema: Schema, entity: EntityDef): string {
    const pkg = modulePackage(schema);
    const key = singleScalarKey(entity);
    const isTreeEntity = entity.props.some(p => p.kind === 'manyToOne' && p.target === entity.name);
    const selfRef = entity.props.find((p): p is ManyToOneProp => p.kind === 'manyToOne' && p.target === entity.name);
    const childrenProp = entity.props.find(p => p.kind === 'oneToMany')?.name ?? 'children';
    const capitalizeLocal = capitalize;

    // sortCode 属性白名单：与手写仓库同款(见 UserRepository.sortable / core PageOrders)；
    // BaseEntity 的 createdTime/modifiedTime 不在 schema props 里，这里补上
    const sortableProps = entity.props
        .filter((p): p is ScalarProp => p.kind === 'scalar' && p.name !== 'tenant')
        .map(p => p.name);
    if (entity.baseEntity !== false) {
        sortableProps.push('createdTime', 'modifiedTime');
    }

    const treeMethod = isTreeEntity && selfRef
        ? `
    /**
     * 加载全部根节点，子树用递归 fetcher 抓取，例如：
     * {@code ${entity.name}Fetcher.$.allScalarFields().recursive${capitalizeLocal(childrenProp)}()}
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

import com.jezetek.core.runtime.repository.PageOrders;
import ${pkg}.model.${entity.name};
import ${pkg}.model.${entity.name}Table;
import org.babyfish.jimmer.Specification;
import org.babyfish.jimmer.spring.repo.support.AbstractJavaRepository;
import org.babyfish.jimmer.spring.repository.support.SpringPageFactory;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.NotNull;
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
     * Pageable 描述分页排序，三者均由调用方按需组装。
     * jimmer fetchPage 不读 Pageable 的 Sort，sortCode 在此显式翻译为 orderBy
     */
    public Page<@NotNull ${entity.name}> find(
            Pageable pageable,
            Specification<${entity.name}> specification,
            @Nullable Fetcher<${entity.name}> fetcher
    ) {
        return sql
                .createQuery(table)
                .where(specification)
                .orderBy(PageOrders.translate(pageable.getSort(), ${entity.name}Repository::sortable))
                .select(table.fetch(fetcher))
                .fetchPage(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        SpringPageFactory.getInstance()
                );
    }

    /** sortCode 属性白名单：白名单外回退 id(见 PageOrders) */
    private static Expression<?> sortable(String property) {
        return switch (property) {
${sortableProps.map((p) => '            case "' + p + '" -> table.' + p + '();').join('\n')}
            default -> null;
        };
    }
${keyMethod}${treeMethod}}
`;
}

// ---------------------------------------------------------------- service + dto

function genService(schema: Schema, entity: EntityDef): string {
    const pkg = modulePackage(schema);
    const code = schema.project.moduleCode;
    const lower = entity.name.toLowerCase();
    const perm = `${code}:${lower}`;
    const sortCode = entity.defaultSort ?? 'id asc';
    const key = singleScalarKey(entity);

    const keyEndpoint = key
        ? `
    @PreAuthorize("@perm.has('${perm}:list')")
    @GetMapping("/by${capitalize(key.name)}/{${key.name}}")
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
    @PreAuthorize("@perm.has('${perm}:list')")
    @GetMapping("/list/bySuperQBE")
    public Page<@NotNull @FetchBy("DEFAULT_FETCHER") ${entity.name}> find${entity.name}sBySuperQBE(
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
`
        : '';

    return `package ${pkg}.service;

import com.jezetek.core.runtime.log.Log;
import ${pkg}.model.Fetchers;
import ${pkg}.model.${entity.name};
import ${pkg}.repository.${entity.name}Repository;
import ${pkg}.service.dto.${entity.name}Input;${entity.search ? `\nimport ${pkg}.service.dto.${entity.name}Specification;` : ''}
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
 * 参照 jimmer-sql 示例的做法：web 注解直挂 service。
 * 路径遵循 /api/v1/{code} 约定，权限点遵循 {code}:{实体}:{动作} 约定
 */
@RestController
@RequestMapping("/api/v1/${code}/${lower}")
@Transactional
public class ${entity.name}Service implements Fetchers {

    private final ${entity.name}Repository ${lower}Repository;

    public ${entity.name}Service(${entity.name}Repository ${lower}Repository) {
        this.${lower}Repository = ${lower}Repository;
    }
${qbeEndpoint}
    @Log(module = "${schema.project.moduleName}", action = "查询${entityLabel(schema, entity)}")
    @PreAuthorize("@perm.has('${perm}:list')")
    @GetMapping("/{id}")
    @Nullable
    public @FetchBy("DEFAULT_FETCHER") ${entity.name} find${entity.name}(
            @PathVariable("id") long id
    ) {
        return ${lower}Repository.findById(id, DEFAULT_FETCHER);
    }
${keyEndpoint}
    @Log(module = "${schema.project.moduleName}", action = "保存${entityLabel(schema, entity)}")
    @PreAuthorize("@perm.hasAny('${perm}:add', '${perm}:edit')")
    @PutMapping
    public @FetchBy("DEFAULT_FETCHER") ${entity.name} save${entity.name}(
            @Valid @RequestBody ${entity.name}Input input
    ) {
        return ${lower}Repository
                .saveCommand(input)
                .execute(DEFAULT_FETCHER)
                .getModifiedEntity();
    }

    @Log(module = "${schema.project.moduleName}", action = "删除${entityLabel(schema, entity)}")
    @PreAuthorize("@perm.has('${perm}:delete')")
    @DeleteMapping("/{id}")
    public void delete${entity.name}(@PathVariable("id") long id) {
        ${lower}Repository.deleteById(id);
    }

    /**
     * 默认抓取形状：全部标量属性${entity.tenantAware ? '(不含 tenant)' : ''}
     */
    private static final Fetcher<${entity.name}> DEFAULT_FETCHER =
            ${entity.name.toUpperCase()}_FETCHER
                    .allScalarFields()${entity.tenantAware ? `\n                    .tenant(false)` : ''};
}
`;
}

function genDto(schema: Schema, entity: EntityDef): string {
    const targetPkg = `${modulePackage(schema)}.service.dto`;

    const inputProps: string[] = ['    id\n'];
    for (const p of entity.props) {
        if (p.kind === 'scalar') {
            const annotations = (p.validation ?? []).map(a => `    ${a}`).join('\n');
            inputProps.push(`${annotations ? annotations + '\n' : ''}    ${p.name}\n`);
        } else if (p.kind === 'manyToOne') {
            inputProps.push(`    id(${p.name}) as ${p.name}Id\n`);
        } else if (p.kind === 'manyToMany' && p.joinTable) {
            inputProps.push(`    id(${p.name}) as ${p.name}Ids\n`);
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
        specBody.push(`    flat(${flat.by}) {\n        like/i(${flat.prop}) as ${flat.as}\n    }`);
    }
    const spec = specBody.length
        ? `\nspecification ${entity.name}Specification {\n${specBody.join('\n')}\n}\n`
        : '';

    return `/*
 * 本文件由 jimmer-apt 编译，修改后需重新构建；
 * 校验注解必须全限定名且写在属性行之前
 */

export ${modulePackage(schema)}.model.${entity.name}
    -> package ${targetPkg}

input ${entity.name}Input {

${inputProps.join('\n')}
}${spec}`;
}

function genDtoGeneration(schema: Schema): string {
    return `package ${modulePackage(schema)}.service;

import org.babyfish.jimmer.client.EnableImplicitApi;
import org.babyfish.jimmer.sql.EnableDtoGeneration;

/**
 * jimmer-apt 需要该注解手动启用 src/main/dto 下 DTO 文件的编译；
 * EnableImplicitApi 为前端 TS 客户端生成接口元数据
 */
@EnableDtoGeneration
@EnableImplicitApi
public interface DtoGeneration {
}
`;
}

// ---------------------------------------------------------------- ModuleProvider

function genModuleProvider(schema: Schema): string {
    const code = schema.project.moduleCode;
    const children = schema.entities
        .map((entity, index) => {
            const lower = entity.name.toLowerCase();
            const icon = entity.icon ?? 'application';
            const label = entity.label ?? entity.name;
            return `                MenuNode.of("${label}", MenuType.MENU)
                        .path("/${code}/${lower}")
                        .component("/${code}/${lower}/index")
                        .icon("${icon}")
                        .sortOrder(${index + 1})
                        .children(
                                button("查询${label}", "${code}:${lower}:list", 1),
                                button("新增${label}", "${code}:${lower}:add", 2),
                                button("编辑${label}", "${code}:${lower}:edit", 3),
                                button("删除${label}", "${code}:${lower}:delete", 4)
                        )`;
        })
        .join(',\n');

    return `package ${modulePackage(schema)}.module;

import com.jezetek.core.runtime.module.MenuNode;
import com.jezetek.core.runtime.module.MenuType;
import com.jezetek.core.runtime.module.ModuleProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ${schema.project.moduleName}模块自描述：菜单树声明 + 受保护接口前缀。
 *
 * <p>注意：要让角色看到某节点，该节点及其全部祖先都必须显式 .roles(...)
 * (祖先漏标会导致路由树断链)；ADMIN 超管直通无需声明</p>
 */
@Component
public class ${capitalize(code)}ModuleProvider implements ModuleProvider {

    @Override
    public String code() {
        return "${code}";
    }

    @Override
    public String name() {
        return "${schema.project.moduleName}";
    }

    @Override
    public List<String> apiPrefixes() {
        return List.of("/api/v1/${code}/**");
    }

    @Override
    public List<MenuNode> menus() {
        return List.of(
                MenuNode.of("${schema.project.moduleName}", MenuType.DIR)
                        .path("/${code}")
                        .sortOrder(9)
                        .children(
${children}
                        )
        );
    }

    private static MenuNode button(String name, String perms, int sortOrder) {
        return MenuNode.of(name, MenuType.BUTTON).perms(perms).sortOrder(sortOrder);
    }
}
`;
}

// ---------------------------------------------------------------- sql

function genSchemaSql(schema: Schema): string {
    const parts: string[] = [
        `-- ${schema.project.moduleName}模块表结构(随模块 jar 自带，主应用通过 classpath*:sql/*-schema.sql 通配加载)`,
        '-- 约束：模块之间不允许外键引用，模块内部建表顺序自洽',
        '',
    ];
    // 先删中间表，再按声明逆序删实体表
    for (const entity of schema.entities) {
        for (const p of entity.props) {
            if (p.kind === 'manyToMany' && p.joinTable) {
                parts.push(`drop table ${p.joinTable.name.toLowerCase()} if exists;`);
            }
        }
    }
    for (const entity of [...schema.entities].reverse()) {
        parts.push(`drop table ${entity.table.toLowerCase()} if exists;`);
    }
    parts.push('');
    for (const entity of schema.entities) {
        parts.push(...createTable(schema, entity));
    }
    return parts.join('\n') + '\n';
}

function createTable(schema: Schema, entity: EntityDef): string[] {
    const cols: string[] = ['id identity(100, 1) not null'];
    for (const p of entity.props) {
        if (p.kind === 'scalar') {
            cols.push(`${snake(p.name).toLowerCase()} ${h2Type(p)}${p.nullable ? '' : ' not null'}`);
        } else if (p.kind === 'manyToOne') {
            cols.push(`${snake(p.name).toLowerCase()}_id bigint`);
        }
    }
    if (entity.tenantAware) {
        cols.push('tenant varchar(20) not null');
    }
    if (entity.baseEntity !== false) {
        cols.push('created_time timestamp not null', 'modified_time timestamp not null');
    }

    const alters: string[] = [];
    const keys = keyColumns(entity);
    if (keys.length) {
        alters.push(
            `alter table ${entity.table.toLowerCase()}`,
            `    add constraint business_key_${entity.table.toLowerCase()}`,
            `        unique(${keys.join(', ').toLowerCase()});`
        );
    }
    for (const p of entity.props) {
        if (p.kind === 'manyToOne') {
            alters.push(
                `alter table ${entity.table.toLowerCase()}`,
                `    add constraint fk_${entity.table.toLowerCase()}__${snake(p.name).toLowerCase()}`,
                `        foreign key(${snake(p.name).toLowerCase()}_id)`,
                `            references ${findTable(schema, p.target).toLowerCase()}(id);`
            );
        }
    }
    return [
        `create table ${entity.table.toLowerCase()}(`,
        ...cols.map((c, i) => `    ${c}${i < cols.length - 1 ? ',' : ''}`),
        ');',
        ...alters,
        '',
    ];
}

function findTable(schema: Schema, entityName: string): string {
    return schema.entities.find(e => e.name === entityName)?.table ?? entityName.toLowerCase();
}

function genDataSql(schema: Schema): string | undefined {
    if (!schema.seeds) {
        return undefined;
    }
    const lines: string[] = [`-- ${schema.project.moduleName}模块种子数据`, ''];
    for (const [entityName, rows] of Object.entries(schema.seeds)) {
        const entity = schema.entities.find(e => e.name === entityName);
        if (!entity) {
            continue;
        }
        lines.push(`insert into ${entity.table.toLowerCase()} values`, ...rows.map(r => `    ${r}`), ';', '');
    }
    return lines.join('\n');
}
