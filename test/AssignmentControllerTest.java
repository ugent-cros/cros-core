import com.avaje.ebean.Ebean;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.routes;
import models.Assignment;
import models.Checkpoint;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import play.libs.Json;
import play.mvc.Result;
import play.test.FakeRequest;
import utilities.JsonHelper;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.callAction;
import static play.test.Helpers.contentAsString;
/**
 * Created by Benjamin on 5/03/2015.
 */
public class AssignmentControllerTest extends TestSuperclass {
    private static List<Assignment> testAssignments = new ArrayList<>();

    @BeforeClass
    public static void initialiseAssignmentControllerTest() {
        // Start application
        startFakeApplication();
        // Create some checkpoints and add them to a list
        Checkpoint chckpnt1 = new Checkpoint(1,2,3);
        Checkpoint chckpnt2 = new Checkpoint(2,4,6);
        Checkpoint chckpnt3 = new Checkpoint(3,6,9);
        List<Checkpoint> list1 = new ArrayList<>();
        List<Checkpoint> list2 = new ArrayList<>();
        list1.add(chckpnt1);
        list2.add(chckpnt2);
        list2.add(chckpnt3);

        // Add assignments to the database
        testAssignments.add(new Assignment(list1, getUser()));
        testAssignments.add(new Assignment(list2, getUser()));
        Ebean.save(testAssignments);
    }

    @AfterClass
    public static void finaliseAssignmentControllerTest() {
        // End application
        stopFakeApplication();
    }

    @Test
    public void getAll_DatabaseFilledWithAssigments_SuccessfullyGetAllAssignments() {
        Result result = callAction(routes.ref.AssignmentController.getAll(),
                authorizeRequest(new FakeRequest(), getUser()));
        String jsonString = contentAsString(result);

        JsonNode node = JsonHelper.removeRootElement(jsonString, Assignment.class);
        if (node.isArray()) {
            for (int i = 0; i < node.size(); ++i) {
                Assignment testAssignment = testAssignments.get(i);
                Assignment receivedAssignment = Json.fromJson(node.get(i), Assignment.class);
                assertThat(testAssignment.id).isEqualTo(receivedAssignment.id);
            }
        } else
            Assert.fail("Returned JSON is not an array");
    }

    @Test
    public void getAssignment_DatabaseFilledWithAssignments_SuccessfullyGetAssignment() {
        int index = testAssignments.size()-1;
        Assignment testAssignment = testAssignments.get(index);
        Result result = callAction(routes.ref.AssignmentController.get(testAssignment.id), authorizeRequest(new FakeRequest(), getUser()));
        String jsonString = contentAsString(result);

        JsonNode node = JsonHelper.removeRootElement(jsonString, Assignment.class);
        Assignment receivedAssignment = Json.fromJson(node, Assignment.class);
        assertThat(testAssignment).isEqualTo(receivedAssignment);
    }

    /*@Test
    public void getAssignment_DatabaseFilledWithAssignments_BadRequestResponseFromServer() {
        Result result = callAction(routes.ref.AssignmentController.get(testAssignments.size()+1), authorizeRequest(new FakeRequest(), getUser()));
        assertThat(status(result)).isEqualTo(Http.Status.BAD_REQUEST);
    }*/
}
