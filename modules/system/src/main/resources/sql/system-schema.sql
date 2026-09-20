-- 系统管理模块表结构(随模块 jar 自带，主应用通过 classpath*:sql/*-schema.sql 通配加载)
-- 约束：模块之间不允许外键引用，模块内部建表顺序自洽

drop table sys_user_post if exists;
drop table sys_user_role_mapping if exists;
drop table sys_role_menu_mapping if exists;
drop table sys_user if exists;
drop table sys_role if exists;
drop table sys_menu if exists;
drop table sys_post if exists;
drop table sys_dept if exists;

-- 菜单(全局数据，无租户列)
-- type: M 目录 / C 菜单(页面) / F 按钮
create table sys_menu(
    id identity(100, 1) not null,
    name varchar(50) not null,
    type varchar(1) not null default 'C',
    parent_id bigint,
    path varchar(200),
    component varchar(200),
    perms varchar(100),
    icon varchar(100),
    visible boolean not null default true,
    sort_order integer not null,
    module_code varchar(50),
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table sys_menu
    add constraint business_key_sys_menu
        unique(parent_id, name);
alter table sys_menu
    add constraint fk_sys_menu__parent
        foreign key(parent_id)
            references sys_menu(id)
                on delete set null;

-- 部门(租户隔离)树形结构
create table sys_dept(
    id identity(100, 1) not null,
    name varchar(50) not null,
    parent_id bigint,
    sort_order integer not null,
    enabled boolean not null default true,
    leader varchar(50),
    phone varchar(50),
    email varchar(100),
    tenant varchar(20) not null,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table sys_dept
    add constraint business_key_sys_dept
        unique(parent_id, name);
alter table sys_dept
    add constraint fk_sys_dept__parent
        foreign key(parent_id)
            references sys_dept(id);

-- 岗位(租户隔离)
create table sys_post(
    id identity(100, 1) not null,
    code varchar(50) not null,
    name varchar(50) not null,
    sort_order integer not null,
    enabled boolean not null default true,
    tenant varchar(20) not null,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table sys_post
    add constraint business_key_sys_post
        unique(code);

-- 角色(租户隔离)
-- code=ADMIN 为内置超管：菜单与接口权限直通
create table sys_role(
    id identity(100, 1) not null,
    code varchar(50) not null,
    name varchar(50) not null,
    description varchar(200),
    tenant varchar(20) not null,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table sys_role
    add constraint business_key_sys_role
        unique(code);

-- 用户(租户隔离)，password 为 BCrypt 密文
create table sys_user(
    id identity(100, 1) not null,
    username varchar(50) not null,
    password varchar(100),
    nickname varchar(50),
    enabled boolean not null,
    dept_id bigint,
    tenant varchar(20) not null,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table sys_user
    add constraint business_key_sys_user
        unique(username);
alter table sys_user
    add constraint fk_sys_user__dept
        foreign key(dept_id)
            references sys_dept(id);

create table sys_user_role_mapping(
    user_id bigint not null,
    role_id bigint not null
);
alter table sys_user_role_mapping
    add constraint pk_sys_user_role_mapping
        primary key(user_id, role_id);
alter table sys_user_role_mapping
    add constraint fk_sys_user_role_mapping__user
        foreign key(user_id)
            references sys_user(id)
                on delete cascade;
alter table sys_user_role_mapping
    add constraint fk_sys_user_role_mapping__role
        foreign key(role_id)
            references sys_role(id)
                on delete cascade;

create table sys_role_menu_mapping(
    role_id bigint not null,
    menu_id bigint not null
);
alter table sys_role_menu_mapping
    add constraint pk_sys_role_menu_mapping
        primary key(role_id, menu_id);
alter table sys_role_menu_mapping
    add constraint fk_sys_role_menu_mapping__role
        foreign key(role_id)
            references sys_role(id)
                on delete cascade;
alter table sys_role_menu_mapping
    add constraint fk_sys_role_menu_mapping__menu
        foreign key(menu_id)
            references sys_menu(id)
                on delete cascade;

create table sys_user_post(
    user_id bigint not null,
    post_id bigint not null
);
alter table sys_user_post
    add constraint pk_sys_user_post
        primary key(user_id, post_id);
alter table sys_user_post
    add constraint fk_sys_user_post__user
        foreign key(user_id)
            references sys_user(id)
                on delete cascade;
alter table sys_user_post
    add constraint fk_sys_user_post__post
        foreign key(post_id)
            references sys_post(id)
                on delete cascade;
