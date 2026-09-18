-- 系统管理模块种子数据。
-- 菜单不在 SQL 中预置：菜单的唯一代码来源是 SystemModuleProvider 的声明，
-- 启动时由 MenuSyncService 幂等同步进 sys_menu 并按声明绑定角色

insert into sys_role(id, code, name, description, tenant, created_time, modified_time) values
    (1, 'ADMIN', '管理员', '拥有全部权限的内置角色', 'default', current_timestamp(), current_timestamp()),
    (2, 'USER', '普通用户', '基础查看权限', 'default', current_timestamp(), current_timestamp())
;

-- admin / demo 的密码均为 123456(BCrypt 密文)
insert into sys_user(id, username, password, nickname, enabled, tenant, created_time, modified_time) values
    (1, 'admin', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'Administrator', true, 'default', current_timestamp(), current_timestamp()),
    (2, 'demo', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', '演示用户', true, 'default', current_timestamp(), current_timestamp())
;

insert into sys_user_role_mapping(user_id, role_id) values
    (1, 1),
    (2, 2)
;
