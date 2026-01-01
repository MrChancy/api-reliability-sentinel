package com.fluffycat.sentinelapp.dashboard.repo;

import com.fluffycat.sentinelapp.domain.entity.target.TargetEntity;
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
}
