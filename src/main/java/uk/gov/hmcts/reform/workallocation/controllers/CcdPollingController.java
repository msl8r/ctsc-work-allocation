package uk.gov.hmcts.reform.workallocation.controllers;

import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import uk.gov.hmcts.reform.workallocation.model.AuditLog;
import uk.gov.hmcts.reform.workallocation.services.CcdPollingService;
import uk.gov.hmcts.reform.workallocation.services.LastRunTimeService;
import uk.gov.hmcts.reform.workallocation.services.LoggingService;
import uk.gov.hmcts.reform.workallocation.util.MemoryAppender;

import java.time.LocalDateTime;
import java.util.stream.StreamSupport;

/**
 * Created just for the prototype to be able to test.
 *
 */
@Controller
public class CcdPollingController {

    private final CcdPollingService ccdPollingService;
    private final LastRunTimeService lastRunTimeService;
    private final LoggingService loggingService;

    public CcdPollingController(CcdPollingService ccdPollingService,
                                LastRunTimeService lastRunTimeService,
                                LoggingService loggingService) {
        this.ccdPollingService = ccdPollingService;
        this.lastRunTimeService = lastRunTimeService;
        this.loggingService = loggingService;
    }

    @GetMapping("/get-cases")
    public ResponseEntity<String> pollCcd() throws ServiceBusException, InterruptedException {
        LocalDateTime lastRunTime = LastRunTimeService.getMinDate();
        lastRunTimeService.updateLastRuntime(lastRunTime);
        ccdPollingService.pollCcdEndpoint();
        return ResponseEntity.ok(generateResponse(lastRunTime));
    }

    @GetMapping("/get-cases/{time}")
    public ResponseEntity<String> pollCcd(@PathVariable("time") String time) throws ServiceBusException,
        InterruptedException {
        LocalDateTime lastRunTime = LocalDateTime.parse(time);
        lastRunTimeService.updateLastRuntime(lastRunTime);
        ccdPollingService.pollCcdEndpoint();
        return ResponseEntity.ok(generateResponse(lastRunTime));
    }

    @GetMapping("/get-cases/now")
    public ResponseEntity<String> pollCcdNow() throws ServiceBusException, InterruptedException {
        LocalDateTime lastRunTime = lastRunTimeService.getLastRunTime().orElse(LastRunTimeService.getMinDate());
        ccdPollingService.pollCcdEndpoint();
        return ResponseEntity.ok(generateResponse(lastRunTime));
    }

    @GetMapping("/last-run-log")
    public ResponseEntity<String> getMemoryLogs() {
        String log = MemoryAppender.getEventLog()
            .stream()
            .reduce("", (iLoggingEvent, iLoggingEvent2) ->
                iLoggingEvent + "<br/>" + iLoggingEvent2);
        return ResponseEntity.ok("<html>" + log + "</html>");
    }

    @GetMapping("/logs")
    public ResponseEntity<String> getDbLogs() {
        return ResponseEntity.ok("<html>" + loggingService.getLogs().stream()
            .map(AuditLog::toString)
            .reduce("", (s, s2) -> s + "<br/>" + s2) + "</html>");
    }

    private String generateResponse(LocalDateTime lastRunTime) {
        return String.format("Requesting cases with date %s was successful", lastRunTime);
    }
}
