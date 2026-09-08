import type { EntityDef, Schema } from '../schema.js';
import { h2Type, keyColumns, snake } from '../support.js';

/** 生成 H2 建表脚本，命名约定与手写版一致: identity(100,1)、business_key_ 前缀唯一约束、fk_表__目标 外键 */
export function genSchemaSql(schema: Schema): string {
    const parts: string[] = [];
    parts.push(...dropStatements(schema));
    parts.push(...schema.entities.flatMap(e => createTable(schema, e)));
    parts.push(...joinTables(schema));
    return parts.join('\n') + '\n';
}

function dropStatements(schema: Schema): string[] {
    const lines: string[] = [];
    // 先删中间表，再按声明逆序删实体表
    for (const entity of schema.entities) {
        for (const p of entity.props) {
            if (p.kind === 'manyToMany' && p.joinTable) {
                lines.push(`drop table ${p.joinTable.name.toLowerCase()} if exists;`);
            }
        }
    }
    for (const entity of [...schema.entities].reverse()) {
        lines.push(`drop table ${entity.table.toLowerCase()} if exists;`);
    }
    return [...lines, ''];
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
        if (p.kind !== 'manyToOne') {
            continue;
        }
        const target = schema.entities.find(e => e.name === p.target);
        if (!target) {
            throw new Error(`实体 ${entity.name} 的关联目标 ${p.target} 未定义`);
        }
        const onDelete =
            p.onDissociate === 'SET_NULL' ? 'on delete set null'
            : p.onDissociate === 'DELETE' ? 'on delete cascade'
            : '';
        alters.push(
            `alter table ${entity.table.toLowerCase()}`,
            `    add constraint fk_${entity.table.toLowerCase()}__${p.name.toLowerCase()}`,
            `        foreign key(${snake(p.name).toLowerCase()}_id)`,
            `            references ${target.table.toLowerCase()}(id)${onDelete ? `\n                ${onDelete}` : ''};`
        );
    }

    return [
        entity.comment ? `-- ${entity.comment}` : '',
        `create table ${entity.table.toLowerCase()}(`,
        cols.map(c => `    ${c}`).join(',\n'),
        ');',
        ...alters,
        '',
    ].filter(l => l !== '');
}

function joinTables(schema: Schema): string[] {
    const out: string[] = [];
    for (const entity of schema.entities) {
        for (const p of entity.props) {
            if (p.kind !== 'manyToMany' || !p.joinTable) {
                continue;
            }
            const target = schema.entities.find(e => e.name === p.target)!;
            const jt = p.joinTable.name.toLowerCase();
            const colA = p.joinTable.joinColumnName.toLowerCase();
            const colB = p.joinTable.inverseJoinColumnName.toLowerCase();
            // 外键名取连接列去掉 _ID 后缀的短名(USER_ID -> user)，与手写版约定一致
            const shortA = colA.replace(/_id$/, '');
            const shortB = colB.replace(/_id$/, '');
            out.push(
                `create table ${jt}(`,
                `    ${colA} bigint not null,`,
                `    ${colB} bigint not null`,
                ');',
                `alter table ${jt}`,
                `    add constraint pk_${jt}`,
                `        primary key(${colA}, ${colB});`,
                `alter table ${jt}`,
                `    add constraint fk_${jt}__${shortA}`,
                `        foreign key(${colA})`,
                `            references ${entity.table.toLowerCase()}(id)`,
                `                on delete cascade;`,
                `alter table ${jt}`,
                `    add constraint fk_${jt}__${shortB}`,
                `        foreign key(${colB})`,
                `            references ${target.table.toLowerCase()}(id)`,
                `                on delete cascade;`,
                ''
            );
        }
    }
    return out;
}

export function genDataSql(schema: Schema): string | undefined {
    const entities = schema.entities.filter(e => schema.seeds?.[e.name]?.length);
    if (!entities.length) {
        return undefined;
    }
    const lines: string[] = [];
    for (const entity of entities) {
        const seed = schema.seeds?.[entity.name] ?? [];
        if (entity.comment) {
            lines.push(`-- ${entity.comment}`);
        }
        lines.push(...seed, '');
    }
    return lines.join('\n') + '\n';
}
