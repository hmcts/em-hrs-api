package uk.gov.hmcts.reform.em.hrs.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.em.hrs.dto.RecordingFilenameDto;
import uk.gov.hmcts.reform.em.hrs.service.FolderService;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolderControllerTest {

    @Mock
    private FolderService folderService;

    @InjectMocks
    private FolderController folderController;

    @Test
    void getFilenamesShouldReturnFilenamesWhenFolderExists() {
        String folderName = "folder-1";
        Set<String> files = Set.of("file1.mp4", "file2.mp4");

        when(folderService.getStoredFiles(folderName)).thenReturn(files);

        ResponseEntity<RecordingFilenameDto> response = folderController.getFilenames(folderName);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFolderName()).isEqualTo(folderName);
        assertThat(response.getBody().getFilenames()).containsExactlyInAnyOrder("file1.mp4", "file2.mp4");

        verify(folderService).getStoredFiles(folderName);
    }

    @Test
    void getFilenamesShouldReturnEmptySetWhenNoFilesExist() {
        String folderName = "folder-empty";

        when(folderService.getStoredFiles(folderName)).thenReturn(Collections.emptySet());

        ResponseEntity<RecordingFilenameDto> response = folderController.getFilenames(folderName);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFolderName()).isEqualTo(folderName);
        assertThat(response.getBody().getFilenames()).isEmpty();

        verify(folderService).getStoredFiles(folderName);
    }
}
