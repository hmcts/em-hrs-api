package uk.gov.hmcts.reform.em.hrs.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.em.hrs.service.FolderService;

import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FolderController.class)
public class FolderControllerTest extends BaseWebTest {

    @MockitoBean
    FolderService folderService;

    @Test
    public void should_return_full_list_of_files() throws Exception {
        var folderName = "audioStream123";
        var fileName1 = "32123-32-23-332.mpeg";
        var fileName2 = "dcfds9923-ss-FB.mpeg";
        Set<String> folderSet = Set.of(fileName1, fileName2);
        when(folderService.getStoredFiles(folderName)).thenReturn(folderSet);
        mockMvc.perform(get("/folders/" + folderName))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.folder-name").value(folderName))
            .andExpect(jsonPath("$.filenames", hasSize(2)))
            .andExpect(jsonPath("$.filenames", containsInAnyOrder(fileName1, fileName2)));

    }


    @Test
    public void should_return_empty_list_of_files() throws Exception {
        var folderName = "audioStream9084";
        Set<String> folderSet = Set.of();
        when(folderService.getStoredFiles(folderName)).thenReturn(folderSet);
        mockMvc.perform(get("/folders/" + folderName))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.folder-name").value(folderName))
            .andExpect(jsonPath("$.filenames", hasSize(0)));
    }
}
