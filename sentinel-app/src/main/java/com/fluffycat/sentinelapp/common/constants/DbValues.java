package com.fluffycat.sentinelapp.common.constants;

public final class DbValues {
    private DbValues() {}

    public static final class ProbeStatus {
        private ProbeStatus() {}
        public static final String SUCCESS = "SUCCESS";
        public static final String FAIL = "FAIL";
    }

    public static final class AlertStatus {
        private AlertStatus() {}
        public static final String OPEN = "OPEN";
        public static final String ACK = "ACK";
        public static final String RESOLVED = "RESOLVED";
    }

    public static final class AlertType {
        private AlertType() {}
        public static final String ERROR_RATE = "ERROR_RATE";
    }

    public static final class AlertLevel {
        private AlertLevel() {}
        public static final String P1 = "P1";
    }

    public static final class NotifyChannel {
        private NotifyChannel() {}
        public static final String EMAIL = "EMAIL";
    }

    public static final class NotifyStatus {
        private NotifyStatus() {}
        public static final String SENT = "SENT";
        public static final String FAIL = "FAIL";
    }
}

