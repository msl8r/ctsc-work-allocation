package uk.gov.hmcts.reform.workallocation.model;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.workallocation.exception.CaseTransformException;

import java.util.HashMap;
import java.util.Map;

public class TaskTest {

    private Map<String, Object> data;

    @Before
    public void setUp() {
        data = new HashMap<>();
        data.put("id", 1563460551495313L);
        data.put("jurisdiction", "DIVORCE");
        data.put("state", "Submitted");
        data.put("version", null);
        data.put("case_type_id", "DIVORCE");
        data.put("created_date", "2019-07-18T14:35:51.473");
        data.put("last_modified", "2019-07-18T14:36:25.862");
        data.put("security_classification", "PUBLIC");
    }

    @Test
    public void testConvertCaseToTaskHappyPath() throws CaseTransformException {
        Task task = Task.fromCcdDCase(data, "DIVORCE");
        Assert.assertEquals("1563460551495313", task.getId());
        Assert.assertEquals("DIVORCE", task.getJurisdiction());
        Assert.assertEquals("DIVORCE", task.getCaseTypeId());
    }

    @Test(expected = CaseTransformException.class)
    public void testConvertCaseToTaskWithoutId() throws CaseTransformException {
        data.remove("id");
        Task.fromCcdDCase(data, "DIVORCE");
    }

    @Test(expected = CaseTransformException.class)
    public void testConvertCaseToTaskWitWrongDateFormat() throws CaseTransformException {
        data.put("last_modified", "asdasd121234");
        Task.fromCcdDCase(data, "DIVORCE");
    }
}
