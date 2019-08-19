package uk.gov.hmcts.reform.workallocation.email;

import uk.gov.hmcts.reform.workallocation.model.Task;

public class VelocityContextFactory {

    private VelocityContextFactory() {
        super();
    }

    @SuppressWarnings("unchecked")
    public static <T> TypedVelocityContext<T> getContext(Class clazz) {
        if (clazz.equals(Task.class)) {
            return (TypedVelocityContext<T>) new TaskVelocityContext();
        }
        return null;
    }
}
