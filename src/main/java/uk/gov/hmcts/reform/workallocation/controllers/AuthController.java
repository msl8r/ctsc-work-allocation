package uk.gov.hmcts.reform.workallocation.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @GetMapping("/auth/callback")
    public ResponseEntity<String> callback() {
        return ResponseEntity.ok("callback called");
    }
}
