package uk.gov.hmcts.reform.workallocation.email;

import net.serenitybdd.junit.runners.SerenityRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import uk.gov.hmcts.reform.workallocation.model.Task;

@RunWith(SerenityRunner.class)
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
