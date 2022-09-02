package uk.gov.hmcts.reform.em.hrs.storage;

import java.time.LocalDate;

public class StorageReport {

    public final LocalDate today;

    public final long cvpItemCount;
    public final long hrsItemCount;

    public final long cvpItemCountToday;
    public final long hrsItemCountToday;

    public StorageReport(
        LocalDate today,
        long cvpItemCount,
        long hrsItemCount,
        long cvpItemCountToday,
        long hrsItemCountToday
    ) {
        this.today = today;
        this.cvpItemCount = cvpItemCount;
        this.hrsItemCount = hrsItemCount;
        this.cvpItemCountToday = cvpItemCountToday;
        this.hrsItemCountToday = hrsItemCountToday;
    }

}

