ARG APP_INSIGHTS_AGENT_VERSION=3.2.6
# Application image

FROM hmctspublic.azurecr.io/base/java:17-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/em-hrs-api.jar /opt/app/

CMD [ "em-hrs-api.jar" ]
EXPOSE 8080

