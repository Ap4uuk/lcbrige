package ru.ap4uuk.lcbridge;

import java.util.concurrent.atomic.AtomicBoolean;

public class LCBridgeSyncGuard {

    private static final AtomicBoolean SYNCING = new AtomicBoolean(false);

    public static void setSyncing(boolean state) {
        SYNCING.set(state);
    }

    public static boolean isSyncing() {
        return SYNCING.get();
    }
}
