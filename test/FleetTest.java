import drones.models.DroneActor;
import drones.models.DroneDriver;
import drones.models.Fleet;
import models.Drone;
import models.DroneType;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Created by yasser on 7/04/15.
 */
public class FleetTest extends TestSuperclass {

    private class DummyDriver implements DroneDriver {

        private String id;
        private DroneType supportedType;

        public DummyDriver(String id, DroneType supportedType) {
            this.id = id;
            this.supportedType = supportedType;
        }

        @Override
        public Set<DroneType> supportedTypes() {
            Set<DroneType> types = new HashSet<>();
            types.add(supportedType);
            return types;
        }

        @Override
        public <T extends DroneActor> Class<T> getActorClass() {
            return null;
        }

        @Override
        public <T extends DroneActor> T createActor(Drone droneEntity) {
            return null;
        }

        @Override
        public int hashCode() {
            int result = 63;
            result = 13 * result + (id != null ? id.hashCode() : 0);
            result = 13 * result + (supportedType != null ? supportedType.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(Object o) {

            if (o == this) return true;

            if (o != null && o instanceof DummyDriver) {
                DummyDriver other = (DummyDriver) o;

                boolean sameID = (this.id == null && other.id == null)
                        || (this.id != null && this.id.equals(other.id));
                boolean sameType = (this.supportedType == null && other.supportedType == null)
                        || (this.supportedType != null && this.supportedType.equals(other.supportedType));
                return sameType && sameID;
            }
            return false;
        }
    }

    @Test
    public void registerDriver_ForOneNonExistingType_IsRegisteredOnce() {

        DroneType type = new DroneType("RegTest1", "1");
        DummyDriver driver = new DummyDriver("Reg test", type);

        Fleet f = Fleet.getFleet();
        f.registerDriver(type, driver);

        // Check if registered
        Map<DroneType, DroneDriver> drivers = f.registeredDrivers();
        assertThat(drivers.get(type) == driver);    // reference equality

        // Check if registered once
        drivers.remove(type);
        assertThat(drivers.get(type)).isNull();
    }

    @Test
    public void registerDriver_ForOneExistingType_ReplacedDriverOnce() {

        DroneType type1 = new DroneType("RegTest2", "1");
        DroneType type2 = new DroneType("RegTest2", "2");
        DummyDriver driver1 = new DummyDriver("Driver1", type1);
        DummyDriver driver2 = new DummyDriver("Driver2", type1);

        Fleet f = Fleet.getFleet();
        // Register 1st driver for multiple types
        f.registerDriver(type1, driver1);
        f.registerDriver(type2, driver1);

        // Replace driver for 2nd type
        f.registerDriver(type2, driver2);

        // Check if both drivers are registered for the correct type
        Map<DroneType, DroneDriver> drivers = f.registeredDrivers();
        assertThat(drivers.get(type1) == driver1);    // reference equality
        assertThat(drivers.get(type2) == driver2);
    }

    @Test
    public void unregisterDriver_ForNonExistingType_ReturnsFalse() {

        DroneType type = new DroneType("RegTest3", "1");
        DummyDriver driver = new DummyDriver("DriverTest3", type);

        Fleet f = Fleet.getFleet();
        assertThat(f.unregisterDriver(type, driver)).isFalse();
    }

    @Test
    public void unregisterDriver_ForRegisteredTypeWrongDriver_ReturnsFalse() {

        DroneType type = new DroneType("RegTest4", "1");
        DummyDriver driver1 = new DummyDriver("DriverTest4.1", type);
        DummyDriver driver2 = new DummyDriver("DriverTest4.2", null);

        Fleet f = Fleet.getFleet();
        f.registerDriver(type, driver1);

        // Register 1st driver for multiple types
        assertThat(f.unregisterDriver(type, driver2)).isFalse();
    }

    @Test
    public void unregisterDriver_ForRegisteredTypeCorrectDriver_ReturnsTrue() {

        DroneType type = new DroneType("RegTest4", "1");
        DummyDriver driver = new DummyDriver("DriverTest4", type);

        Fleet f = Fleet.getFleet();
        f.registerDriver(type, driver);

        // Register 1st driver for multiple types
        assertThat(f.unregisterDriver(type, driver)).isTrue();
    }
}
