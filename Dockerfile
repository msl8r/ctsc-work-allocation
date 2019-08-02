ARG APP_INSIGHTS_AGENT_VERSION=2.3.1

FROM hmctspublic.azurecr.io/base/java:openjdk-8-distroless-1.0

COPY build/libs/ctsc-work-allocation.jar /opt/app/

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" wget -q --spider http://localhost:4550/health || exit 1

EXPOSE 8080
CMD [ "ctsc-work-allocation.jar" ]
