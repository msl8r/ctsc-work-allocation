# CTSC Work Allocation Service

## Purpose

The purpose of this application to integrate the different **HMCTS** services with 8x8 contact center.

## Overview
The application is written in Java based on Spring Boot framework.
There are two ways of integration:
  * indirectly through CCD elastic search:
	  * A scheduled task running every 30 minutes and running a query on `/searchCases` endpoint. This endpoint is the elastic search endpoint of CCD. For every service there is a predefined query to get the relevant cases what needs to be appear in the 8x8 queue. The queries only searching for changes since the last run time shifted by -5 minutes to give enough time to elastic search to index the data.
  * directly through work-allocation web-service endpoint:
	  * There is a `/task` web-service endpoint in the application where it waits tasks to be sent from different services. S2S authentication is requires to be able to access the endpoint. The format of the payload is the following:
```
	{
	  "id": 1563460551495313,
	  "jurisdiction": "DIVORCE",
	  "state": "valami",
	  "case_type_id": "DIVORCE",
	  "last_modified_date": "2019-07-18T14:36:25.862"
	}
```
Once the service has the case details either way it will put the cases into an azure service-bus. The same time another thread starts to connect the service-bus as a consumer and waiting for the cases to appear. Anytime a new case appears on the service-bus the consumer gets it and converts it into email based on the predefined template for the give service and sends it as an email to one of the HMCTS mail account. On 8x8 side there is an imap client connected to the mailbox and puts converts the mail message to task and puts in on the queue.

### Prerequisites
You will need jdk and maven installed on your machine or use mvnw to install the prerequisites.

### Installing
1. Clone the repo to your machine using git clone https://github.com/hmcts/ctsc-work-allocation.git
2. Run $ ./gradlew build

### Running the tests

You can run the tests using 'gradle test or ./gradlew test'


### Deployment
See Jenkinsfile for the deployment details

### Run the application
To run the application at local developer machine use following command

$ gradle  bootRun  or ./gradlew bootRun

Once application server is started use swagger ui to find the endpoints and test these.
http://localhost:8080/v2/api-docs
