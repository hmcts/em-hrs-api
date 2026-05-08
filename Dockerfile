ARG APP_INSIGHTS_AGENT_VERSION=3.4.18
# Application image

FROM hmctsprod.azurecr.io/base/java:21-distroless

USER hmcts
COPY lib/applicationinsights.json /opt/app/
COPY build/libs/em-hrs-api.jar /opt/app/

CMD [ "em-hrs-api.jar" ]
EXPOSE 8080

