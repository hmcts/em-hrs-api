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

    @Value("${azure.storage.hrs.cvp-dest-blob-container-name}")
    private String hrsCvpContainer;

    @Value("${azure.storage.hrs.vh-dest-blob-container-name}")
    private String hrsVhContainer;

    @Value("${azure.storage.cvp.connection-string}")
    private String cvpConnectionString;

    @Value("${azure.storage.cvp.blob-container-reference}")
    private String cvpContainer;


    @Value("${azure.storage.vh.connection-string}")
    private String vhConnectionString;

    @Value("${azure.storage.vh.blob-container-name}")
    private String vhContainer;

    @Bean(name = "hrsCvpBlobContainerClient")
    public BlobContainerClient hrsCvpBlobContainerClient() {
        LOGGER.info("HRS ConnectionString: {}, HRS cvp Container: {} ", hrsConnectionString, hrsCvpContainer);
        return new BlobContainerClientBuilder()
            .connectionString(hrsConnectionString)
            .containerName(hrsCvpContainer)
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

    @Bean(name = "vhBlobContainerClient")
    public BlobContainerClient vhBlobContainerClient() {
        LOGGER.info("VH ConnectionString: {}, VH Container: {} ", vhConnectionString, vhContainer);
        return new BlobContainerClientBuilder()
            .connectionString(vhConnectionString)
            .containerName(vhContainer)
            .buildClient();
    }
}
