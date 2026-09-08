import type { EntityDef, ManyToOneProp, ManyToManyProp, OneToManyProp, PropDef, ScalarProp, Schema } from './schema.js';

/** camelCase -> SNAKE_CASE */
export function snake(name: string): string {
    return name.replace(/([a-z0-9])([A-Z])/g, '$1_$2').toUpperCase();
}

/** 首字母大写 */
export function capitalize(name: string): string {
    return name.charAt(0).toUpperCase() + name.slice(1);
}

export function findEntity(schema: Schema, name: string): EntityDef {
    const entity = schema.entities.find(e => e.name === name);
    if (!entity) {
        throw new Error(`实体 ${name} 未在 schema 中定义`);
    }
    return entity;
}

export function isScalar(p: PropDef): p is ScalarProp {
    return p.kind === 'scalar';
}

/** ScalarType -> Java 类型 */
export function javaType(type: ScalarProp['type'], nullable: boolean): string {
    switch (type) {
        case 'string': return 'String';
        case 'boolean': return nullable ? 'Boolean' : 'boolean';
        case 'int': return nullable ? 'Integer' : 'int';
        case 'long': return nullable ? 'Long' : 'long';
        case 'BigDecimal': return 'BigDecimal';
        case 'LocalDateTime': return 'LocalDateTime';
    }
}

/** ScalarProp -> H2 列类型 */
export function h2Type(p: ScalarProp): string {
    switch (p.type) {
        case 'string': return `varchar(${p.length ?? 255})`;
        case 'boolean': return 'boolean';
        case 'int': return 'integer';
        case 'long': return 'bigint';
        case 'BigDecimal': return `numeric(${p.precision ?? '10,2'})`;
        case 'LocalDateTime': return 'timestamp';
    }
}

/** 自引用树形实体(如菜单) */
export function isTree(schema: Schema, entity: EntityDef): boolean {
    return entity.props.some(p => p.kind === 'manyToOne' && p.target === entity.name);
}

/** 拥有方多对多(带中间表定义) */
export function owningManyToMany(entity: EntityDef): ManyToManyProp[] {
    return entity.props.filter((p): p is ManyToManyProp => p.kind === 'manyToMany' && !!p.joinTable);
}

/** 反向方多对多 */
export function inverseManyToMany(entity: EntityDef): ManyToManyProp[] {
    return entity.props.filter((p): p is ManyToManyProp => p.kind === 'manyToMany' && !!p.mappedBy);
}

/** 唯一业务键(@Key)列名列表：外键列在前、标量列在后(与手写版顺序一致) */
export function keyColumns(entity: EntityDef): string[] {
    const keys = entity.props.filter((p): p is ScalarProp | ManyToOneProp =>
        (p.kind === 'scalar' || p.kind === 'manyToOne') && !!p.key);
    const fkCols = keys.filter(p => p.kind === 'manyToOne').map(p => `${snake(p.name)}_ID`);
    const scalarCols = keys.filter(p => p.kind === 'scalar').map(p => snake(p.name));
    return [...fkCols, ...scalarCols];
}

/** 单一标量业务键(生成 findBy<Key> 查询与按键查询接口) */
export function singleScalarKey(entity: EntityDef): ScalarProp | undefined {
    const keys = entity.props.filter(p => p.kind === 'scalar' && p.key);
    return keys.length === 1 ? keys[0] as ScalarProp : undefined;
}

/** java 包路径 com.jezetek.core -> com/jezetek/core */
export function packagePath(pkg: string): string {
    return pkg.replace(/\./g, '/');
}
