package uk.gov.hmcts.reform.em.hrs.functional.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
<<<<<<<< HEAD:src/functionalTest/java/uk/gov/hmcts/reform/em/hrs/functional/config/HrsAzureClient.java

import static org.slf4j.LoggerFactory.getLogger;
========
>>>>>>>> refactored and fixed functional tests junit version issue:src/aat/java/uk/gov/hmcts/reform/em/hrs/config/HrsAzureClient.java

@Component
public class HrsAzureClient {

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
        return new BlobContainerClientBuilder()
            .connectionString(hrsConnectionString)
            .containerName(hrsContainer)
            .buildClient();
    }

    @Bean(name = "cvpBlobContainerClient")
    public BlobContainerClient cvpBlobContainerClient() {
        return new BlobContainerClientBuilder()
            .connectionString(cvpConnectionString)
            .containerName(cvpContainer)
            .buildClient();
    }
}
