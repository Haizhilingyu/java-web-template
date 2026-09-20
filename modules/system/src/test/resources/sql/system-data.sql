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

-- 基础字典：是/否、通知/公告(公告类型在工单04消费)
insert into sys_dict_type(id, type, name, description, enabled, tenant, created_time, modified_time) values
    (1, 'sys_yes_no', '是/否', '通用布尔字典', true, 'default', current_timestamp(), current_timestamp()),
    (2, 'sys_notice_type', '通知公告类型', '通知/公告分类', true, 'default', current_timestamp(), current_timestamp())
;

insert into sys_dict_data(id, dict_type_id, label, data_value, sort_order, enabled, tenant, created_time, modified_time) values
    (1, 1, '是', 'Y', 1, true, 'default', current_timestamp(), current_timestamp()),
    (2, 1, '否', 'N', 2, true, 'default', current_timestamp(), current_timestamp()),
    (3, 2, '通知', '1', 1, true, 'default', current_timestamp(), current_timestamp()),
    (4, 2, '公告', '2', 2, true, 'default', current_timestamp(), current_timestamp())
;

-- 默认部门树：总公司 + 三个部门(挂在总公司下)，与主种子保持一致
insert into sys_dept(id, name, parent_id, sort_order, enabled, leader, phone, email, tenant, created_time, modified_time) values
    (1, '总公司', null, 1, true, null, null, null, 'default', current_timestamp(), current_timestamp()),
    (2, '研发部', 1, 1, true, '张三', '0755-88880001', 'dev@example.com', 'default', current_timestamp(), current_timestamp()),
    (3, '市场部', 1, 2, true, '李四', '0755-88880002', 'market@example.com', 'default', current_timestamp(), current_timestamp()),
    (4, '财务部', 1, 3, true, '王五', '0755-88880003', 'finance@example.com', 'default', current_timestamp(), current_timestamp())
;

-- 内置 4 个岗位
insert into sys_post(id, code, name, sort_order, enabled, tenant, created_time, modified_time) values
    (1, 'CEO', '董事长', 1, true, 'default', current_timestamp(), current_timestamp()),
    (2, 'PM', '项目经理', 2, true, 'default', current_timestamp(), current_timestamp()),
    (3, 'HR', '人力资源', 3, true, 'default', current_timestamp(), current_timestamp()),
    (4, 'STAFF', '普通员工', 4, true, 'default', current_timestamp(), current_timestamp())
;

insert into sys_role_menu_mapping(role_id, menu_id) values
    (1, 1), (1, 2), (1, 3), (1, 4),
    (2, 1), (2, 2)
;

-- admin / demo / frozen 的密码均为 123456(BCrypt 固定密文)，frozen 为禁用账号
insert into sys_user(id, username, password, nickname, enabled, dept_id, tenant, created_time, modified_time) values
    (1, 'admin', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Administrator', true, 1, 'default', current_timestamp(), current_timestamp()),
    (2, 'demo', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '演示用户', true, 2, 'default', current_timestamp(), current_timestamp()),
    (3, 'frozen', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '冻结用户', false, 2, 'default', current_timestamp(), current_timestamp())
;

insert into sys_user_role_mapping(user_id, role_id) values
    (1, 1),
    (2, 2),
    (3, 2)
;
