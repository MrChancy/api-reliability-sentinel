CREATE DATABASE IF NOT EXISTS api_sentinel DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE api_sentinel;

-- 1) 探测目标
CREATE TABLE IF NOT EXISTS probe_target (
                                            id              BIGINT PRIMARY KEY AUTO_INCREMENT,
                                            name            VARCHAR(128) NOT NULL,
    enabled         TINYINT NOT NULL DEFAULT 1,

    method          VARCHAR(10) NOT NULL DEFAULT 'GET',
    base_url        VARCHAR(255) NOT NULL,
    path            VARCHAR(255) NOT NULL DEFAULT '/',
    headers_json    JSON NULL,
    body_json       JSON NULL,

    interval_sec    INT NOT NULL DEFAULT 60,
    timeout_ms      INT NOT NULL DEFAULT 1000,
    retries         INT NOT NULL DEFAULT 0,

    window_sec              INT NOT NULL DEFAULT 300,
    error_rate_threshold    DECIMAL(5,2) NULL,      -- 50.00 表示 50%
    fail_threshold          INT NOT NULL DEFAULT 3,  -- 预留：未来做 DOWN/连续失败

-- 静默（演示“维护期不打扰，但仍记录事件/告警”）
    silenced_until  DATETIME NULL,
    silence_reason  VARCHAR(255) NULL,

    env             VARCHAR(16) NOT NULL DEFAULT 'docker' COMMENT '运行环境：local/docker',
    tags            VARCHAR(255) NULL,
    owner           VARCHAR(64) NULL,

    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB;

CREATE INDEX idx_probe_target_env ON probe_target(env);
CREATE INDEX idx_probe_target_owner ON probe_target(owner);
CREATE INDEX idx_probe_target_enabled ON probe_target(enabled);

-- 2) 探测事件
CREATE TABLE IF NOT EXISTS probe_event (
                                           id          BIGINT PRIMARY KEY AUTO_INCREMENT,
                                           target_id   BIGINT NOT NULL,
                                           ts          DATETIME NOT NULL,

                                           status      VARCHAR(16) NOT NULL,     -- SUCCESS / FAIL
    http_code   INT NULL,
    rt_ms       INT NULL,

    error_type  VARCHAR(64) NULL,         -- TIMEOUT / DNS / CONN / 5XX / 4XX / UNKNOWN
    error_msg   VARCHAR(512) NULL,

    trace_id    VARCHAR(64) NULL,
    sample      JSON NULL,

    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_probe_event_target FOREIGN KEY (target_id) REFERENCES probe_target(id)
    ) ENGINE=InnoDB;

CREATE INDEX idx_probe_event_target_ts ON probe_event(target_id, ts);
CREATE INDEX idx_probe_event_ts ON probe_event(ts);
CREATE INDEX idx_probe_event_status ON probe_event(status);

-- 3) 告警事件（ERROR_RATE 为 M1 主类型）
CREATE TABLE IF NOT EXISTS alert_event (
                                           id              BIGINT PRIMARY KEY AUTO_INCREMENT,
                                           target_id       BIGINT NOT NULL,

                                           alert_type      VARCHAR(32) NOT NULL,      -- ERROR_RATE (M1)
    alert_level     VARCHAR(8)  NOT NULL,      -- P1 (M1固定)
    dedupe_key      VARCHAR(128) NOT NULL,     -- hash(target_id + alert_type)
    status          VARCHAR(16) NOT NULL,      -- OPEN / ACK / RESOLVED

    first_seen_ts   DATETIME NOT NULL,
    last_seen_ts    DATETIME NOT NULL,
    last_sent_ts    DATETIME NULL,

    count_in_window INT NOT NULL DEFAULT 1,
    summary         VARCHAR(512) NULL,
    details_json    JSON NULL,

    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_alert_event_target FOREIGN KEY (target_id) REFERENCES probe_target(id),
    UNIQUE KEY uk_alert_dedupe_status (dedupe_key)
    ) ENGINE=InnoDB;

CREATE INDEX idx_alert_target_status ON alert_event(target_id, status);
CREATE INDEX idx_alert_last_seen ON alert_event(last_seen_ts);

-- 4) 通知日志（Email）
CREATE TABLE IF NOT EXISTS notify_log (
                                          id          BIGINT PRIMARY KEY AUTO_INCREMENT,
                                          alert_id    BIGINT NOT NULL,
                                          channel     VARCHAR(32) NOT NULL,      -- EMAIL
    receiver    VARCHAR(255) NOT NULL,
    status      VARCHAR(16) NOT NULL,      -- SENT / FAIL
    error_msg   VARCHAR(512) NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notify_alert FOREIGN KEY (alert_id) REFERENCES alert_event(id)
    ) ENGINE=InnoDB;

CREATE INDEX idx_notify_alert ON notify_log(alert_id);

---Alter Table
ALTER TABLE probe_target
  ADD COLUMN next_probe_ts DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN last_probe_ts DATETIME NULL,
  ADD COLUMN lease_until  DATETIME NULL,
  ADD COLUMN lease_owner  VARCHAR(64) NULL;

CREATE INDEX idx_probe_due ON probe_target(env, enabled, next_probe_ts, lease_until);
CREATE INDEX idx_alert_status_last_seen on alert_event(status,last_seen_ts);
CREATE INDEX idx_probe_event_agg ON probe_event(target_id,stauts,ts);
