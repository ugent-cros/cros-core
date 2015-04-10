import drones.models.DroneActor;
import drones.models.DroneCommander;
import drones.models.DroneDriver;
import drones.models.Fleet;
import drones.simulation.SimulatorDriver;
import models.Drone;
import models.DroneType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
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
        public <T extends DroneActor> T createActor(String droneAddress) {
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


    @BeforeClass
    public static void setup() {

        startFakeApplication();

        // Check if simulator driver is registered, if not register
        Fleet f = Fleet.getFleet();
        if(!f.registeredDrivers().containsKey(SimulatorDriver.SIMULATOR_TYPE)) {
            f.registerDriver(SimulatorDriver.SIMULATOR_TYPE, new SimulatorDriver());
        }
    }

    @AfterClass
    public static void teardown() {
        stopFakeApplication();;
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

        DroneType type = new DroneType("RegTest5", "1");
        DummyDriver driver = new DummyDriver("DriverTest5", type);

        Fleet f = Fleet.getFleet();
        f.registerDriver(type, driver);

        // Register 1st driver for multiple types
        assertThat(f.unregisterDriver(type, driver)).isTrue();
    }

    @Test
    public void getCommander_ForNonRegisteredType_ReturnsNull() {

        //TODO: later an exception instead maybe?
        DroneType type = new DroneType("RegTest6", "1");
        Drone drone = new Drone("FleetTestDrone6", Drone.Status.AVAILABLE, type, "x");
        drone.save();

        Fleet f = Fleet.getFleet();
        assertThat(f.getCommanderForDrone(drone)).isNull();
    }

    @Test
    public void getCommander_ForRegisteredType_ReturnsCommander() {

        Fleet f = Fleet.getFleet();

        // Use simulator driver for this test: should be registered in beforeclass method
        Drone drone = new Drone("FleetTestDrone7", Drone.Status.AVAILABLE, SimulatorDriver.SIMULATOR_TYPE, "x");
        drone.save();

        assertThat(f.getCommanderForDrone(drone)).isNotNull();
    }

    @Test
    public void getCommander_MultipleTimesForRegisteredType_ReturnsSameCommander() {

        Fleet f = Fleet.getFleet();

        // Use simulator driver for this test: should be registered in beforeclass method
        Drone drone = new Drone("FleetTestDrone8", Drone.Status.AVAILABLE, SimulatorDriver.SIMULATOR_TYPE, "x");
        drone.save();

        // TODO: rethink desired behavior: do we want 2 commanders for the same drone?
        DroneCommander commander1 = f.getCommanderForDrone(drone);
        DroneCommander commander2 = f.getCommanderForDrone(drone);

        assertThat(commander1 == commander2);   // referene equality should hold
    }
}
