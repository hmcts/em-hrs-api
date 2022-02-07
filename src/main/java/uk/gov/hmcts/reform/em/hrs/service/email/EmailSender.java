package uk.gov.hmcts.reform.em.hrs.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.Map;

@Component
@ConditionalOnProperty("spring.mail.host")
public class EmailSender implements MessageSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mailSender;

    public EmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendMessageWithAttachments(
        String subject,
        String body,
        String from,
        String[] recipients,
        Map<String, File> attachments
    ) throws SendEmailException {
        try {
            MimeMessage msg = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(msg, true);
            helper.setFrom(from);
            helper.setTo(recipients);
            helper.setSubject(subject);
            helper.setText(body);
            for (Map.Entry<String, File> attachment : attachments.entrySet()) {
                helper.addAttachment(attachment.getKey(), attachment.getValue());
            }

            mailSender.send(msg);

            log.info(String.format("Message sent, subject %s", subject));
        } catch (Exception exc) {
            throw new SendEmailException(
                String.format("Error sending message, subject %s", subject),
                exc
            );
        }
    }
}
