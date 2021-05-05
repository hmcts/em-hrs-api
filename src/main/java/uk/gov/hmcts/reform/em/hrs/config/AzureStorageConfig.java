package uk.gov.hmcts.reform.em.hrs.config;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.apache.commons.lang3.StringUtils;
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

    @Bean
    public BlobContainerAsyncClient provideBlobContainerAsyncClient() {

        BlobContainerClientBuilder blobContainerAsyncClientBuilder = new BlobContainerClientBuilder()
            .connectionString(hrsConnectionString)
            .containerName(hrsContainer);

        boolean isACvpEndpointUrl =
            cvpConnectionString.contains("cvprecordings") && !cvpConnectionString.contains("AccountName");

        if (isACvpEndpointUrl) {
            LOGGER.info("****************************");
            LOGGER.info("Using Managed Identity");
            LOGGER.info("cvp end point: {}", cvpConnectionString);
            LOGGER.info("cvp container name: n/a inferred from sourceUrl");
            LOGGER.info(
                "Building client with default credential builder / managed identity");
            LOGGER.info("****************************");

            DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();
            blobContainerAsyncClientBuilder.credential(credential);
        } else {
            LOGGER.info("****************************");
            LOGGER.info("Not a CVP endpoint - cvpConnectionString(60): {} ", StringUtils.left(cvpConnectionString, 60));
            LOGGER.info("****************************");

        }


        final BlobContainerAsyncClient blobContainerAsyncClient = blobContainerAsyncClientBuilder.buildAsyncClient();

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
