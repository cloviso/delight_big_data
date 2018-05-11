### kettle 使用时间戳增量实时更新同步数据



需求：如果源库（表）数据结构有时间戳字段，可以直接利用时间戳来查询最新一次的数据。如果源表没有时间戳字段，那需要设置，一般主键+插入时间戳作为id去写到零时表，再扫描临时表去更新到目标库，显然这样会影响源表数据结构，建议有时间戳的就用时间戳实现增量更新。

方法：需要设置变量，获取最大最后一次源表时间戳，再进行表插入更新。

实现：

将源 mysql18 上 market 中的 `tb_boc_exchange` 数据增量更新到目标mysql91集群ai 对应的表中。

源表结构如下：

```
CREATE TABLE `tb_boc_exchange` (
  `t_id` varchar(64) NOT NULL COMMENT '主键',
  `money_type` varchar(64) NOT NULL COMMENT '货币类型',
  `cash_buy` varchar(64) DEFAULT NULL COMMENT '现汇买入',
  `money_buy` varchar(64) DEFAULT NULL COMMENT '现钞买入',
  `cash_sale` varchar(64) DEFAULT NULL COMMENT '现汇卖出',
  `money_sale` varchar(64) DEFAULT NULL COMMENT '现钞卖出',
  `mid_discount` varchar(64) DEFAULT NULL COMMENT '中行折算价',
  `launch_dt` varchar(64) DEFAULT NULL COMMENT '发布时间',
  `CURRENT_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间戳',
  PRIMARY KEY (`t_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```



利用kettle 新建一个转化: 增量更新tb_boc_exchange.ktr 文件。

1).写一个输入表，连接源表数据库，查询该表最大时间戳，并命名为别名 maxsj

```
SELECT max(UNIX_TIMESTAMP(tbe.CURRENT_TIME)) maxsj FROM tb_boc_exchange tbe;
```

2).再把kettle中的‘设置变量’控件拖出来，作为表的输出，会得到该最后一次更新的时间戳，需要设置变量名为MAXSJ，并要设置到`Valid in the Java Virtual Machine` 中才能生效。

3).写源表输入，并执行SQL，得到新的增量数据，再插入更新到目标表，并勾选替换SQL语句变量，执行每一行。

```
SELECT
  t_id
, money_type
, cash_buy
, money_buy
, cash_sale
, money_sale
, mid_discount
, launch_dt
, `CURRENT_TIME`
FROM tb_boc_exchange tbe1 where  UNIX_TIMESTAMP(tbe1.CURRENT_TIME) >=${MAXSJ};
```

结论：相比全量更新执行效率高很多，是增量更新。

