package uk.gov.hmcts.reform.em.hrs.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.service.IngestionService;
import uk.gov.hmcts.reform.em.hrs.storage.HearingRecordingStorage;

@Service
@Transactional
public class IngestionServiceImpl implements IngestionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestionServiceImpl.class);

    private final HearingRecordingStorage hearingRecordingStorage;


    @Autowired
    public IngestionServiceImpl(final HearingRecordingStorage hearingRecordingStorage) {
        this.hearingRecordingStorage = hearingRecordingStorage;
    }

    @Override
    public void ingest(final HearingRecordingDto hrDto) {
        LOGGER.info("Ingestion: Copying File: {}", hrDto.getFilename());
        hearingRecordingStorage.copyRecording(hrDto);
        LOGGER.info("Ingestion: Completed Copying File: {}", hrDto.getFilename());
    }

}
