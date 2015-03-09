import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.routes;
import models.Assignment;
import models.User;
import org.junit.*;
import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;
/**
 * Created by Benjamin on 5/03/2015.
 */
public class AssignmentControllerTest extends TestSuperclass {
    private static List<Assignment> testAssignments = new ArrayList<>();

    @BeforeClass
    public static void initialiseAssignmentControllerTest() {
        // Start application
        startFakeApplication();

        // Mock out some user
        User john = new User("john@mail.com", "password123", "John", "Doe");
        john.save();
        User jane = new User("jane@mail.com", "123password", "Jane", "Doe");
        jane.save();

        // Add assignments to the database
        testAssignments.add(new Assignment(null, john));
        testAssignments.add(new Assignment(null, jane));
        Ebean.save(testAssignments);
    }

    @AfterClass
    public static void finaliseAssignmentControllerTest() {
        // End application
        stopFakeApplication();
    }

    @Test
    public void getAll_DatabaseFilledWithAssigments_SuccessfullyGetAllAssignments() {
        Result result = callAction(routes.ref.AssignmentController.getAllAssignments(), authorizeRequest(new FakeRequest(), getUser()));
        String jsonString = contentAsString(result);

        System.out.println(jsonString);
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(jsonString).get("assignment");
            if(node.isArray()) {
                for(int i = 0; i < node.size(); ++i) {
                    assertThat(node.get(i).get("id").asLong()).isEqualTo(testAssignments.get(i).id);
                }
            }
            else
                Assert.fail("Returned JSON is not an array");
        } catch (IOException e) {
            Assert.fail("Cast failed: invalid JSON string\nError message: " + e);
        }
    }

    @Test
    public void getAssignment_DatabaseFilledWithAssignments_SuccessfullyGetAssignment() {
        Result result = callAction(routes.ref.AssignmentController.getAssignment(testAssignments.size()-1), authorizeRequest(new FakeRequest(), getUser()));
        String jsonString = contentAsString(result);
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(jsonString);
            assertThat(node.get("id").asLong()).isEqualTo(testAssignments.size()-1);
        } catch (IOException e) {
            Assert.fail("Cast failed: invalid JSON string\nError message: " + e);
        }
    }

    @Test
    public void getAssignment_DatabaseFilledWithAssignments_BadRequestResponseFromServer() {
        Result result = callAction(routes.ref.AssignmentController.getAssignment(testAssignments.size()+1), authorizeRequest(new FakeRequest(), getUser()));
        assertThat(status(result)).isEqualTo(Http.Status.BAD_REQUEST);
    }
}
