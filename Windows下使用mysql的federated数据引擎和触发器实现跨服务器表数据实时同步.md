# Windows下使用mysql的federated数据引擎和触发器实现跨服务器表数据实时同步



> 使用 Mysql 引擎 Federated，它可以实现操作数据库源的一张表，目标数据库对应一张表也同步。
>

**目的**：实现跨服务器数据库表数据同步，当远程数据库的一张表数据增删改时，另一个系统的数据表也即时同步。

**环境**：Windows10 操作系统 、MySQL57

**步骤**：

1. 先配置mysql的federated，默认mysql5.7以上都不是默认开启该数据引擎的，所以手动配置，在mysql的根目录下找到隐藏的配置文件my.ini, 然后在mysqld 中添加federated即可。然后用命令 show engines; 查看下：


```
mysql> show engines;
+--------------------+---------+----------------------------------------------------------------+--------------+------+------------+
| Engine             | Support | Comment                                                        | Transactions | XA   | Savepoints |
+--------------------+---------+----------------------------------------------------------------+--------------+------+------------+
| InnoDB             | DEFAULT | Supports transactions, row-level locking, and foreign keys     | YES          | YES  | YES        |
| MRG_MYISAM         | YES     | Collection of identical MyISAM tables                          | NO           | NO   | NO         |
| MEMORY             | YES     | Hash based, stored in memory, useful for temporary tables      | NO           | NO   | NO         |
| BLACKHOLE          | YES     | /dev/null storage engine (anything you write to it disappears) | NO           | NO   | NO         |
| MyISAM             | YES     | MyISAM storage engine                                          | NO           | NO   | NO         |
| CSV                | YES     | CSV storage engine                                             | NO           | NO   | NO         |
| ARCHIVE            | YES     | Archive storage engine                                         | NO           | NO   | NO         |
| PERFORMANCE_SCHEMA | YES     | Performance Schema                                             | NO           | NO   | NO         |
| FEDERATED          | YES     | Federated MySQL storage engine                                 | NO           | NO   | NO         |
+--------------------+---------+----------------------------------------------------------------+--------------+------+------------+
9 rows in set (0.00 sec)
```

如果显示YES 则开启成功。

2.下一步就是在源端创建一张表，然后目的端数据结构一定要保持一致，名字可以不一样。

```
DROP TABLE IF EXISTS `tb_students`;
CREATE TABLE `tb_students` (
  `u_id` varchar(20) NOT NULL,
  `u_name` varchar(20) DEFAULT NULL,
  `u_gender` char(8) DEFAULT NULL,
  `u_height` float(8,1) DEFAULT NULL,
  `u_weight` float(8,1) DEFAULT NULL,
  PRIMARY KEY (`u_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

3.再创建一张中间配置表，就是用来连接远程服务器。

```
CREATE TABLE `fed_students` (  
   `u_id` varchar(20) NOT NULL AUTO_INCREMENT,  
  `u_name` varchar(20) DEFAULT NULL,  
  `u_gender` char(8) DEFAULT NULL,
  `u_height` float(8,1) DEFAULT NULL,
  `u_weight` float(8,1) DEFAULT NULL,
  PRIMARY KEY (`u_id`)
) ENGINE=FEDERATED DEFAULT CHARSET=utf8  CONNECTION='mysql://用户名:密码@ip:端口/test/tb_students';
```

4.再手动写个插入触发器，把源端学生表tb_students的数据插入自动触发到中间表fed_students中

```
- 创建学生插入触发器 在插入后更新--
DELIMITER $$  
  
CREATE  
    TRIGGER `students_insert_trigger` AFTER INSERT ON `tb_students`   
    FOR EACH ROW BEGIN  
    INSERT INTO `fed_students`(  
`u_id`,   
`u_name`,   
`u_gender`,
`u_height`,
`u_weight`  
) VALUES (  
new.`u_id`,   
new.`u_name`,   
new.`u_gender`,
new.`u_height`,
new.`u_weight`
    );  
  
    END;  
$$  
  
DELIMITER ;  
```

同样在修改时候也需要写触发器 ：给学生表添加更新触发器 在修改已经存在的数据记录之后，会触发更新到零时表fed_students

```
DELIMITER $$

create trigger students_update_trigger

after update on tb_students for each row begin

UPDATE fed_students SET u_name=NEW.u_name WHERE u_id=old.u_id; 
END;
$$

DELIMITER ;
```

**测试数据对比时间**

为了测试插入和更新所需要的时间，我们批量插入10万条数据试了下，我采用存储过程插入10万条：

```
delimiter ;;
create procedure myproc (IN num Int)

begin
declare @num int ;
set num = 1 ;
while num < 100000 do
    insert into tb_students (u_id, u_name, u_gender,u_height,u_weight)
values
    (num, concat("u_name", num), 1,1,1) ;
set num = num + 1 ;
end
while ;

end;;
```

写好存储过程后，用命令调用：

```
mysql> call myproc();//调用存储过程
mysql> select * from tb_students;
+------+--------------------------------+----------+----------+----------+
| u_id | u_name                         | u_gender | u_height | u_weight |
+------+--------------------------------+----------+----------+----------+
|      | u_name1                        | 1        |      1.0 |      1.0 |
| 1    | yang1                          | female   |    160.0 |     50.0 |
| 10   | yang10                         | female   |    155.3 |     48.2 |
| 11   | yang11                         | male     |    167.8 |     62.1 |
| 12   | 杨大猫                         | female   |    180.0 |     55.0 |
| 13   | 杨大猫13                       | male     |     88.0 |    599.0 |
| 14   | yangyang14                     | male     | 123456.0 | 789654.0 |
| 18   | 猫猫猫是已经秃顶的猫           | male     |    123.5 |     74.6 |
| 19   | yang8                          | female88 |    162.1 |     43.1 |
| 2    | yang2                          | male     |    173.1 |     62.2 |
| 20   | 杨二狗测试qqq                  | female   |    888.6 |    888.2 |
| 21   | 杨大傻111                      | female   |     88.3 |     99.7 |
| 3    | yang3                          | male     |    184.2 |     71.2 |
| 4    | yang4                          | female   |    163.3 |     52.7 |
| 5    | yang5                          | female   |    156.4 |     49.0 |
| 6    | yang6                          | male     |    183.1 |     80.7 |
| 7    | yang7                          | male     |    179.6 |     82.5 |
| 9    | yang9                          | male     |    172.0 |     53.0 |
| num  | u_name1                        | 1        |      1.0 |      1.0 |
+------+--------------------------------+----------+----------+----------+
19 rows in set (0.00 sec)
mysql> drop procedure myproc;

```

这个方法对比插入更新时间如下：

```
mysql> call myproc();
Query OK, 1 row affected (53 min 35.73 sec)
```



当然这个方法也有缺点，通过查15.8.3 FEDERATED Storage Engine Notes and Tips文档，主要缺点如下：

- 不支持事务操作，最好目的端只查只读，因为修改数据双向都会变化保持一致，会影响业务数据
- 不支持按照索引查询
- 表结构目的端必须要和源端一致，不支持修改表结构
- 只能一对一对表进行操作，逐一创建比较麻烦

查询倒是蛮快的，我查询目标表数据89999条花了0.37seconds

```
| 99982 | u_name99982 | 1        |      1.0 |      1.0 |
| 99983 | u_name99983 | 1        |      1.0 |      1.0 |
| 99984 | u_name99984 | 1        |      1.0 |      1.0 |
| 99985 | u_name99985 | 1        |      1.0 |      1.0 |
| 99986 | u_name99986 | 1        |      1.0 |      1.0 |
| 99987 | u_name99987 | 1        |      1.0 |      1.0 |
| 99988 | u_name99988 | 1        |      1.0 |      1.0 |
| 99989 | u_name99989 | 1        |      1.0 |      1.0 |
| 99990 | u_name99990 | 1        |      1.0 |      1.0 |
| 99991 | u_name99991 | 1        |      1.0 |      1.0 |
| 99992 | u_name99992 | 1        |      1.0 |      1.0 |
| 99993 | u_name99993 | 1        |      1.0 |      1.0 |
| 99994 | u_name99994 | 1        |      1.0 |      1.0 |
| 99995 | u_name99995 | 1        |      1.0 |      1.0 |
| 99996 | u_name99996 | 1        |      1.0 |      1.0 |
| 99997 | u_name99997 | 1        |      1.0 |      1.0 |
| 99998 | u_name99998 | 1        |      1.0 |      1.0 |
| 99999 | u_name99999 | 1        |      1.0 |      1.0 |
+-------+-------------+----------+----------+----------+
89999 rows in set (0.37 sec)
```



插入1000条数据花时间36.88 sec，1000藕条数据需要花费

```
// 插入一千条数据
mysql> call myproc();
Query OK, 1 row affected (36.88 sec)
// 插入一万条数据
mysql> call myproc();
Query OK, 1 row affected (5 min 3.42 sec)
```

| options | records | times       |
| ------- | ------- | ----------- |
|         | 1000    | 36.88s'     |
| insert  | 10000   | 5m'3.24s'   |
|         | 100000  | 53m'35.73s' |



MySQL实时性比较高的数据同步解决方案：

1、Triggers：不跨服务器，不同库，同表插入更新同步，使用触发器可以实现。

2、主从复制：跨服务器同步实时数据，需要通过工具和插件，可以搭主从备份。MySQL Replication只能实现主从机上库对库的数据同步，没法将主机上的库中单独的一张表同步。

3、另外触发器不便于管理和以后维护，HBASE和Redis都没有基于表的触发器。

4、要么业务自己主动双写实时备份。

5、federated引擎：用mysql的federated数据引擎插件和触发器实现跨服务器表数据同步。

源数据库增加触发器后，把数据同步到federated 表中，此刻federated表会通过一个可访问目的数据库的账号把数据同步到目的数据库的对应表中。

6、Event Scheduler ：用kettle设置定时任务job。

目前1、5、6已经测试过了 理论上可以实现。