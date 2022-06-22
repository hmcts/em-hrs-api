package uk.gov.hmcts.reform.em.hrs.config;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.em.hrs.util.CvpConnectionResolver;

import java.util.Optional;

@Configuration
public class AzureStorageConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStorageConfig.class);

    @Value("${azure.storage.hrs.connection-string}")
    private String hrsConnectionString;

    @Value("${azure.storage.hrs.blob-container-reference}")
    private String hrsContainer;

    @Value("${azure.storage.cvp.connection-string}")
    private String cvpConnectionString;

    @Value("${azure.storage.cvp.blob-container-reference}")
    private String cvpContainer;

    @Bean("HrsBlobContainerClient")
    public BlobContainerClient provideBlobContainerClient() {
        BlobContainerClient blobContainerClient = new BlobContainerClientBuilder()
            .connectionString(hrsConnectionString)
            .containerName(hrsContainer)
            .buildClient();




        final boolean containerExists = Optional.ofNullable(blobContainerClient.exists())
            .orElse(false);

        if (!containerExists) {
            LOGGER.info("Creating container {} in HRS Storage", hrsContainer);
            blobContainerClient.create();
        }
        return blobContainerClient;

    }


    @Bean("CvpBlobContainerClient")
    public BlobContainerClient provideCvpBlobContainerClient() {
        BlobContainerClientBuilder b = new BlobContainerClientBuilder();


        if (CvpConnectionResolver.isACvpEndpointUrl(cvpConnectionString)) {
            LOGGER.info("****************************");
            LOGGER.info("Using Managed Identity For Cvp Blob Container Client (For SAS Token Generation)");
            LOGGER.info("cvp end point: {}", cvpConnectionString);
            LOGGER.info("cvp container: {}", cvpContainer);
            LOGGER.info(
                "Building client with default credential builder / managed identity");
            LOGGER.info("****************************");

            DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();
            b.endpoint(cvpConnectionString);
            b.containerName(cvpContainer);
            b.credential(credential);
        } else {
            b.connectionString(cvpConnectionString);
            b.containerName(cvpContainer);
            LOGGER.info("****************************");
            LOGGER.info(
                "This is not a real CVP endpoint - cvpConnectionString(60): {} ",
                StringUtils.left(cvpConnectionString, 60)
            );
            LOGGER.info("****************************");

        }

        BlobContainerClient blobContainerClient = b.buildClient();
        final boolean containerExists = Optional.ofNullable(blobContainerClient.exists())
            .orElse(false);

        if (!containerExists) {
            LOGGER.info("Creating container {} in HRS Storage", cvpContainer);
            blobContainerClient.create();
        }
        return blobContainerClient;
    }

}
