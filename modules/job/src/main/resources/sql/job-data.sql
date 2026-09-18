-- 定时任务模块种子数据：一条暂停状态的示例任务，可在界面启停/立即执行
insert into sys_job(id, name, cron, handler, param, status, memo, created_time, modified_time) values
    (1, '示例日志任务', '0 * * * * ?', 'sampleLogJob', 'hello', 1, '每分钟打印一行日志的演示任务(默认暂停)', current_timestamp(), current_timestamp())
;
