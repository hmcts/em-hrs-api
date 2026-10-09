package uk.gov.hmcts.reform.em.hrs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class RecordingFilenameDto {

    @JsonProperty("folder-name")
    private final String folderName;

    private final Set<String> filenames;
}
