package uk.gov.hmcts.reform.em.hrs.storage;

import java.io.OutputStream;

public interface BlobstoreClient {

    void downloadFile(String filename, OutputStream output);
}
