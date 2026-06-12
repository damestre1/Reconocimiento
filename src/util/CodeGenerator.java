package util;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class CodeGenerator {

    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    private CodeGenerator() {
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static String accessRecordId() {
        return "ACC-" + System.currentTimeMillis()
                + "-" + SEQUENCE.incrementAndGet();
    }
}
