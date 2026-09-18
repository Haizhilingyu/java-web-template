-- system 模块测试种子数据(启动菜单同步已关闭，菜单在此预置供树/授权用例使用)

insert into sys_menu(id, name, type, parent_id, path, component, perms, icon, visible, sort_order, module_code, created_time, modified_time) values
    (1, '系统管理', 'M', null, '/system', null, null, 'setting', true, 1, 'system', current_timestamp(), current_timestamp()),
        (2, '用户管理', 'C', 1, '/user/index', '/user/index', null, 'user', true, 1, null, current_timestamp(), current_timestamp()),
        (3, '角色管理', 'C', 1, '/role/index', '/role/index', null, 'usergroup', true, 2, null, current_timestamp(), current_timestamp()),
        (4, '菜单管理', 'C', 1, '/menu/index', '/menu/index', null, 'menu', true, 3, null, current_timestamp(), current_timestamp())
;

insert into sys_role(id, code, name, description, tenant, created_time, modified_time) values
    (1, 'ADMIN', '管理员', '拥有全部权限的内置角色', 'default', current_timestamp(), current_timestamp()),
    (2, 'USER', '普通用户', '基础查看权限', 'default', current_timestamp(), current_timestamp())
;

insert into sys_role_menu_mapping(role_id, menu_id) values
    (1, 1), (1, 2), (1, 3), (1, 4),
    (2, 1), (2, 2)
;

-- admin / demo / frozen 的密码均为 123456(BCrypt 固定密文)，frozen 为禁用账号
insert into sys_user(id, username, password, nickname, enabled, tenant, created_time, modified_time) values
    (1, 'admin', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Administrator', true, 'default', current_timestamp(), current_timestamp()),
    (2, 'demo', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '演示用户', true, 'default', current_timestamp(), current_timestamp()),
    (3, 'frozen', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '冻结用户', false, 'default', current_timestamp(), current_timestamp())
;

insert into sys_user_role_mapping(user_id, role_id) values
    (1, 1),
    (2, 2),
    (3, 2)
;
