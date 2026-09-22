-- 系统管理模块表结构(随模块 jar 自带，主应用通过 classpath*:sql/*-schema.sql 通配加载)
-- 约束：模块之间不允许外键引用，模块内部建表顺序自洽

drop table sys_oper_log if exists;
drop table sys_logininfor if exists;
drop table sys_notice if exists;
drop table sys_config if exists;
drop table sys_dict_data if exists;
drop table sys_dict_type if exists;
drop table sys_role_dept if exists;
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

-- 操作日志(租户隔离)：@Log 标注的写操作由切面异步落库
create table sys_oper_log(
    id identity(100, 1) not null,
    module varchar(50) not null,
    action varchar(50) not null,
    operator varchar(50) not null,
    uri varchar(200),
    params varchar(2000),
    result varchar(2000),
    error_msg varchar(2000),
    cost_ms bigint not null,
    success boolean not null,
    tenant varchar(20) not null,
    created_time timestamp not null,
    modified_time timestamp not null
);

-- 登录日志(租户隔离)：登录成功/失败/登出由认证流程写入，管理端只读+清空
create table sys_logininfor(
    id identity(100, 1) not null,
    username varchar(50) not null,
    ip varchar(50) not null,
    message varchar(255),
    tenant varchar(20) not null,
    created_time timestamp not null,
    modified_time timestamp not null
);

-- 参数配置(租户隔离)：configKey 全局唯一；决策——不预置无人读取的键
create table sys_config(
    id identity(100, 1) not null,
    config_key varchar(100) not null,
    config_name varchar(100) not null,
    config_value varchar(500),
    remark varchar(200),
    tenant varchar(20) not null,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table sys_config
    add constraint business_key_sys_config
        unique(config_key);

-- 公告(租户隔离)：仅管理端 CRUD，无用户侧展示面；content 为纯文本
create table sys_notice(
    id identity(100, 1) not null,
    notice_title varchar(100) not null,
    notice_type varchar(50) not null,
    content clob,
    enabled boolean not null default true,
    tenant varchar(20) not null,
    created_time timestamp not null,
    modified_time timestamp not null
);

-- 字典类型(租户隔离)：type 为全局唯一编码，前端 useDict 按编码取条目
create table sys_dict_type(
    id identity(100, 1) not null,
    type varchar(50) not null,
    name varchar(50) not null,
    description varchar(200),
    enabled boolean not null default true,
    tenant varchar(20) not null,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table sys_dict_type
    add constraint business_key_sys_dict_type
        unique(type);

-- 字典条目(租户隔离)：同一字典内 value 唯一
create table sys_dict_data(
    id identity(100, 1) not null,
    dict_type_id bigint not null,
    label varchar(50) not null,
    data_value varchar(50) not null,
    sort_order integer not null,
    enabled boolean not null default true,
    tenant varchar(20) not null,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table sys_dict_data
    add constraint business_key_sys_dict_data
        unique(dict_type_id, data_value);
alter table sys_dict_data
    add constraint fk_sys_dict_data__type
        foreign key(dict_type_id)
            references sys_dict_type(id);

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
    data_scope integer not null default 1,
    tenant varchar(20) not null,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table sys_role
    add constraint business_key_sys_role
        unique(code);

-- 角色-自定义数据范围部门关联(精确集合，无层级展开)
create table sys_role_dept(
    role_id bigint not null,
    dept_id bigint not null
);
alter table sys_role_dept
    add constraint pk_sys_role_dept
        primary key(role_id, dept_id);
alter table sys_role_dept
    add constraint fk_sys_role_dept__role
        foreign key(role_id)
            references sys_role(id)
                on delete cascade;
alter table sys_role_dept
    add constraint fk_sys_role_dept__dept
        foreign key(dept_id)
            references sys_dept(id)
                on delete cascade;

-- 用户(租户隔离)，password 为 BCrypt 密文
create table sys_user(
    id identity(100, 1) not null,
    username varchar(50) not null,
    password varchar(100),
    nickname varchar(50),
    avatar blob,
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
