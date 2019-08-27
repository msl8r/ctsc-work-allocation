package uk.gov.hmcts.reform.workallocation.email;

import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.workallocation.model.Task;

public class VelocityContextFactoryTest {

    @Test
    public void testGetContext() {
        Assert.assertTrue(VelocityContextFactory.getContext(Task.class) instanceof TypedVelocityContext);
    }

    @Test
    public void testGetContextWhenNoSuitableContext() {
        Assert.assertNull(VelocityContextFactory.getContext(String.class));
    }
}
