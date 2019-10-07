package uk.gov.hmcts.reform.workallocation.controllers;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import uk.gov.hmcts.reform.workallocation.services.CcdPollingService;
import uk.gov.hmcts.reform.workallocation.services.LastRunTimeService;

import java.time.LocalDateTime;

/**
 * Created just for the prototype to be able to test.
 *
 */
@Controller
@ConditionalOnExpression("${test.endpoints.enabled:true}")
public class CcdPollingController {

    private final CcdPollingService ccdPollingService;
    private final LastRunTimeService lastRunTimeService;

    public CcdPollingController(CcdPollingService ccdPollingService,
                                LastRunTimeService lastRunTimeService) {
        this.ccdPollingService = ccdPollingService;
        this.lastRunTimeService = lastRunTimeService;
    }

    @GetMapping("/get-cases")
    public ResponseEntity<String> pollCcd() {
        LocalDateTime lastRunTime = lastRunTimeService.getMinDate();
        lastRunTimeService.updateLastRuntime(lastRunTime);
        ccdPollingService.pollCcdEndpoint();
        return ResponseEntity.ok(generateResponse(lastRunTime));
    }

    @GetMapping("/get-cases/{time}")
    public ResponseEntity<String> pollCcd(@PathVariable("time") String time) {
        LocalDateTime lastRunTime = LocalDateTime.parse(time);
        lastRunTimeService.updateLastRuntime(lastRunTime);
        ccdPollingService.pollCcdEndpoint();
        return ResponseEntity.ok(generateResponse(lastRunTime));
    }

    @GetMapping("/get-cases/now")
    public ResponseEntity<String> pollCcdNow() {
        LocalDateTime lastRunTime = lastRunTimeService.getLastRunTime().orElse(lastRunTimeService.getMinDate());
        ccdPollingService.pollCcdEndpoint();
        return ResponseEntity.ok(generateResponse(lastRunTime));
    }

    private String generateResponse(LocalDateTime lastRunTime) {
        return String.format("Requesting cases with date %s was successful", lastRunTime);
    }
}
