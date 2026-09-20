-- 系统管理模块种子数据。
-- 菜单不在 SQL 中预置：菜单的唯一代码来源是 SystemModuleProvider 的声明，
-- 启动时由 MenuSyncService 幂等同步进 sys_menu 并按声明绑定角色

insert into sys_role(id, code, name, description, tenant, created_time, modified_time) values
    (1, 'ADMIN', '管理员', '拥有全部权限的内置角色', 'default', current_timestamp(), current_timestamp()),
    (2, 'USER', '普通用户', '基础查看权限', 'default', current_timestamp(), current_timestamp())
;

-- 默认部门树：总公司 + 三个部门(挂在总公司下)
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

-- admin / demo 的密码均为 123456(BCrypt 密文)
insert into sys_user(id, username, password, nickname, enabled, dept_id, tenant, created_time, modified_time) values
    (1, 'admin', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Administrator', true, 1, 'default', current_timestamp(), current_timestamp()),
    (2, 'demo', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '演示用户', true, 2, 'default', current_timestamp(), current_timestamp())
;

insert into sys_user_role_mapping(user_id, role_id) values
    (1, 1),
    (2, 2)
;
