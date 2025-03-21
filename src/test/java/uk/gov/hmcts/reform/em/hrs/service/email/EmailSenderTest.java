package uk.gov.hmcts.reform.em.hrs.service.email;

import com.google.common.io.Resources;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.io.File;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailSenderTest {

    private static final String FROM_ADDRESS = "from@hmcts.net";
    private static final String RECIPIENT_1 = "Foo <foo@hmcts.net>";
    private static final String RECIPIENT_2 = "bar@hmcts.net";
    private static final String FILE_NAME_1 = "email/test1.zip";
    private static final String FILE_NAME_2 = "email/test2.zip";
    private static final String SUBJECT = "subject";
    private static final String BODY = "body";

    @Test
    void should_handle_mail_exception() throws Exception {
        // given
        JavaMailSender mailSender = mock(JavaMailSender.class);

        given(mailSender.createMimeMessage())
            .willReturn(new JavaMailSenderImpl().createMimeMessage());

        willThrow(MailSendException.class)
            .given(mailSender)
            .send(any(MimeMessage.class));

        EmailSender emailSender = new EmailSender(mailSender);

        File file1 = new File(Resources.getResource(FILE_NAME_1).toURI());
        File file2 = new File(Resources.getResource(FILE_NAME_2).toURI());

        // when
        SendEmailException ex = catchThrowableOfType(
            SendEmailException.class,
            () -> emailSender.sendMessageWithAttachments(
                SUBJECT,
                BODY,
                FROM_ADDRESS,
                new String[]{RECIPIENT_1, RECIPIENT_2},
                Map.of(FILE_NAME_1, file1, FILE_NAME_2, file2)
            )
        );

        // then
        assertThat(ex.getMessage())
            .isEqualTo(String.format("Error sending message, subject %s", SUBJECT));
    }

    @Test
    void should_send_email() throws Exception {
        // given
        JavaMailSender mailSender = mock(JavaMailSender.class);

        given(mailSender.createMimeMessage())
            .willReturn(new JavaMailSenderImpl().createMimeMessage());


        willDoNothing()
        .given(mailSender).send(any(MimeMessage.class));

        EmailSender emailSender = new EmailSender(mailSender);

        File file1 = new File(Resources.getResource(FILE_NAME_1).toURI());
        File file2 = new File(Resources.getResource(FILE_NAME_2).toURI());

        // when
        emailSender.sendMessageWithAttachments(
            SUBJECT,
            BODY,
            FROM_ADDRESS,
            new String[]{RECIPIENT_1, RECIPIENT_2},
            Map.of(FILE_NAME_1, file1, FILE_NAME_2, file2)
        );

        ArgumentCaptor<MimeMessage> accountCaptor = ArgumentCaptor.forClass(MimeMessage.class);

        // then
        verify(mailSender).send(accountCaptor.capture());

        MimeMessage message = accountCaptor.getValue();
        assertThat(message.getFrom()[0]).hasToString(FROM_ADDRESS);
        assertThat(message.getAllRecipients()[0]).hasToString(RECIPIENT_1);
        assertThat(message.getAllRecipients()[1]).hasToString(RECIPIENT_2);
        assertThat(message.getSubject()).isEqualTo(SUBJECT);
    }

}
