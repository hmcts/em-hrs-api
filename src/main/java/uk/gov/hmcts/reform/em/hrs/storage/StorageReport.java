package uk.gov.hmcts.reform.em.hrs.storage;

import java.time.LocalDate;

public class StorageReport {

    public final LocalDate today;

    public final long cvpItemCount;
    public final long hrsCvpItemCount;

    public final long cvpItemCountToday;
    public final long hrsCvpItemCountToday;


    public StorageReport(
        LocalDate today,
        HrsSourceVsDestinationCounts cvpHrsCount
    ) {
        this.today = today;
        this.cvpItemCount = cvpHrsCount.sourceTotalItemCount;
        this.hrsCvpItemCount = cvpHrsCount.hrsTotalItemCount;
        this.cvpItemCountToday = cvpHrsCount.sourceCountToday;
        this.hrsCvpItemCountToday = cvpHrsCount.hrsItemCountToday;
    }

    public record HrsSourceVsDestinationCounts(
        long sourceTotalItemCount,
        long hrsTotalItemCount,
        long sourceCountToday,
        long hrsItemCountToday
    ) {
    }
}

