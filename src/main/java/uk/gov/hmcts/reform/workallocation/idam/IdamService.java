package uk.gov.hmcts.reform.workallocation.idam;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.util.Base64;

@Service
@Slf4j
public class IdamService {
    public static final int ONE_HOUR = 1000 * 60 * 60;

    private final AuthTokenGenerator authTokenGenerator;
    private final IdamApiClient idamApiClient;

    @Value("${idam.service-user.email}")
    private String idamOauth2UserEmail;

    @Value("${idam.service-user.password}")
    private String idamOauth2UserPassword;

    @Value("${auth.idam.client.client_id}")
    private String idamOauth2ClientId;

    @Value("${auth.idam.client.client_secret}")
    private String idamOauth2ClientSecret;

    @Value("${auth.idam.client.redirect_uri}")
    private String idamOauth2RedirectUrl;

    @Value("${server-url}")
    private String serverUrl;

    // Tactical idam token caching solution implemented
    // SSCS-5895 - will deliver the strategic caching solution
    private String cachedToken;

    @Autowired
    IdamService(AuthTokenGenerator authTokenGenerator, IdamApiClient idamApiClient) {
        this.authTokenGenerator = authTokenGenerator;
        this.idamApiClient = idamApiClient;
    }

    public String generateServiceAuthorization() throws IdamConnectionException {
        try {
            return authTokenGenerator.generate();
        } catch (Exception e) {
            throw new IdamConnectionException("Failed to get S2S token", e);
        }
    }

    public String getUserId(String oauth2Token) throws IdamConnectionException {
        try {
            return idamApiClient.getUserDetails(oauth2Token).getId();
        } catch (Exception e) {
            throw new IdamConnectionException("Failed to get user details", e);
        }
    }

    public String getIdamOauth2Token() throws IdamConnectionException {
        String redirectUrl = serverUrl + idamOauth2RedirectUrl;
        try {
            log.info("Requesting idam token...");
            String authorisation = idamOauth2UserEmail + ":" + idamOauth2UserPassword;
            String base64Authorisation = Base64.getEncoder().encodeToString(authorisation.getBytes());

            Authorize authorize = idamApiClient.authorizeCodeType(
                "Basic " + base64Authorisation,
                "code",
                idamOauth2ClientId,
                redirectUrl,
                " "
            );

            log.info("Passing authorization code to IDAM to get a token");

            Authorize authorizeToken = idamApiClient.authorizeToken(
                authorize.getCode(),
                "authorization_code",
                redirectUrl,
                idamOauth2ClientId,
                idamOauth2ClientSecret,
                " "
            );

            cachedToken = "Bearer " + authorizeToken.getAccessToken();

            log.info("Requesting idam token successful");

            return cachedToken;
        } catch (Exception e) {
            throw new IdamConnectionException("Requesting idam token failed: ", e);
        }
    }

    @Scheduled(fixedRate = ONE_HOUR)
    public void evictCacheAtIntervals() {
        log.info("Evicting idam token cache");
        cachedToken = null;
    }

    public IdamTokens getIdamTokens() throws IdamConnectionException {

        String idamOauth2Token;

        if (StringUtils.isEmpty(cachedToken)) {
            log.info("No cached IDAM token found, requesting from IDAM service.");
            idamOauth2Token =  getIdamOauth2Token();
        } else {
            log.info("Using cached IDAM token.");
            idamOauth2Token =  cachedToken;
        }

        return IdamTokens.builder()
                .idamOauth2Token(idamOauth2Token)
                .serviceAuthorization(generateServiceAuthorization())
                .userId(getUserId(idamOauth2Token))
                .build();
    }
}
