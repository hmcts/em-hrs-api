package uk.gov.hmcts.reform.em.hrs.smoke;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGeneratorFactory;

@Configuration
public class CcdAuthTokenGeneratorConfiguration {

    @Bean
    public AuthTokenGenerator ccdAuthTokenGenerator(
        @Value("${s2s.api.ccdGwSecret}") final String secret,
        @Value("${s2s.api.ccdGwServiceName}") final String microService,
        final ServiceAuthorisationApi serviceAuthorisationApi
    ) {
        return AuthTokenGeneratorFactory.createDefaultGenerator(secret, microService, serviceAuthorisationApi);
    }

}
