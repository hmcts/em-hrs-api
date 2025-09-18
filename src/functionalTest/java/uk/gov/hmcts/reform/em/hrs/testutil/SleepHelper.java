package uk.gov.hmcts.reform.em.hrs.testutil;

import java.util.concurrent.TimeUnit;

public class SleepHelper {

    private SleepHelper() {
    }

    public static void sleepForSeconds(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
