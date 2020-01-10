package uk.gov.hmcts.reform.workallocation.validator;

import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class JurisdictionValidator implements ConstraintValidator<JurisdictionConstraint, String> {

    public final List<String> jurisdictions = new ArrayList<>();

    public JurisdictionValidator(@Value("${service.emails}") String serviceEmails) {
        Arrays.stream(serviceEmails.split(",")).forEach(s -> {
            String[] values = s.split(":");
            this.jurisdictions.add(values[0]);
        });
    }

    @Override
    public void initialize(JurisdictionConstraint constraintAnnotation) {

    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && jurisdictions.contains(value.toUpperCase());
    }
}
