ARG APP_INSIGHTS_AGENT_VERSION=2.3.1

FROM hmctspublic.azurecr.io/base/java:openjdk-8-distroless-1.2

COPY build/libs/ctsc-work-allocation.jar /opt/app/
COPY lib/applicationinsights-agent-2.3.1.jar lib/AI-Agent.xml /opt/app/

EXPOSE 8080
CMD [ "ctsc-work-allocation.jar" ]
