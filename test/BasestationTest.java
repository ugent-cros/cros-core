import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import play.test.FakeRequest;
import play.test.WithApplication;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.CREATED;
import static play.test.Helpers.*;
import static play.test.Helpers.status;

/**
 * Created by Eveline on 8/03/2015.
 */
public class BasestationTest extends WithApplication {
    @Before
    public void setUp(){
        start(fakeApplication(inMemoryDatabase()));
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
}
