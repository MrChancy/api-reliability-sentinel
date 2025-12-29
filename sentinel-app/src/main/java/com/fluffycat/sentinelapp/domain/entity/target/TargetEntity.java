package com.fluffycat.sentinelapp.domain.entity.target;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * probe_target：探测目标配置（黑盒探测）
 * 说明：
 * - intervalSec/timeoutMs/retries：控制“怎么探测”
 * - windowSec/errorRateThreshold/failThreshold：控制“怎么判定告警”
 * - silencedUntil：控制“是否抑制通知”（但事件/告警仍可记录）
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("probe_target")
public class TargetEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 目标名称：用于 UI/列表展示与告警文案 */
    private String name;

    /** 是否启用：false 时 Probe 不探测、Alert 不计算（通常） */
    private Boolean enabled;

    /** HTTP 方法：GET/POST/... */
    private String method;

    /** 基础 URL */
    private String baseUrl;

    /** 路径：例如 /demo/ok /api/health */
    private String path;

    /** 请求头 JSON：存储为 JSON 字符串（例如 {"X-Env":"docker"}） */
    private String headersJson;

    /** 请求体 JSON：用于 POST/PUT（例如 {"foo":"bar"}），GET 可为空 */
    private String bodyJson;

    /**
     * 探测周期（秒）：Probe 调度器多久对该 target 发起一次探测
     */
    private Integer intervalSec;

    /** 探测超时（毫秒）：HTTP 请求的超时时间（connect+read 或整体） */
    private Integer timeoutMs;

    /** 重试次数：单次探测失败后再尝试几次（0 表示不重试） */
    private Integer retries;

    /**
     * 告警统计窗口（秒）：AlertJob 统计 now-windowSec ~ now 之间 probe_event 的总数与失败数
     * 用于计算 errorRate 等指标
     */
    private Integer windowSec;

    /**
     * 错误率阈值（百分比）：例如 50.00 表示 >=50% 即 breach
     * 一般与 windowSec 配合使用
     */
    private BigDecimal errorRateThreshold;

    /**
     * 失败次数阈值（预留/可选）：例如窗口内 fail>=3 也可触发
     */
    private Integer failThreshold;

    /**
     * 静默截止时间：在该时间之前不发送通知（但仍可继续探测/落库/聚合）
     */
    private LocalDateTime silencedUntil;

    /** 静默原因：维护/已知故障等，用于审计与告警详情 */
    private String silenceReason;

    /** 标签：用于路由/筛选（如 "payment,core" 或 "demo,error-rate"） */
    private String tags;

    /** 负责人：最简单的路由字段（可直接是邮箱或用户ID） */
    private String owner;

    /** 创建时间/更新时间：审计用途 */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 运行环境：local/docker/prod 等，用于路由/筛选/多环境隔离 */
    private String env;
}
