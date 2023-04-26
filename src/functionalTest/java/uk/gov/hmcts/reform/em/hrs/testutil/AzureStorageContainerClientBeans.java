package uk.gov.hmcts.reform.em.hrs.testutil;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class AzureStorageContainerClientBeans {

    private static final Logger LOGGER = getLogger(AzureStorageContainerClientBeans.class);

    @Value("${azure.storage.hrs.connection-string}")
    private String hrsConnectionString;

    @Value("${azure.storage.hrs.blob-container-reference}")
    private String hrsContainer;

    @Value("${azure.storage.cvp.connection-string}")
    private String cvpConnectionString;

    @Value("${azure.storage.cvp.blob-container-reference}")
    private String cvpContainer;

    @Bean(name = "hrsBlobContainerClient")
    public BlobContainerClient hrsBlobContainerClient() {
        LOGGER.info("HRS ConnectionString: {}, HRS Container: {} ", hrsConnectionString, hrsContainer);
        return new BlobContainerClientBuilder()
            .connectionString(hrsConnectionString)
            .containerName(hrsContainer)
            .buildClient();
    }

    @Bean(name = "cvpBlobContainerClient")
    public BlobContainerClient cvpBlobContainerClient() {
        LOGGER.info("CVP ConnectionString: {}, CVP Container: {} ", cvpConnectionString, cvpContainer);
        return new BlobContainerClientBuilder()
            .connectionString(cvpConnectionString)
            .containerName(cvpContainer)
            .buildClient();
    }
}
