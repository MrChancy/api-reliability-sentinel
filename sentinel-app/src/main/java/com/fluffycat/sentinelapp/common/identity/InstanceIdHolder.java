package com.fluffycat.sentinelapp.common.identity;

import java.lang.management.ManagementFactory;

public class InstanceIdHolder {
    public static final String ID = ManagementFactory.getRuntimeMXBean().getName();
}
