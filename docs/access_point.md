Joining a network
=================

This way, you can control multiple drones using one access point (make sure DHCP is enabled).

ArDrone2
---------
The following script can be used (and transferred using telnet) to join an unencrypted network named *DroneAP*:
~~~bash
#!/bin/sh
killall udhcpd
ifconfig ath0 down
iwconfig ath0 mode managed essid DroneAP ap any channel auto commit
ifconfig ath0 up
udhcpc -b -i ath0
~~~

Bebop/ArDrone3
--------------
This can be configured to react to the buttons using the bash scripts provided in */bin/onoffbutton*:
~~~bash
#!/bin/sh
ESSID=DroneAP
#WPA2_KEY='openses@me'
DEFAULT_WIFI_SETUP=/sbin/broadcom_setup.sh

# Set light to orange
BLDC_Test_Bench -G 1 1 0 >/dev/null

# Check whether drone is in access point mode
if [ $(bcmwl ap) -eq 1 ]
then
        echo "Trying to connect to $ESSID" | logger -s -t "LongPress" -p user.info

		# Bring access point mode down
        $DEFAULT_WIFI_SETUP remove_net_interface
		# Configure wifi to connect to given essid
        ifconfig eth0 down
        bcmwl down
        bcmwl band auto
        bcmwl autocountry 1
        bcmwl up
        bcmwl ap 0
        bcmwl join ${ESSID}                                
        ifconfig eth0 up
		# Run dhpc client
        udhcpc -b -i eth0 --hostname=$(hostname)
else
		# Should make drone an access point again
		# Bug: does not work yet (turn drone off & on instead)
        $DEFAULT_WIFI_SETUP create_net_interface
fi

# Set light back to green after 1 second
(sleep 1; BLDC_Test_Bench -G 0 1 0 >/dev/null) &
~~~
