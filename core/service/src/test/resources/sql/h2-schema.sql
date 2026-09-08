drop table sys_user_role_mapping if exists;
drop table sys_role_menu_mapping if exists;
drop table sys_user if exists;
drop table sys_role if exists;
drop table sys_menu if exists;

-- 菜单(全局数据，无租户列)
create table sys_menu(
    id identity(100, 1) not null,
    name varchar(50) not null,
    parent_id bigint,
    path varchar(200),
    icon varchar(100),
    sort_order integer not null,
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

-- 角色(租户隔离)
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

-- 用户(租户隔离)
create table sys_user(
    id identity(100, 1) not null,
    username varchar(50) not null,
    password varchar(100),
    nickname varchar(50),
    enabled boolean not null,
    tenant varchar(20) not null,
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table sys_user
    add constraint business_key_sys_user
        unique(username);

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
