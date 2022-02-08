package uk.gov.hmcts.reform.em.hrs.storage;

public class StorageReport {

    public final long cvpItemCount;
    public final long hrsItemCount;

    public StorageReport(long cvpItemCount, long hrsItemCount) {
        this.cvpItemCount = cvpItemCount;
        this.hrsItemCount = hrsItemCount;
    }
}
