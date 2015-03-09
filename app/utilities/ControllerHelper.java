package utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.data.Form;
import play.data.validation.ValidationError;
import play.libs.Json;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by matthias on 6/03/2015.
 */
public class ControllerHelper {

    public static class Link {

        public String rel;
        public String path;

        public Link(String rel, String path) {
            this.rel = rel;
            this.path = path;
        }
    }

    public static class Summary { }


    /**
     * This method will remove any errors on missing fields in the form.
     * If a field is not present, any errors due to constraints on the field will be removed from the form.
     * Warning: this also includes "field required" errors.
     *
     * @param form
     * The form from which the errors will be removed. The form will only contain errors of the fields which are present
     * @return
     * A list of the removed errors is returned for further inspection if desired.
     */
    public static <T> List<ValidationError> removeErrorsOnMissingFields(Form<T> form) {

        // Copy all errors
        Map<String, List<ValidationError>> errors = new HashMap<>(form.errors());
        // Remove all errors from the form
        form.discardErrors();

        // Create list to save removed errors
        List<ValidationError> removedErrors = new LinkedList<>();

        // Iterate over the keys of the errors
        for(String key : errors.keySet()) {

            // Check the value of the field associated with the error
            if(form.data().containsKey(key)) {
                // If the field is filled in we add all errors associated with this field back to the form
                for(ValidationError e : errors.get(key)) {
                    form.reject(e);
                }
            }
            else {
                // If the field wasn't present we don't place the error back
                // but put in the list of removed errors
                for(ValidationError e : errors.get(key)) {
                    removedErrors.add(e);
                }
            }
        }

        return removedErrors;
    }

    public static ObjectNode objectToJsonWithView(Object o, Class view) {

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ObjectNode node =  (ObjectNode) Json.parse(objectMapper.writerWithView(view).writeValueAsString(o));
            return node;
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
