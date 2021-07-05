package uk.gov.hmcts.reform.em.hrs.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class BlobInfo {

    long fileSize;
    String contentType;
}
