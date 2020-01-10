package uk.gov.hmcts.reform.workallocation.validator;

import org.junit.Assert;
import org.junit.Test;

public class JurisdictionValidatorTest {

    private static final String EMAILS =
        "DIVORCE:service_divorce_email@mail.com,PROBATE:service_probate_email@mail.com,CMC:service_cmc_email@mail.com";

    @Test
    public void testIsValid() {
        JurisdictionValidator validator = new JurisdictionValidator(EMAILS);
        Assert.assertTrue(validator.isValid("divorce", null));
        Assert.assertTrue(validator.isValid("Cmc", null));
        Assert.assertTrue(validator.isValid("PROBATE", null));
        Assert.assertFalse(validator.isValid("something", null));
    }
}
