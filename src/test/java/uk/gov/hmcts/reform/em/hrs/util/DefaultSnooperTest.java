package uk.gov.hmcts.reform.em.hrs.util;

import org.junit.jupiter.api.Test;

class DefaultSnooperTest {

    private final DefaultSnooper defaultSnooper = new DefaultSnooper();

    @Test
    void test_snoop() {
        defaultSnooper.snoop("Test message");
    }

}
