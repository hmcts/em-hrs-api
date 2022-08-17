package uk.gov.hmcts.reform.em.hrs.data.migration;

public class PendingMigrationScriptException extends RuntimeException {

    public PendingMigrationScriptException(String script) {
        super("Found migration not yet applied: " + script);
    }
}
