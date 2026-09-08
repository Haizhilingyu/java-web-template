import type { EntityDef, Schema } from '../schema.js';
import { capitalize, javaType, snake } from '../support.js';

const KEY_UNIQUE = `@KeyUniqueConstraint(
        // Only for mysql
        noMoreUniqueConstraints = true,
        // Only for postgres
        isNullNotDistinct = true
)`;

export function genEntity(schema: Schema, entity: EntityDef): string {
    const pkg = schema.project.javaPackage;
    const baseEntity = entity.baseEntity !== false;
    const usesTenant = !!entity.tenantAware;
    const hasKey = entity.props.some(p => (p.kind === 'scalar' || p.kind === 'manyToOne') && p.key);

    const needNullable = entity.props.some(
        p => (p.kind === 'scalar' || p.kind === 'manyToOne') && p.nullable
    );
    const needList = entity.props.some(p => p.kind === 'oneToMany' || p.kind === 'manyToMany');
    const needBigDecimal = entity.props.some(p => p.kind === 'scalar' && p.type === 'BigDecimal');
    const needDateTime = entity.props.some(p => p.kind === 'scalar' && p.type === 'LocalDateTime');

    const imports: string[] = [];
    if (needNullable) {
        imports.push('import org.jetbrains.annotations.Nullable;');
    }
    if (needList) {
        imports.push('');
        imports.push('import java.util.List;');
    }
    if (needBigDecimal) {
        imports.push('');
        imports.push('import java.math.BigDecimal;');
    }
    if (needDateTime) {
        imports.push('');
        imports.push('import java.time.LocalDateTime;');
    }

    const baseIfaces = [baseEntity ? 'BaseEntity' : '', usesTenant ? 'TenantAware' : '']
        .filter(Boolean)
        .join(', ');

    const classDoc = entity.comment
        ? `/**
 * ${entity.comment}
 */\n`
        : '';

    const body = entity.props.map(p => genProp(p)).join('\n\n');

    return `package ${pkg}.model;

import org.babyfish.jimmer.sql.*;
import ${pkg}.model.common.BaseEntity;${usesTenant ? `
import ${pkg}.model.common.TenantAware;` : ''}${imports.length ? '\n' + imports.join('\n') : ''}
${classDoc}@Entity
${entity.table ? `@Table(name = "${entity.table}")\n` : ''}${hasKey ? KEY_UNIQUE + '\n' : ''}public interface ${entity.name}${baseIfaces ? ' extends ' + baseIfaces : ''} {

${body}
}
`;
}

function doc(comment?: string): string {
    return comment ? `    /**
     * ${comment}
     */
` : '';
}

function genProp(p: EntityDef['props'][number]): string {
    switch (p.kind) {
        case 'id':
            return `${doc(p.comment)}    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();`;
        case 'scalar': {
            const anno: string[] = [];
            if (p.key) {
                anno.push('    @Key');
            }
            if (p.nullable) {
                anno.push('    @Nullable');
            }
            const annotations = anno.length ? anno.join('\n') + '\n' : '';
            return `${doc(p.comment)}${annotations}    ${javaType(p.type, !!p.nullable)} ${p.name}();`;
        }
        case 'manyToOne': {
            const anno: string[] = [];
            if (p.nullable) {
                anno.push('    @Nullable');
            }
            if (p.key) {
                anno.push('    @Key');
            }
            anno.push('    @ManyToOne');
            if (p.onDissociate) {
                anno.push(`    @OnDissociate(DissociateAction.${p.onDissociate})`);
            }
            return `${doc(p.comment)}${anno.join('\n')}
    ${p.target} ${p.name}();`;
        }
        case 'oneToMany': {
            const ordered = p.orderedBy
                ? `, orderedProps = @OrderedProp("${p.orderedBy}")`
                : '';
            return `${doc(p.comment)}    @OneToMany(mappedBy = "${p.mappedBy}"${ordered})
    List<${p.target}> ${p.name}();`;
        }
        case 'manyToMany': {
            const idView = idViewName(p.name);
            if (p.joinTable) {
                const ordered = p.orderedBy
                    ? `(orderedProps = ${p.orderedBy
                          .split(',')
                          .map(o => `@OrderedProp("${o.trim()}")`)
                          .join(', ')})`
                    : '';
                return `${doc(p.comment)}    @ManyToMany${ordered}
    @JoinTable(
            name = "${p.joinTable.name}",
            joinColumnName = "${p.joinTable.joinColumnName}",
            inverseJoinColumnName = "${p.joinTable.inverseJoinColumnName}"
    )
    List<${p.target}> ${p.name}();

    /**
     * 关联属性 ${p.name} 的 id 视图
     */
    @IdView("${p.name}")
    List<Long> ${idView}();`;
            }
            return `${doc(p.comment)}    @ManyToMany(mappedBy = "${p.mappedBy}")
    List<${p.target}> ${p.name}();`;
        }
    }
}

export function genBaseEntity(pkg: string): string {
    return `package ${pkg}.model.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.babyfish.jimmer.sql.MappedSuperclass;

import java.time.LocalDateTime;

@MappedSuperclass
public interface BaseEntity {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdTime();

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime modifiedTime();
}
`;
}

export function genTenantAware(pkg: string): string {
    return `package ${pkg}.model.common;

import org.babyfish.jimmer.sql.MappedSuperclass;

@MappedSuperclass
public interface TenantAware {

    String tenant();
}
`;
}

/** 实体文件名与表名的对照，供 SQL 生成等使用 */
export function tableOf(entity: EntityDef): string {
    return entity.table ?? snake(entity.name);
}

/**
 * 多对多拥有方的 id 视图属性名: roles -> roleIds, menus -> menuIds
 * (去掉词尾 s 后加 Ids)
 */
export function idViewName(propName: string): string {
    return `${propName.replace(/s$/, '')}Ids`;
}

export function keyQuerySuffix(keyProp: string): string {
    return `By${capitalize(keyProp)}`;
}
