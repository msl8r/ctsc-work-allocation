package uk.gov.hmcts.reform.workallocation;

import net.serenitybdd.junit.runners.SerenityRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.workallocation.controllers.RootController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SerenityRunner.class)
public class RootControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private RootController rootController;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(rootController).build();
    }

    @Test
    public void testWelcomeEndpoint() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().is3xxRedirection()).andReturn();
    }
}
