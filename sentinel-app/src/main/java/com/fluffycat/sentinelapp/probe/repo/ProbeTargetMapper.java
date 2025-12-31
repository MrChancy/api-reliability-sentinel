package com.fluffycat.sentinelapp.probe.repo;

import com.fluffycat.sentinelapp.domain.entity.target.TargetEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ProbeTargetMapper {

    @Select("""
        SELECT id, env, enabled, method, base_url, path, headers_json, body_json,
               interval_sec, timeout_ms, retries,
               next_probe_ts, lease_until, lease_owner
        FROM probe_target
        WHERE env = #{env}
          AND enabled = 1
          AND next_probe_ts <= NOW()
          AND (lease_until IS NULL OR lease_until < NOW())
        ORDER BY next_probe_ts ASC
        LIMIT #{limit}
        FOR UPDATE SKIP LOCKED
        """)
    List<TargetEntity> selectDueForUpdateSkipLocked(@Param("env") String env,
                                                    @Param("limit") int limit);

    @Update("""
        UPDATE probe_target
        SET lease_until = DATE_ADD(NOW(), INTERVAL #{leaseSec} SECOND),
            lease_owner = #{owner}
        WHERE id = #{id}
        """)
    int acquireLease(@Param("id") Long id,
                     @Param("leaseSec") int leaseSec,
                     @Param("owner") String owner);

    @Update("""
        UPDATE probe_target
        SET last_probe_ts = #{lastProbeTs},
            next_probe_ts = #{nextProbeTs},
            lease_until = NULL,
            lease_owner = NULL
        WHERE id = #{id}
          AND lease_owner = #{owner}
        """)
    int finishAndRelease(@Param("id") Long id,
                         @Param("owner") String owner,
                         @Param("lastProbeTs") LocalDateTime lastProbeTs,
                         @Param("nextProbeTs") LocalDateTime nextProbeTs);
}

