package uk.gov.hmcts.reform.em.hrs.storage;

import java.time.LocalDate;

public class StorageReport {

    public final LocalDate today;

    public final long cvpItemCount;
    public final long hrsCvpItemCount;

    public final long cvpItemCountToday;
    public final long hrsCvpItemCountToday;


    public final long vhItemCount;
    public final long hrsVhItemCount;

    public final long vhItemCountToday;
    public final long hrsVhItemCountToday;

    public StorageReport(
        LocalDate today,
        HrsSourceVsDestinationCounts cvpHrsCount,
        HrsSourceVsDestinationCounts vhHrsCount
    ) {
        this.today = today;
        this.cvpItemCount = cvpHrsCount.sourceTotalItemCount;
        this.hrsCvpItemCount = cvpHrsCount.hrsTotalItemCount;
        this.cvpItemCountToday = cvpHrsCount.sourceCountToday;
        this.hrsCvpItemCountToday = cvpHrsCount.hrsItemCountToday;

        this.vhItemCount = vhHrsCount.sourceTotalItemCount;
        this.hrsVhItemCount = vhHrsCount.hrsTotalItemCount;
        this.vhItemCountToday = vhHrsCount.sourceCountToday;
        this.hrsVhItemCountToday = vhHrsCount.hrsItemCountToday;
    }

    public record HrsSourceVsDestinationCounts(
        long sourceTotalItemCount,
        long hrsTotalItemCount,
        long sourceCountToday,
        long hrsItemCountToday
    ) {
    }
}

