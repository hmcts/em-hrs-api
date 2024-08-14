package uk.gov.hmcts.reform.em.hrs.testutil;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.BaseTest;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.em.hrs.testutil.SleepHelper.sleepForSeconds;

@Component
public class BlobUtil {

    public static final int FIND_BLOB_TIMEOUT = 30;
    public final BlobContainerClient hrsCvpBlobContainerClient;
    public final BlobContainerClient cvpBlobContainerClient;
    public final BlobContainerClient vhBlobContainerClient;
    public final BlobContainerClient hrsVhBlobContainerClient;


    private static final Logger LOGGER = LoggerFactory.getLogger(BaseTest.class);

    @Autowired
    public BlobUtil(@Qualifier("hrsCvpBlobContainerClient") BlobContainerClient hrsCvpBlobContainerClient,
                    @Qualifier("cvpBlobContainerClient") BlobContainerClient cvpBlobContainerClient,
                    @Qualifier("vhBlobContainerClient") BlobContainerClient vhBlobContainerClient,
                    @Qualifier("hrsVhBlobContainerClient") BlobContainerClient hrsVhBlobContainerClient
                    ) {
        this.hrsCvpBlobContainerClient = hrsCvpBlobContainerClient;
        this.cvpBlobContainerClient = cvpBlobContainerClient;
        this.vhBlobContainerClient = vhBlobContainerClient;
        this.hrsVhBlobContainerClient = hrsVhBlobContainerClient;
    }


    public void checkIfUploadedToStore(Set<String> fileNames,
                                       BlobContainerClient containerClient) {

        int retryCount = 0;
        int filesFound = 0;
        int filesToCheckCount = fileNames.size();
        while (retryCount <= 30 && filesFound < filesToCheckCount) {
            sleepForSeconds(FIND_BLOB_TIMEOUT);
            filesFound = getBlobCount(containerClient, fileNames);
            LOGGER.info("checked for upload of {} files...{} found", filesToCheckCount, filesFound);
            retryCount++;
        }
        if (retryCount > 30) {
            throw new IllegalStateException(
                "Could not find files within test.\nActual count =" + filesFound + ", Expected Total = "
                    + filesToCheckCount);
        }
    }


    private int getBlobCount(BlobContainerClient client, Set<String> fileNames) {
        return (int) client.listBlobs()
            .stream()
            .filter(c -> fileNames.contains(c.getName()))
            .collect(Collectors.toList()).stream().count();
    }


    public void uploadFileFromPathToCvpContainer(final String blobName, final String pathToFile) throws Exception {
        final FileInputStream fileInputStream = getFileFromPath(pathToFile);
        final byte[] bytes = fileInputStream.readAllBytes();
        final InputStream inStream = new ByteArrayInputStream(bytes);

        final BlobClient blobClient = cvpBlobContainerClient.getBlobClient(blobName);
        LOGGER.info("cvpBlobContainerClient url {}", cvpBlobContainerClient.getBlobContainerUrl());
        blobClient.upload(new BufferedInputStream(inStream), bytes.length);
    }

    public void uploadFileFromPathToVhContainer(final String blobName, final String pathToFile) throws Exception {
        final FileInputStream fileInputStream = getFileFromPath(pathToFile);
        final byte[] bytes = fileInputStream.readAllBytes();
        final InputStream inStream = new ByteArrayInputStream(bytes);

        final BlobClient blobClient = vhBlobContainerClient.getBlobClient(blobName);
        LOGGER.info("vhBlobContainerClient url {}", vhBlobContainerClient.getBlobContainerUrl());
        blobClient.upload(new BufferedInputStream(inStream), bytes.length);
    }

    public void uploadFileFromPathToHrsContainer(final String blobName, final String pathToFile) throws Exception {
        final FileInputStream fileInputStream =  getFileFromPath(pathToFile);
        final byte[] bytes = fileInputStream.readAllBytes();
        final InputStream inStream = new ByteArrayInputStream(bytes);

        final BlobClient blobClient = hrsCvpBlobContainerClient.getBlobClient(blobName);
        LOGGER.debug("hrsCvpBlobContainerClient url {}", hrsCvpBlobContainerClient.getBlobContainerUrl());
        blobClient.upload(new BufferedInputStream(inStream), bytes.length);
    }


    public FileInputStream getFileFromPath(final String pathToFile) throws Exception {
        final URL resource = ClassLoader.getSystemResource(pathToFile);
        final File file = new File(Objects.requireNonNull(resource).toURI());
        return new FileInputStream(file);
    }

    public long getFileSizeFromStore(String filename, BlobContainerClient bobContainerClient) {
        return bobContainerClient.getBlobClient(filename).getProperties().getBlobSize();
    }

    public long getFileSizeFromStore(Set<String> fileNames, BlobContainerClient bobContainerClient) {
        long fileSize = 0;
        for (String fileName : fileNames) {
            fileSize += bobContainerClient.getBlobClient(fileName).getProperties().getBlobSize();
        }
        return fileSize;
    }

}
