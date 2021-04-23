package uk.gov.hmcts.reform.em.hrs.testutil;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;

public class UploadToCVPBlobstore {
    //   https://docs.microsoft.com/en-us/azure/storage/blobs/storage-quickstart-blobs-java?tabs=powershell


    public void uploadBlob() throws IOException {

        // Create a BlobServiceClient object which will be used to create a container client
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString("DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://localhost:10000/devstoreaccount1").buildClient();

        //Create a unique name for the container
        String containerName = "cvptestcontainer";
        BlobContainerClient containerClient;

        // Create the container and return a container client object
        if (blobServiceClient.getBlobContainerClient(containerName).exists() == true) {
            containerClient = blobServiceClient.getBlobContainerClient(containerName);
        }
        else
        {
            containerClient = blobServiceClient.createBlobContainer(containerName);
        }

        // Create a local file in the ./data/ directory for uploading and downloading
        //    String localPath = "./functional-tests-1";
        String fileName = "functional-tests-1/quickstart.txt";
        File localFile = new File( fileName);

// Write text to the file
        FileWriter writer = new FileWriter(fileName, true);
        writer.write("Hello, World!");
        writer.close();

// Get a reference to a blob
        BlobClient blobClient = containerClient.getBlobClient(fileName);

        System.out.println("\nUploading to Blob storage as blob:\n\t" + blobClient.getBlobUrl());

// Upload the blob
        if (blobClient.exists() == false) {
            blobClient.uploadFromFile(fileName);
        }
    }


}

