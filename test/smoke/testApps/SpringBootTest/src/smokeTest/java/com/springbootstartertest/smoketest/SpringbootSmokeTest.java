package com.springbootstartertest.smoketest;

import java.util.List;
import java.util.Map;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.EventData;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.AiSmokeTest;
import com.microsoft.applicationinsights.smoketest.TargetUri;
import com.microsoft.applicationinsights.smoketest.UseAgent;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.*;

@UseAgent
public class SpringbootSmokeTest extends AiSmokeTest {

    @Test
    @TargetUri("/basic/trackEvent")
    public void trackEvent() throws Exception {
        mockedIngestion.waitForItems("RequestData", 1);
        mockedIngestion.waitForItemsInRequest("EventData", 2);

        // TODO get event data envelope and verify value
        final List<EventData> data = mockedIngestion.getTelemetryDataByTypeInRequest("EventData");
        assertThat(
                data,
                hasItem(
                        new TypeSafeMatcher<EventData>() {
                            final String name = "EventDataTest";
                            Matcher<String> nameMatcher = Matchers.equalTo(name);

                            @Override
                            protected boolean matchesSafely(EventData item) {
                                return nameMatcher.matches(item.getName());
                            }

                            @Override
                            public void describeTo(Description description) {
                                description.appendDescriptionOf(nameMatcher);
                            }
                        }));

        assertThat(
                data,
                hasItem(
                        new TypeSafeMatcher<EventData>() {
                            final String expectedKey = "key";
                            final String expectedName = "EventDataPropertyTest";
                            final String expectedPropertyValue = "value";
                            final Double expectedMetricValue = 1d;
                            Matcher<Map<? extends String, ? extends Double>> metricMatcher =
                                    Matchers.hasEntry(expectedKey, expectedMetricValue);
                            Matcher<Map<? extends String, ? extends String>> propertyMatcher =
                                    Matchers.hasEntry(expectedKey, expectedPropertyValue);
                            Matcher<String> nameMatcher = Matchers.equalTo(expectedName);

                            @Override
                            public void describeTo(Description description) {
                                description.appendDescriptionOf(nameMatcher);
                                description.appendDescriptionOf(propertyMatcher);
                                description.appendDescriptionOf(metricMatcher);
                            }

                            @Override
                            protected boolean matchesSafely(EventData item) {
                                return nameMatcher.matches(item.getName())
                                        && propertyMatcher.matches(item.getProperties())
                                        && metricMatcher.matches(item.getMeasurements());
                            }
                        }));
    }

    @Test
    @TargetUri("/throwsException")
    public void testResultCodeWhenRestControllerThrows() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItemsInRequest("RemoteDependencyData", 1);
        List<Envelope> edList = mockedIngestion.waitForItemsInRequest("ExceptionData", 2);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope = rddList.get(0);
        Envelope edEnvelope1 = edList.get(0);

        RequestData rd = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data) rddEnvelope.getData()).getBaseData();
        System.out.println("Response code after exception: " + rd.getResponseCode());
        int code = -123;
        try {
            code = Integer.parseInt(rd.getResponseCode());
        } catch (NumberFormatException e) {
            fail("Response code is not a number");
        }
        assertThat(code, greaterThanOrEqualTo(500));

        assertParentChild(rdd.getId(), rdEnvelope, edEnvelope1);
    }

    @Test
    @TargetUri("/asyncDependencyCall")
    public void testAsyncDependencyCall() throws Exception {
        commonValidation();
    }

    private static void commonValidation() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        List<Envelope> rddList = mockedIngestion.waitForItemsInRequest("RemoteDependencyData", 3);

        Envelope rdEnvelope = rdList.get(0);
        Envelope rddEnvelope1 = rddList.get(0);
        Envelope rddEnvelope2 = rddList.get(1);
        Envelope rddEnvelope3 = rddList.get(2);

        RequestData rd = (RequestData) ((Data) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd1 =
                (RemoteDependencyData) ((Data) rddEnvelope1.getData()).getBaseData();
        RemoteDependencyData rdd2 =
                (RemoteDependencyData) ((Data) rddEnvelope2.getData()).getBaseData();
        RemoteDependencyData rdd3 =
                (RemoteDependencyData) ((Data) rddEnvelope3.getData()).getBaseData();

        assertEquals("GET /asyncDependencyCall", rd.getName());
        assertEquals("TestController.asyncDependencyCall", rdd1.getName());
        assertEquals("GET /", rdd2.getName());
        assertEquals("TestController.asyncDependencyCall", rdd3.getName());
        assertEquals("www.bing.com", rdd2.getTarget());

        assertParentChild(rdd1.getId(), rdEnvelope, rddEnvelope2);
        assertParentChild(rdd1.getId(), rdEnvelope, rddEnvelope3);
    }

    private static void assertParentChild(
            String parentId, Envelope parentEnvelope, Envelope childEnvelope) {
        String operationId = parentEnvelope.getTags().get("ai.operation.id");

        assertNotNull(operationId);

        assertEquals(operationId, childEnvelope.getTags().get("ai.operation.id"));
        assertEquals(parentId, childEnvelope.getTags().get("ai.operation.parentId"));
    }
}
