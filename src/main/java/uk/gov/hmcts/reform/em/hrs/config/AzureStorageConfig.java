package uk.gov.hmcts.reform.em.hrs.config;

import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
public class AzureStorageConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStorageConfig.class);

    @Value("${azure.storage.hrs.connection-string}")
    private String hrsConnectionString;

    @Value("${azure.storage.cvp.connection-string}")
    private String cvpConnectionString;

    @Value("${azure.storage.hrs.blob-container-reference}")
    private String hrsContainer;

    @Value("${azure.storage.cvp.blob-container-reference}")
    private String cvpContainer;

    @Bean
    public BlobContainerAsyncClient provideBlobContainerAsyncClient() {
        final BlobContainerAsyncClient blobContainerAsyncClient = new BlobContainerClientBuilder()
            .connectionString(hrsConnectionString)
            .containerName(hrsContainer)
            .buildAsyncClient();

        final boolean containerExists = Optional.ofNullable(blobContainerAsyncClient.exists().block())
            .orElse(false);

        if (!containerExists) {
            blobContainerAsyncClient.create()
                .subscribe(
                    response -> LOGGER.info("Create {} container completed", hrsContainer),
                    error -> LOGGER.error("Error while creating container {}::: ", hrsContainer, error)
                );
        }
        return blobContainerAsyncClient;
    }

    @Bean("HrsBlobContainerClient")
    public BlobContainerClient provideBlobContainerClient() {
        return new BlobContainerClientBuilder()
            .connectionString(hrsConnectionString)
            .containerName(hrsContainer)
            .buildClient();
    }

}
