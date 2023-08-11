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

import java.util.Optional;

@Configuration
public class AzureStorageConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStorageConfig.class);

    @Value("${azure.storage.hrs.connection-string}")
    private String hrsConnectionString;

    @Value("${azure.storage.hrs.cvp-dest-blob-container-name}")
    private String hrsContainer;

    @Value("${azure.storage.cvp.connection-string}")
    private String cvpConnectionString;

    @Value("${azure.storage.cvp.blob-container-reference}")
    private String cvpContainer;

    @Value("${azure.storage.vh.connection-string}")
    private String vhConnectionString;

    @Value("${azure.storage.vh.blob-container-name}")
    private String vhContainer;

    @Value("${azure.storage.use-ad-auth}")
    private boolean useAdAuth;

    @Bean("HrsCvpBlobContainerClient")
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
    public BlobContainerClient getCvpBlobContainerClient() {
        LOGGER.info("************   CVP   ***********");
        return createBlobClient(cvpConnectionString, cvpContainer);
    }

    @Bean("VhBlobContainerClient")
    public BlobContainerClient getVhBlobContainerClient() {
        LOGGER.info("************   VH   ***********");
        return createBlobClient(vhConnectionString, vhContainer);
    }

    private BlobContainerClient createBlobClient(String connectionString, String containerName) {
        BlobContainerClientBuilder b = new BlobContainerClientBuilder();

        if (useAdAuth) {
            LOGGER.info("****************************");
            LOGGER.info("Using Managed Identity For Blob Container Client (For SAS Token Generation)");
            LOGGER.info("end point: {}", connectionString);
            LOGGER.info("container: {}", containerName);
            LOGGER.info("****************************");

            DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();
            b.endpoint(connectionString);
            b.containerName(containerName);
            b.credential(credential);
        } else {
            b.connectionString(connectionString);
            b.containerName(containerName);
            LOGGER.info("****************************");
            LOGGER.info(
                "This is not a real endpoint - connectionString(60): {} ",
                StringUtils.left(connectionString, 60)
            );
            LOGGER.info("****************************");

        }
        return b.buildClient();
    }
}
