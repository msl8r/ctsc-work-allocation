package uk.gov.hmcts.reform.workallocation.idam;

import org.apache.http.HttpHeaders;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "idam-api", url = "${auth.idam.client.baseUrl}")
public interface IdamApiClient {

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/oauth2/authorize",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    Authorize authorizeCodeType(
        @RequestHeader(HttpHeaders.AUTHORIZATION) final String authorisation,
        @RequestParam("response_type") final String responseType,
        @RequestParam("client_id") final String clientId,
        @RequestParam("redirect_uri") final String redirectUri,
        @RequestBody final String body
    );

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/oauth2/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    Authorize authorizeToken(
        @RequestParam("code") final String code,
        @RequestParam("grant_type") final String grantType,
        @RequestParam("redirect_uri") final String redirectUri,
        @RequestParam("client_id") final String clientId,
        @RequestParam("client_secret") final String clientSecret,
        @RequestBody final String body
    );

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/details"
    )
    UserDetails getUserDetails(@RequestHeader(HttpHeaders.AUTHORIZATION) final String oauth2Token);

}
