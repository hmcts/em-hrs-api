package uk.gov.hmcts.reform.em.hrs.repository;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.gov.hmcts.reform.em.hrs.service.ScheduledTaskRunner;

@ActiveProfiles("test")
@DataJpaTest
@EnableJpaAuditing
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = AbstractRepositoryIntegrationTest.DockerPostgreDataSourceInitializer.class)
@Testcontainers
public abstract class AbstractRepositoryIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRepositoryIntegrationTest.class);

    private static final String POSTGRES_IMAGE = "hmctspublic.azurecr.io/imported/postgres:16-alpine";

    private static final String DATABASE_NAME = "emhrs";
    private static final String USER_NAME = "emhrs";
    private static final String PASSWORD = "emhrs";
    private static final int MAPPED_PORT = 5432;

    private static final DockerImageName postgresImage
        = DockerImageName.parse(POSTGRES_IMAGE).asCompatibleSubstituteFor("postgres");

    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>(postgresImage)
        .withDatabaseName(DATABASE_NAME)
        .withUsername(USER_NAME)
        .withPassword(PASSWORD)
        .withExposedPorts(MAPPED_PORT)
        .withLogConsumer(new Slf4jLogConsumer(LOGGER))
        .waitingFor(Wait.forListeningPort());

    static {
        POSTGRES_CONTAINER.start();
    }

    @MockitoBean
    public ScheduledTaskRunner taskRunner;

    public static class DockerPostgreDataSourceInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                applicationContext,
                "spring.datasource.url=" + POSTGRES_CONTAINER.getJdbcUrl(),
                "spring.datasource.username=" + POSTGRES_CONTAINER.getUsername(),
                "spring.datasource.password=" + POSTGRES_CONTAINER.getPassword()
            );
        }
    }
}
