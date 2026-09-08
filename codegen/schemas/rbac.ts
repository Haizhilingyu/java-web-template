import type { Schema } from '../src/schema.js';

/**
 * RBAC(用户/角色/菜单)示例 Schema —— 与 java-web-template 现有手写实现一一对应，
 * 用于验收生成器输出
 */
const schema: Schema = {
    project: {
        javaPackage: 'com.jezetek.core',
        groupId: 'com.jezetek.core',
        artifactId: 'core',
        version: '0.0.1-SNAPSHOT',
        rootProject: { groupId: 'com.jezetek', artifactId: 'java-web-template', version: '0.0.1-SNAPSHOT' },
        jimmerVersion: '0.12.0',
        springBootVersion: '4.0.8',
        jacksonVersion: '2.21.0',
        javaVersion: '21',
        defaultTenantProperty: 'core.default-tenant',
    },
    entities: [
        {
            name: 'User',
            table: 'sys_user',
            tenantAware: true,
            defaultSort: 'username asc',
            props: [
                { kind: 'id' },
                {
                    kind: 'scalar', name: 'username', type: 'string', length: 50, key: true,
                    comment: '登录用户名，全局唯一',
                    validation: [
                        '@jakarta.validation.constraints.NotBlank(message = "用户名不能为空")',
                        '@jakarta.validation.constraints.Size(min = 3, max = 50, message = "用户名长度必须在3~50之间")',
                        '@jakarta.validation.constraints.Pattern(regexp = "[a-zA-Z0-9_]+", message = "用户名只能包含字母、数字和下划线")',
                    ],
                },
                {
                    kind: 'scalar', name: 'password', type: 'string', length: 100, nullable: true, comment: '登录密码(密文)',
                    validation: ['@jakarta.validation.constraints.Size(max = 100, message = "密码长度不能超过100")'],
                },
                {
                    kind: 'scalar', name: 'nickname', type: 'string', length: 50, nullable: true, comment: '显示昵称',
                    validation: ['@jakarta.validation.constraints.Size(max = 50, message = "昵称长度不能超过50")'],
                },
                { kind: 'scalar', name: 'enabled', type: 'boolean', comment: '是否启用' },
                {
                    kind: 'manyToMany', name: 'roles', target: 'Role', orderedBy: 'code',
                    joinTable: { name: 'sys_user_role_mapping', joinColumnName: 'USER_ID', inverseJoinColumnName: 'ROLE_ID' },
                    comment: '当前用户拥有的所有角色',
                },
            ],
            search: {
                keyword: ['username', 'nickname'],
                eq: ['enabled'],
                flatLike: [{ by: 'roles', prop: 'name', as: 'roleName' }],
            },
        },
        {
            name: 'Role',
            table: 'sys_role',
            tenantAware: true,
            defaultSort: 'code asc',
            props: [
                { kind: 'id' },
                {
                    kind: 'scalar', name: 'code', type: 'string', length: 50, key: true,
                    comment: '角色编码(如 ADMIN)',
                    validation: [
                        '@jakarta.validation.constraints.NotBlank(message = "角色编码不能为空")',
                        '@jakarta.validation.constraints.Size(max = 50, message = "角色编码长度不能超过50")',
                        '@jakarta.validation.constraints.Pattern(regexp = "[A-Z][A-Z0-9_]*", message = "角色编码必须为大写字母开头，可含大写字母、数字和下划线")',
                    ],
                },
                {
                    kind: 'scalar', name: 'name', type: 'string', length: 50, comment: '角色显示名',
                    validation: [
                        '@jakarta.validation.constraints.NotBlank(message = "角色名称不能为空")',
                        '@jakarta.validation.constraints.Size(max = 50, message = "角色名称长度不能超过50")',
                    ],
                },
                {
                    kind: 'scalar', name: 'description', type: 'string', length: 200, nullable: true, comment: '角色描述',
                    validation: ['@jakarta.validation.constraints.Size(max = 200, message = "角色描述长度不能超过200")'],
                },
                { kind: 'manyToMany', name: 'users', target: 'User', mappedBy: 'roles', comment: '拥有当前角色的所有用户' },
                {
                    kind: 'manyToMany', name: 'menus', target: 'Menu', orderedBy: 'sortOrder',
                    joinTable: { name: 'sys_role_menu_mapping', joinColumnName: 'ROLE_ID', inverseJoinColumnName: 'MENU_ID' },
                    comment: '当前角色可访问的所有菜单',
                },
            ],
            search: {
                keyword: ['code', 'name'],
                flatLike: [{ by: 'menus', prop: 'name', as: 'menuName' }],
            },
        },
        {
            name: 'Menu',
            table: 'sys_menu',
            comment: '菜单树(全局数据，不挂租户)',
            defaultSort: 'sortOrder asc',
            props: [
                { kind: 'id' },
                {
                    kind: 'scalar', name: 'name', type: 'string', length: 50, key: true, comment: '菜单名，与 parent 组成唯一约束',
                    validation: [
                        '@jakarta.validation.constraints.NotBlank(message = "菜单名不能为空")',
                        '@jakarta.validation.constraints.Size(max = 50, message = "菜单名长度不能超过50")',
                    ],
                },
                {
                    kind: 'manyToOne', name: 'parent', target: 'Menu', nullable: true, key: true,
                    onDissociate: 'SET_NULL', comment: '父菜单，根节点为 null',
                },
                {
                    kind: 'scalar', name: 'path', type: 'string', length: 200, nullable: true, comment: '前端路由地址',
                    validation: ['@jakarta.validation.constraints.Size(max = 200, message = "路由地址长度不能超过200")'],
                },
                {
                    kind: 'scalar', name: 'icon', type: 'string', length: 100, nullable: true, comment: '菜单图标名',
                    validation: ['@jakarta.validation.constraints.Size(max = 100, message = "图标名长度不能超过100")'],
                },
                {
                    kind: 'scalar', name: 'sortOrder', type: 'int', comment: '同级排序号，越小越靠前',
                    validation: ['@jakarta.validation.constraints.Min(value = 0, message = "排序号不能为负数")'],
                },
                { kind: 'oneToMany', name: 'children', target: 'Menu', mappedBy: 'parent', orderedBy: 'sortOrder', comment: '子菜单' },
                { kind: 'manyToMany', name: 'roles', target: 'Role', mappedBy: 'menus', comment: '可访问当前菜单的所有角色' },
            ],
            search: {
                keyword: ['name', 'path'],
                flatLike: [{ by: 'parent', prop: 'name', as: 'parentName' }],
            },
        },
    ],
    seeds: {
        Menu: [
            `insert into sys_menu(id, name, parent_id, path, icon, sort_order, created_time, modified_time) values`,
            `    (1, '系统管理', null, null, 'setting', 1, current_timestamp(), current_timestamp()),`,
            `        (2, '用户管理', 1, '/user/index', 'user', 1, current_timestamp(), current_timestamp()),`,
            `        (3, '角色管理', 1, '/role/index', 'usergroup', 2, current_timestamp(), current_timestamp()),`,
            `        (4, '菜单管理', 1, '/menu/index', 'menu', 3, current_timestamp(), current_timestamp());`,
        ],
        Role: [
            `insert into sys_role(id, code, name, description, tenant, created_time, modified_time) values`,
            `    (1, 'ADMIN', '管理员', '拥有全部权限的内置角色', 'default', current_timestamp(), current_timestamp()),`,
            `    (2, 'USER', '普通用户', '基础查看权限', 'default', current_timestamp(), current_timestamp());`,
        ],
    },
};

export default schema;
