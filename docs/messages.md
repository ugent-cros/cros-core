API Messages and commands
=======================
Messages are used to have communication between the protocol implementation and the DroneActor (bi-directional). These messages contain information about commands that needs to be exectued or have information about the drone. We will describe a list of messages that can be send back to the DroneActor.

* **LocationChangedMessage**:
  * Parameters: longitude (double), latitude (double), and gpsHeight (double)
  * This message describes where the drone is located. It is expressed in longitude, latitude (both in degrees) and gpsHeight (in meters). Longitude is a number between -180 (W) and 180 (E), latitude is a number between -90 (N) and 90 (S).


* **GPSFixChangedMessage**:
  * Parameters: fixed (boolean)
  * This message describes if the gps is fixed. This message is needs to be send if you want to use the navigation features.


* **BatteryPercentageChangedMessage**:
  * Parameters:percent (byte).
  * This message describes the battery percentage of the gps.


* **FlyingStateChangedMessage**:
  * Parameters:state (FlyingState)
  * This message describes in what flying state the drone is. The possible states are:LANDED, TAKINGOFF, HOVERING, FLYING, LANDING, and EMERGENCY.


* **AlertStateChangedMessage**:
  * Parameters:state (AlertState)
  * This message describes in what alert state the drone is. The possible states are: NONE, USER_EMERGENCY, CUT_OUT, BATTERY_CRITICAL, BATTERY_LOW, and ANGLE_CRITICAL.


* **FlatTrimChangedMessage**:
  * Parameters: -


* **RotationChangedMessage**:
  * Parameters:roll (double), pitch (double), and yaw (double)
  * This message describes what the rotation is about the 3 axes (in degrees). For more info about this, see: [Flight_dynamics](https://en.wikipedia.org/wiki/Flight_dynamics)


* **AltitudeChangedMessage**:
  * Parameters: altitude (double)
  * This message describes what the altitude of the drone is (in meters).


* **SpeedChangedMessage**:
  * Parameters:speedX (double), speedY (double), speedZ (double)
  * This message describes the vector that represents the speed of the drone (all the parameters are in m/s)


* **ProductVersionChangedVersion**:
  * Parameters: software (String), hardware (String)
  * This message describes what the product version of the drone is. There is no strict definition of how the version should be described.


* **NavigationStateChangedMessage**:
  * Parameters:state (NavigationState), reason (navigationStateReason)
  * This message describes the navigation state of the drone. A change of state must always have a reason. Possible states are: AVAILABLE, IN_PROGRESS, UNAVAILABLE, PENDING. The reason of change can be: REQUESTED, CONNECTION_LOST, BaTTERY_LOW, FINISHED, STOPPED, DISABLED, ENABLED.
  * It is not advised to send this message from your own protocol implementation.


* **MagnetoCalibrationStateChangedMessage**:
  * Parameters:calibrationRequired (boolean)
  * This message describes if there the drone needs calibration.


* **ConnectionStatusChangedMessage**:
   * Parameters:connected (boolean)
   * This message describes if there is a working connection with the drone.


* **ImageMessage**:
  * Parameters: data (byte[])
  * This message represents a picture that is taken with a camera on the drone. The data is the image data (no specific format is needed) converterd to a byte array.


Besides messages, there are also commands. The commands are send from the DroneActor to the protocol and describes how the drone should act. The commands that the protocol can handle are:

* **InitDroneCommand**
* **CalibrateCommand**
* **FlatTrimCommand**
* **TakeOffCommand**
* **LandCommand**
* **SetOutdoorCommand**: this command tells the drone if it is flying outdoor (true) or indoor (false).
* **SetHullCommand**: this command tells the drone if it is flying with a hull (true if it is flying with a hull).
* **MoveCommand**: this command tells the drone to move. The move is defined via 4 parameters. The parameters have always a value between -1 and 1.
  1. vx: forwards ([-1, 0[)/backwards (]0, 1])
  2. vy: left ([-1, 0[)/right (]0, 1])
  3. vz: upwards ([-1, 0[)/downwards (]0, 1])
  4. vr: turn left ([-1, 0[)/turn right (]0, 1])


* **SetMaxHeightCommand**: the passed height is in meters.
* **SetMaxTiltCommand**: the passed tilt is in degrees.
* **FlipCommand**: the filip is detailed in type (the possible types are: FRONT, BACK, LEFT, RIGHT).
* **InitVideoCommand**: this command tells the drone to init its camera. The decoded video should be send back as images via the ImageMessage.
* **StopVideoCommand**
* **EmergencyCommand**
