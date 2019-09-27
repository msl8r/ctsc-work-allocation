package uk.gov.hmcts.reform.workallocation.idam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Authorize {
    private String defaultUrl;
    private String code;
    private String accessToken;
    private String refreshToken;

    public Authorize(@JsonProperty("default-url") String defaultUrl,
                     @JsonProperty("code") String code,
                     @JsonProperty("access_token") String accessToken,
                     @JsonProperty("refresh_token") String refreshToken) {
        this.defaultUrl = defaultUrl;
        this.code = code;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
