package com.fluffycat.sentinelapp.dashboard.repo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fluffycat.sentinelapp.domain.entity.target.TargetEntity;
import com.fluffycat.sentinelapp.domain.enums.alert.AlertEventStatus;
import lombok.Getter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DashboardMapper {

    @Select("""
        SELECT *
        FROM probe_target
        WHERE env = #{env}
        ORDER BY id DESC
        """)
    List<TargetEntity> selectTargetsByEnv(@Param("env") String env);

    @Select("""
        SELECT target_id AS targetId,
               COUNT(*) AS totalCnt,
               SUM(CASE WHEN status = 'FAIL' THEN 1 ELSE 0 END) AS failCnt,
               MAX(ts) AS lastProbeTs
        FROM probe_event
        WHERE target_id IN (${targetIdsCsv})
          AND ts >= #{startTs}
          AND ts < #{endTs}
        GROUP BY target_id
        """)
    List<TargetAggRow> selectTargetAgg5m(@Param("targetIdsCsv") String targetIdsCsv,
                                         @Param("startTs") LocalDateTime startTs,
                                         @Param("endTs") LocalDateTime endTs);

    @Select("""
        WITH w AS (
          SELECT target_id,
                 rt_ms,
                 CUME_DIST() OVER (PARTITION BY target_id ORDER BY rt_ms) AS cd
          FROM probe_event
          WHERE target_id IN (${targetIdsCsv})
            AND ts >= #{startTs}
            AND ts < #{endTs}
            AND rt_ms IS NOT NULL
        )
        SELECT target_id AS targetId,
               MAX(CASE WHEN cd <= 0.95 THEN rt_ms END) AS p95RtMs
        FROM w
        GROUP BY target_id
        """)
    List<TargetP95Row> selectTargetP95_5m(@Param("targetIdsCsv") String targetIdsCsv,
                                          @Param("startTs") LocalDateTime startTs,
                                          @Param("endTs") LocalDateTime endTs);

    @Select("""
        WITH x AS (
          SELECT target_id,
                 status,
                 last_seen_ts,
                 last_sent_ts,
                 ROW_NUMBER() OVER (PARTITION BY target_id ORDER BY last_seen_ts DESC) AS rn
          FROM alert_event
          WHERE target_id IN (${targetIdsCsv})
        )
        SELECT target_id AS targetId,
               status,
               last_seen_ts AS lastSeenTs,
               last_sent_ts AS lastSentTs
        FROM x
        WHERE rn = 1
        """)
    List<TargetAlertRow> selectLatestAlertByTargets(@Param("targetIdsCsv") String targetIdsCsv);

    @Select("""
    SELECT
      a.id            AS alertId,
      a.target_id     AS targetId,
      a.alert_type    AS alertType,
      a.alert_level   AS alertLevel,
      a.status        AS status,
      a.first_seen_ts AS firstSeenTs,
      a.last_seen_ts  AS lastSeenTs,
      a.last_sent_ts  AS lastSentTs,
      a.count_in_window AS countInWindow,
      a.summary       AS summary,

      t.name          AS targetName,
      t.method        AS method,
      t.base_url      AS baseUrl,
      t.path          AS path,
      t.owner         AS owner,
      t.silenced_until AS silencedUntil
    FROM alert_event a
    JOIN probe_target t ON t.id = a.target_id
    WHERE t.env = #{env}
      AND (#{status} IS NULL OR #{status} = '' OR a.status = #{status})
    ORDER BY a.last_seen_ts DESC
    LIMIT #{limit} OFFSET #{offset}
    """)
    Page<AlertOverviewRow> selectAlertsOverview(IPage<?> page,
                                                @Param("env") String env,
                                                @Param("status") AlertEventStatus status);

    @Select("""
    SELECT
      FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(ts) / #{bucketSec}) * #{bucketSec}) AS bucketTs,
      COUNT(*) AS totalCnt,
      SUM(CASE WHEN status = 'FAIL' THEN 1 ELSE 0 END) AS failCnt,
      CAST(AVG(rt_ms) AS SIGNED) AS avgRtMs
    FROM probe_event
    WHERE target_id = #{targetId}
      AND ts >= #{startTs}
      AND ts < #{endTs}
    GROUP BY bucketTs
    ORDER BY bucketTs ASC
    """)
    List<TimeseriesAggRow> selectTargetTimeseriesAgg(@Param("targetId") Long targetId,
                                                     @Param("startTs") LocalDateTime startTs,
                                                     @Param("endTs") LocalDateTime endTs,
                                                     @Param("bucketSec") int bucketSec);

    @Select("""
    WITH w AS (
      SELECT
        FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(ts) / #{bucketSec}) * #{bucketSec}) AS bucketTs,
        rt_ms,
        CUME_DIST() OVER (
          PARTITION BY FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(ts) / #{bucketSec}) * #{bucketSec})
          ORDER BY rt_ms
        ) AS cd
      FROM probe_event
      WHERE target_id = #{targetId}
        AND ts >= #{startTs}
        AND ts < #{endTs}
        AND rt_ms IS NOT NULL
    )
    SELECT
      bucketTs,
      MAX(CASE WHEN cd <= 0.95 THEN rt_ms END) AS p95RtMs
    FROM w
    GROUP BY bucketTs
    ORDER BY bucketTs ASC
    """)
    List<TimeseriesP95Row> selectTargetTimeseriesP95(@Param("targetId") Long targetId,
                                                     @Param("startTs") LocalDateTime startTs,
                                                     @Param("endTs") LocalDateTime endTs,
                                                     @Param("bucketSec") int bucketSec);


    @Select("""
    SELECT
      COALESCE(error_type, 'UNKNOWN') AS errorType,
      COUNT(*) AS cnt
    FROM probe_event
    WHERE target_id = #{targetId}
      AND ts >= #{startTs}
      AND ts < #{endTs}
      AND status = 'FAIL'
    GROUP BY COALESCE(error_type, 'UNKNOWN')
    ORDER BY cnt DESC
    """)
    List<ErrorTypeBreakdownRow> selectErrorTypeBreakdown(@Param("targetId") Long targetId,
                                                         @Param("startTs") LocalDateTime startTs,
                                                         @Param("endTs") LocalDateTime endTs);



    @Getter
    class TargetAggRow {
        private Long targetId;
        private Long totalCnt;
        private Long failCnt;
        private LocalDateTime lastProbeTs;
    }

    @Getter
    class TargetP95Row {
        private Long targetId;
        private Integer p95RtMs;
    }

    @Getter
    class TargetAlertRow {
        private Long targetId;
        private String status;
        private LocalDateTime lastSeenTs;
        private LocalDateTime lastSentTs;
    }

    @Getter
    class AlertOverviewRow {
        private Long alertId;
        private Long targetId;

        private String alertType;
        private String alertLevel;
        private String status;

        private LocalDateTime firstSeenTs;
        private LocalDateTime lastSeenTs;
        private LocalDateTime lastSentTs;

        private Integer countInWindow;
        private String summary;

        private String targetName;
        private String method;
        private String baseUrl;
        private String path;

        private String owner;
        private LocalDateTime silencedUntil;
    }
    @Getter
    class TimeseriesAggRow {
        private LocalDateTime bucketTs;
        private Long totalCnt;
        private Long failCnt;
        private Integer avgRtMs;
    }
    @Getter
    class TimeseriesP95Row {
        private LocalDateTime bucketTs;
        private Integer p95RtMs;
    }
    @Getter
    class ErrorTypeBreakdownRow {
        private String errorType;
        private Long cnt;
    }



}
