package uk.gov.hmcts.reform.em.hrs.storage;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class StorageReport {

    public final LocalDate today;

    @JsonProperty("cvp-item-count")
    public final long cvpItemCount;

    @JsonProperty("hrs-cvp-item-count")
    public final long hrsCvpItemCount;

    @JsonProperty("cvp-item-count-today")
    public final long cvpItemCountToday;

    @JsonProperty("hrs-cvp-item-count-today")
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
