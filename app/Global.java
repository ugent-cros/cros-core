import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import controllers.Application;
import play.GlobalSettings;
import play.Logger;
import play.libs.Json;

/**
 * Created by matthias on 21/02/2015.
 */
public class Global extends GlobalSettings {

    @Override
    public void onStart(play.Application application) {
        super.onStart(application);
        Json.setObjectMapper(new ObjectMapper().configure(SerializationFeature.WRAP_ROOT_VALUE, true));
    }

}
