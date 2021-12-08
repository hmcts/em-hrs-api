package uk.gov.hmcts.reform.em.hrs.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;

import java.util.concurrent.TimeUnit;

@Component
public class CheckEndPointsApplicationReadyListener implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckEndPointsApplicationReadyListener.class);

    @Autowired
    HearingRecordingStorage hearingRecordingStorage;

    @Value("${azure.cluster_name}")
    String clusterName;



    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

        LOGGER.info("Application is Ready!");

        LOGGER.info("Cluster Name: {}",clusterName);

        try {
            LOGGER.info("Sleeping 10 secs to allow token gen:");
            TimeUnit.SECONDS.sleep(10);
            LOGGER.info("StorageReport:");
            String report = hearingRecordingStorage.getStorageReport();
            LOGGER.info(report);
        } catch (Exception e) {
            LOGGER.error("Unable to verify storage connectivity: {}", e.getMessage());
        }


    }


}
