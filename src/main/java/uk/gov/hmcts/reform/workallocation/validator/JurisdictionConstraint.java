package uk.gov.hmcts.reform.workallocation.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = JurisdictionValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface JurisdictionConstraint {
    String message() default "Invalid Jurisdiction";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
