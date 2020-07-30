ARG APP_INSIGHTS_AGENT_VERSION=2.6.1

FROM hmctspublic.azurecr.io/base/java:openjdk-11-distroless-1.4

COPY build/libs/ctsc-work-allocation.jar /opt/app/
COPY lib/applicationinsights-agent-2.6.1.jar lib/AI-Agent.xml /opt/app/

EXPOSE 8080
CMD [ "ctsc-work-allocation.jar" ]
