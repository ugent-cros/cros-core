import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.Basestation;
import models.Checkpoint;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import play.libs.Json;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.WithApplication;

import javax.validation.constraints.AssertTrue;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.CREATED;
import static play.test.Helpers.*;
import static play.test.Helpers.routeAndCall;
import static play.test.Helpers.status;

/**
 * Created by Eveline on 8/03/2015.
 */
public class BasestationTest extends WithApplication {
    @Before
    public void setUp(){
        start(fakeApplication(inMemoryDatabase()));
        //put some data in db
        Checkpoint cp = new Checkpoint(88.0,88.0,2.0);
        cp.save();
        Basestation b = new Basestation("testBar", cp);
        b.save();
    }


    @Test
    public void addBasestation(){
        Map<String, String> data = new HashMap<>();
        data.put("name", "Bar1");
        data.put("lattitude", "55.57");
        data.put("longitude", "55.57");
        data.put("altitude", "1.0");

        FakeRequest addRequest = new FakeRequest("POST", "/basestations/");
        addRequest = addRequest.withFormUrlEncodedBody(data);

        Result result = routeAndCall(addRequest, 10);
        System.out.println(contentAsString(result));
        assertThat(status(result)).isEqualTo(CREATED);
    }

    @Test
    public void getBasestation(){
        FakeRequest getRequest = new FakeRequest("GET", "/basestations/1");
        Result result = routeAndCall(getRequest, 10);
        String jsonString = contentAsString(result);
        System.out.println(jsonString);

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(jsonString).get("Basestation");
            assertThat(node.get("id").asLong()).isEqualTo(1);
        } catch (IOException e) {
            Assert.fail("Cast failed: invalid JSON string\nError message: " + e);
        }
    }

    @Test
    public void updateBasestation(){
        Basestation ref = Basestation.find.byId((long)1);
        Map<String, String> data = new HashMap<>();
        data.put("name", ref.name + "- edited");
        data.put("checkpoint_id",  Long.toString(ref.checkpoint.id));
        data.put("lattitude", Double.toString(ref.checkpoint.lattitude));
        data.put("longitude", Double.toString(ref.checkpoint.longitude));
        data.put("altitude", "0.0");

        FakeRequest putRequest = new FakeRequest("PUT", "/basestations/1");
        putRequest = putRequest.withFormUrlEncodedBody(data);
        Result result = routeAndCall(putRequest, 10);
        //test statuscode
        assertThat(status(result)).isEqualTo(OK);
    }

    @Test
    public void deleteBasestation(){
        FakeRequest deleteRequest = new FakeRequest("DELETE", "/basestations/1");
        Result result = routeAndCall(deleteRequest, 10);
        //test statuscode
        assertThat(status(result)).isEqualTo(OK);
    }


}
