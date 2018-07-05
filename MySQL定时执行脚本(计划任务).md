##### MySQL定时执行脚本(计划任务)

第一步：要查看当前MySQL是否已开启事件调度器，可执行如下SQL：

```
SHOW VARIABLES LIKE 'event_scheduler'; 

若显示： 

+-----------------+-------+ 
| Variable_name   | Value | 
+-----------------+-------+ 
| event_scheduler | OFF   | 
+-----------------+-------+ 
则可执行 

SET GLOBAL event_scheduler = 1; //开启定时任务
```

第二步：创建存储过程，需要定时执行这个程序，就需要写定时事件，创建事件代码如下：

```
create event if not exists e_busi_daily_report
on schedule every 30 second   // 每隔30秒将执行存储过程proc_busi_report()
on completion preserve 
do call proc_busi_report('483fee44-a5ac-11e7-906c-000c29522290'); 
```

要想开启、关闭事件任务，执行如下代码：

```
开启事件任务： 

alter event e_busi_daily_report ON 
COMPLETION PRESERVE ENABLE; 

关闭事件任务 ：
alter event e_busi_daily_report ON 
COMPLETION PRESERVE DISABLE; 

```

