### 表设计

**probe_target**

* id, name, base_url, path, method, enabled
* interval_sec, timeout_ms, retries
* sla_rt_ms, fail_threshold, window_sec
* notify_policy_id

**probe_event**

* id, target_id, ts
* status (SUCCESS/FAIL), http_code
* rt_ms, error_type, error_msg
* trace_id, sample_payload (可选)

**alert_event**

* id, target_id, first_seen_ts, last_seen_ts
* alert_level (P0/P1/P2), alert_type (DOWN/RT_SPIKE/ERROR_RATE)
* dedupe_key, count, summary
* status (OPEN/ACK/RESOLVED), silence_id (可选)

**notify_policy**

* id, channels, receivers, quiet_hours, escalation (可选)

### 索引

* `probe_event(target_id, ts)`
* `alert_event(dedupe_key, status, last_seen_ts)`