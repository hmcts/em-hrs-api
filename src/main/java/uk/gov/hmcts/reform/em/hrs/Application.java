package uk.gov.hmcts.reform.em.hrs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = {"uk.gov.hmcts.reform.em.hrs",
    "uk.gov.hmcts.reform.authorisation",
    "uk.gov.hmcts.reform.idam.client",
    "uk.gov.hmcts.reform.auth"}
)
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
