-- 定时任务模块表结构(随模块 jar 自带，主应用通过 classpath*:sql/*-schema.sql 通配加载)
-- 约束：模块之间不允许外键引用，模块内部建表顺序自洽

drop table sys_job if exists;

-- 定时任务定义(全局数据)：内存调度注册表以本表为唯一事实来源，重启后重建
-- status: 0 调度中 / 1 暂停
create table sys_job(
    id identity(100, 1) not null,
    name varchar(50) not null,
    cron varchar(50) not null,
    handler varchar(100) not null,
    param varchar(200),
    status integer not null default 0,
    memo varchar(200),
    created_time timestamp not null,
    modified_time timestamp not null
);
alter table sys_job
    add constraint business_key_sys_job
        unique(name);
