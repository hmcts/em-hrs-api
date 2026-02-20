package uk.gov.hmcts.reform.em.hrs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.reform.ccd.client.model.CallbackRequest;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.componenttests.AbstractBaseTest;
import uk.gov.hmcts.reform.em.hrs.exception.EmailNotificationException;
import uk.gov.hmcts.reform.em.hrs.exception.HearingRecordingNotFoundException;
import uk.gov.hmcts.reform.em.hrs.exception.InvalidApiKeyException;
import uk.gov.hmcts.reform.em.hrs.exception.UnauthorisedServiceException;
import uk.gov.hmcts.reform.em.hrs.exception.ValidationErrorException;
import uk.gov.hmcts.reform.em.hrs.service.FolderService;
import uk.gov.hmcts.reform.em.hrs.service.ShareAndNotifyService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.CCD_CASE_ID;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SERVER_ERROR_MESSAGE;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SERVICE_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.SHAREE_EMAIL_ADDRESS;
import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.convertObjectToJsonString;

class GlobalExceptionHandlerTest extends AbstractBaseTest {
    @MockitoBean
    private FolderService folderService;

    @MockitoBean
    private ShareAndNotifyService shareService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testShouldReturnNotFoundWithMessageWhenHearingRecordingNotFoundExceptionIsRaised() throws Exception {
        final String path = "/sharees";
        final String expectedMessage = String.format(
            "Hearing Recording with CcdCaseId: %s is not be found",
            CCD_CASE_ID
        );
        final CaseDetails caseDetails = CaseDetails.builder()
            .data(Map.of("recipientEmailAddress", SHAREE_EMAIL_ADDRESS))
            .id(CCD_CASE_ID)
            .build();
        final CallbackRequest callbackRequest = CallbackRequest.builder().caseDetails(caseDetails).build();

        doThrow(new HearingRecordingNotFoundException(CCD_CASE_ID)).when(shareService)
            .shareAndNotify(CCD_CASE_ID, caseDetails.getData(), AUTHORIZATION_TOKEN);

        final MvcResult mvcResult = mockMvc.perform(post(path)
                                                        .content(convertObjectToJsonString(callbackRequest))
                                                        .contentType(APPLICATION_JSON_VALUE)
                                                        .header("Authorization", AUTHORIZATION_TOKEN)
                                                        .header("ServiceAuthorization", SERVICE_AUTHORIZATION_TOKEN))
            .andExpect(status().isNotFound())
            .andExpect(MockMvcResultMatchers.content().string(expectedMessage))
            .andReturn();

        assertThat(mvcResult).isNotNull();
    }

    @Test
    void testShouldReturnBadRequestWithValidationErrorsWhenValidationErrorExceptionIsRaised() throws Exception {
        final String path = "/sharees";
        final Map<String, Object> expectedErrors = Map.of("email", "must be a valid email address");
        final CaseDetails caseDetails = CaseDetails.builder()
            .data(Map.of("recipientEmailAddress", "invalid-email"))
            .id(CCD_CASE_ID)
            .build();
        final CallbackRequest callbackRequest = CallbackRequest.builder().caseDetails(caseDetails).build();

        doThrow(new ValidationErrorException(expectedErrors)).when(shareService)
            .shareAndNotify(CCD_CASE_ID, caseDetails.getData(), AUTHORIZATION_TOKEN);

        mockMvc.perform(post(path)
                            .content(objectMapper.writeValueAsString(callbackRequest))
                            .contentType(APPLICATION_JSON_VALUE)
                            .header("Authorization", AUTHORIZATION_TOKEN)
                            .header("ServiceAuthorization", SERVICE_AUTHORIZATION_TOKEN))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.email").value("must be a valid email address"));
    }

    @Test
    void testShouldReturnInternalServerErrorWithMessageWhenNotificationClientExceptionIsRaised() throws Exception {
        final String path = "/sharees";
        final CaseDetails caseDetails = CaseDetails.builder()
            .data(Map.of("recipientEmailAddress", SHAREE_EMAIL_ADDRESS))
            .id(CCD_CASE_ID)
            .build();
        CallbackRequest callbackRequest = CallbackRequest.builder().caseDetails(caseDetails).build();

        doThrow(EmailNotificationException.class).when(shareService)
            .shareAndNotify(CCD_CASE_ID, caseDetails.getData(), AUTHORIZATION_TOKEN);

        final MvcResult mvcResult = mockMvc.perform(post(path)
                                                        .content(convertObjectToJsonString(callbackRequest))
                                                        .contentType(APPLICATION_JSON_VALUE)
                                                        .header("Authorization", AUTHORIZATION_TOKEN)
                                                        .header("ServiceAuthorization", SERVICE_AUTHORIZATION_TOKEN))
            .andExpect(status().isInternalServerError())
            .andExpect(MockMvcResultMatchers.content().string(SERVER_ERROR_MESSAGE))
            .andReturn();

        assertThat(mvcResult).isNotNull();
    }

    @Test
    void testShouldReturnInternalServerErrorWithMessageToCatchAllOtherExceptions() throws Exception {
        final String testFolder = "folder-1";
        final String path = "/folders/" + testFolder;
        doThrow(RuntimeException.class).when(folderService).getStoredFiles(testFolder);

        final MvcResult mvcResult = mockMvc.perform(get(path).accept(APPLICATION_JSON_VALUE))
            .andExpect(status().isInternalServerError())
            .andExpect(MockMvcResultMatchers.content().string(SERVER_ERROR_MESSAGE))
            .andReturn();

        assertThat(mvcResult).isNotNull();
    }

    @Test
    void testShouldReturnUnauthorizedWhenInvalidApiKeyExceptionIsRaised() throws Exception {
        final String testFolder = "folder-2";
        final String path = "/folders/" + testFolder;
        final String expectedMessage = "Invalid API Key provided";

        doThrow(new InvalidApiKeyException(expectedMessage)).when(folderService).getStoredFiles(testFolder);

        mockMvc.perform(get(path).accept(APPLICATION_JSON_VALUE))
            .andExpect(status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.content().string(expectedMessage));
    }

    @Test
    void testShouldReturnForbiddenWhenUnauthorisedServiceExceptionIsRaised() throws Exception {
        final String testFolder = "folder-3";
        final String path = "/folders/" + testFolder;
        final String expectedMessage = "Service not authorised to make this request";

        doThrow(new UnauthorisedServiceException(expectedMessage)).when(folderService).getStoredFiles(testFolder);

        mockMvc.perform(get(path).accept(APPLICATION_JSON_VALUE))
            .andExpect(status().isForbidden())
            .andExpect(MockMvcResultMatchers.content().string(expectedMessage));
    }
}
