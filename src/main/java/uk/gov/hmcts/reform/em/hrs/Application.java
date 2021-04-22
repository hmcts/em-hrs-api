package uk.gov.hmcts.reform.em.hrs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class excluded as using repository finder pattern?
// Without these exclusions it complains about various dependencies that are there
@SpringBootApplication
//(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
