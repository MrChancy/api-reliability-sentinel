### 告警算法（MVP）

* 输入：某 target 在 windowSec 内的探测样本
* 规则：

    * DOWN：失败次数 >= fail_threshold
    * RT_SPIKE：P95 >= sla_rt_ms（或均值超阈）
* 输出：alert_event（OPEN/UPDATE/RESOLVED）

### 去重与聚合

* dedupeKey = hash(target_id + alert_type + error_type)
* dedupeWindowSec 内同 dedupeKey 只发一次（其余合并计数）
* quietHours/静默：命中静默策略则不发通知，但仍落库

### 通知内容模板

* 标题：`[P1][DOWN] service=xxx endpoint=/api/v1/order`
* 关键字段：最近 10 次样本、失败比例、P95 RT、traceId 示例
* 建议动作：检查依赖、检查近期发布、查看追踪/日志链接（占位）