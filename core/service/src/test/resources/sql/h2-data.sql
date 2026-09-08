insert into sys_menu(id, name, parent_id, path, icon, sort_order, created_time, modified_time) values
    (1, '系统管理', null, null, 'setting', 1, current_timestamp(), current_timestamp()),
        (2, '用户管理', 1, '/user/index', 'user', 1, current_timestamp(), current_timestamp()),
        (3, '角色管理', 1, '/role/index', 'usergroup', 2, current_timestamp(), current_timestamp()),
        (4, '菜单管理', 1, '/menu/index', 'menu', 3, current_timestamp(), current_timestamp())
;

insert into sys_role(id, code, name, description, tenant, created_time, modified_time) values
    (1, 'ADMIN', '管理员', '拥有全部权限的内置角色', 'default', current_timestamp(), current_timestamp()),
    (2, 'USER', '普通用户', '基础查看权限', 'default', current_timestamp(), current_timestamp())
;

insert into sys_role_menu_mapping(role_id, menu_id) values
    (1, 1), (1, 2), (1, 3), (1, 4),
    (2, 1), (2, 2)
;

/*
 * 密码此处用明文仅作演示；接入登录认证后应改为 BCrypt 密文存储
 */
insert into sys_user(id, username, password, nickname, enabled, tenant, created_time, modified_time) values
    (1, 'admin', '123456', 'Administrator', true, 'default', current_timestamp(), current_timestamp()),
    (2, 'demo', '123456', '演示用户', true, 'default', current_timestamp(), current_timestamp())
;

insert into sys_user_role_mapping(user_id, role_id) values
    (1, 1),
    (2, 2)
;
