package uk.gov.hmcts.reform.em.hrs.util;

public interface Snooper {
    void snoop(String message);

    void snoop(String message, Throwable throwable);
}
