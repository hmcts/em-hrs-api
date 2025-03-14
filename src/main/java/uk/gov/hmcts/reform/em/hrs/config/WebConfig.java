package uk.gov.hmcts.reform.em.hrs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.hmcts.reform.em.hrs.interceptors.DeleteRequestInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private DeleteRequestInterceptor deleteRequestInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(deleteRequestInterceptor).addPathPatterns("/delete");
    }
}
