/**
 * 实体模型 Schema 类型定义。
 *
 * Schema 以 TypeScript 文件编写(享受类型检查与注释提示)，
 * 由 src/cli.ts 加载后生成整套 jimmer 模块:
 * model(实体) / repository / runtime(租户等基础设施) / service(服务+DTO) + sql + pom
 */

/** 标量属性类型 -> Java 类型 / H2 列类型的映射见 src/gen/support.ts */
export type ScalarType =
    | 'string'
    | 'boolean'
    | 'int'
    | 'long'
    | 'BigDecimal'
    | 'LocalDateTime';

export type OnDissociate = 'SET_NULL' | 'DELETE';

interface BaseProp {
    /** 属性上的中文备注，生成 javadoc / SQL 注释 */
    comment?: string;
}

/** 自增代理主键: @Id + @GeneratedValue(IDENTITY) + identity(100,1) */
export interface IdProp extends BaseProp {
    kind: 'id';
}

export interface ScalarProp extends BaseProp {
    kind: 'scalar';
    name: string;
    type: ScalarType;
    /** varchar 长度，默认 255；BigDecimal 为精度标度 "10,2" */
    length?: number;
    precision?: string;
    nullable?: boolean;
    /** 业务键(@Key)，参与唯一约束与 upsert 匹配 */
    key?: boolean;
    /**
     * DTO Input 中的校验注解原文(全限定名，jimmer dto 语法要求)，
     * 如 '@jakarta.validation.constraints.NotBlank(message = "用户名不能为空")'
     */
    validation?: string[];
}

export interface ManyToOneProp extends BaseProp {
    kind: 'manyToOne';
    name: string;
    target: string;
    nullable?: boolean;
    /** 作为业务键的一部分(如菜单树 name+parent 唯一) */
    key?: boolean;
    onDissociate?: OnDissociate;
}

export interface OneToManyProp extends BaseProp {
    kind: 'oneToMany';
    name: string;
    /** 目标实体名 */
    target: string;
    /** 目标实体上指向本实体的 manyToOne 属性名 */
    mappedBy: string;
    /** 子项排序属性名 */
    orderedBy?: string;
}

export interface ManyToManyProp extends BaseProp {
    kind: 'manyToMany';
    name: string;
    target: string;
    /** 拥有方: 中间表定义 */
    joinTable?: {
        name: string;
        joinColumnName: string;
        inverseJoinColumnName: string;
    };
    /** 反向方: 指向拥有方属性名 */
    mappedBy?: string;
    orderedBy?: string;
}

export type PropDef =
    | IdProp
    | ScalarProp
    | ManyToOneProp
    | OneToManyProp
    | ManyToManyProp;

/** 超级 QBE 的动态条件定义，生成 <Entity>Specification */
export interface SearchDef {
    /** like/i(...) as keyword 的属性列表(多属性 OR，必须 >=1 才生成) */
    keyword?: string[];
    /** 精确匹配属性 */
    eq?: string[];
    /** flat(关联){ like/i(prop) as alias } */
    flatLike?: Array<{ by: string; prop: string; as: string }>;
}

export interface EntityDef {
    /** 实体名(PascalCase)，如 User */
    name: string;
    /** 表名，如 sys_user(保留字需自行规避) */
    table: string;
    comment?: string;
    /** 菜单图标名(tdesign icons)，默认 application */
    icon?: string;
    /** 中文显示名(菜单名/@Log 动作)，缺省用实体名 */
    label?: string;
    /** 继承 BaseEntity(createdTime/modifiedTime)，默认 true */
    baseEntity?: boolean;
    /** 继承 TenantAware(多租户隔离)，默认 false */
    tenantAware?: boolean;
    /** 列表默认排序，如 'sortOrder asc'，缺省 'id asc' */
    defaultSort?: string;
    props: PropDef[];
    search?: SearchDef;
}

export interface ProjectDef {
    /** 模块编码(小写)：决定包名 com.jezetek.modules.{code}、接口前缀 /api/v1/{code}、权限点 {code}:... */
    moduleCode: string;
    /** 模块显示名(中文)，用于 ModuleProvider 与菜单目录 */
    moduleName: string;
    /** Maven 坐标: 模块 groupId/version 与根工程(artifactId 即 moduleCode) */
    groupId: string;
    version: string;
    rootProject: { groupId: string; artifactId: string; version: string };
    jimmerVersion: string;
    javaVersion: string;
}

export interface Schema {
    project: ProjectDef;
    entities: EntityDef[];
    /** 可选种子数据: 实体名 -> 原样拼进 h2-data.sql 的 INSERT 行 */
    seeds?: Record<string, string[]>;
}
