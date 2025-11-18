package uk.gov.hmcts.reform.em.hrs.config;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Optional;

@Profile({"!integration-web-test-2"})
@Configuration
public class AzureStorageConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStorageConfig.class);

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

    @Value("${azure.storage.jurisdiction-codes.connection-string}")
    private String jurisdictionCodesConnectionString;

    @Value("${azure.storage.jurisdiction-codes.blob-container-name}")
    private String jurisdictionCodesContainer;

    @Value("${azure.storage.use-ad-auth}")
    private boolean useAdAuth;

    @Bean("hrsCvpBlobContainerClient")
    public BlobContainerClient provideHrsCvpBlobContainerClient() {
        BlobContainerClient blobContainerClient = new BlobContainerClientBuilder()
            .connectionString(hrsConnectionString)
            .containerName(hrsCvpContainer)
            .buildClient();

        createIfNotExists(blobContainerClient);
        return blobContainerClient;

    }

    @Bean("hrsVhBlobContainerClient")
    public BlobContainerClient provideHrsVhBlobContainerClient() {
        BlobContainerClient blobContainerClient = new BlobContainerClientBuilder()
            .connectionString(hrsConnectionString)
            .containerName(hrsVhContainer)
            .buildClient();

        createIfNotExists(blobContainerClient);
        return blobContainerClient;
    }

    @Bean("cvpBlobContainerClient")
    public BlobContainerClient getCvpBlobContainerClient() {
        LOGGER.info("************   CVP   ***********");

        BlobContainerClient blobContainerClient = createBlobClient(cvpConnectionString, cvpContainer);
        createIfNotExists(blobContainerClient);
        return blobContainerClient;
    }

    @Bean("jurisdictionCodesContainerClient")
    public BlobContainerClient jurisdictionCodesClient() {
        BlobContainerClient blobContainerClient = new BlobContainerClientBuilder()
            .connectionString(jurisdictionCodesConnectionString)
            .containerName(jurisdictionCodesContainer)
            .buildClient();

        createIfNotExists(blobContainerClient);
        return blobContainerClient;

    }

    private void createIfNotExists(BlobContainerClient blobContainerClient) {
        final boolean containerExists = Optional.of(blobContainerClient.exists())
            .orElse(false);

        if (!containerExists) {
            LOGGER.info("Creating container {} in HRS Storage", hrsCvpContainer);
            blobContainerClient.create();
        }
    }

    private BlobContainerClient createBlobClient(String connectionString, String containerName) {
        BlobContainerClientBuilder b = new BlobContainerClientBuilder();

        if (useAdAuth) {
            LOGGER.info("********useAdAuth********************");
            LOGGER.info("Using Managed Identity For Blob Container Client (For SAS Token Generation)");
            LOGGER.info("end point: {}", connectionString);
            LOGGER.info("container: {}", containerName);

            DefaultAzureCredential credential = new DefaultAzureCredentialBuilder().build();
            b.endpoint(connectionString);
            b.containerName(containerName);
            b.credential(credential);
        } else {
            b.connectionString(connectionString);
            b.containerName(containerName);
            LOGGER.info(
                "This is not a real endpoint - connectionString(60): {} ",
                connectionString
            );
        }
        LOGGER.info("*********BlobContainerClient*******************");
        return b.buildClient();
    }
}
