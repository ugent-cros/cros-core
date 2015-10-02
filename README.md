Dependencies
============
Java 8 is necessary to run the REST-api server which is built on top of the Play Framework:

~~~bash
sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java8-installer
~~~

 EBeans can use other data providers, but for ease we documented a default setup here with PostgreSQL 9.3 or higher:

~~~bash
sudo apt-get install postgresql postgresql-contrib
~~~
In order to run Play Framework, TypeSafe activator is required. [Download activator from here](https://www.typesafe.com/get-started) and add it to your path.
Make sure this is available on the path of the user that will run the REST server as well.

Server configuration
============
First, the server code has to be compiled into a production distribution.This can be executed from within the source directory:
~~~bash
activator dist
~~~
Next, copy the distribution to the folder where the REST server will be ran:
~~~bash
cp -rf target/universal/ /opt/cros
~~~

Make sure the user running the REST server has write permissions to the /logs folder.
Next, create a database user and database for the REST application. This user also requires CREATE, DROP permissions on the database.
This username, password and database credentials can be put into the *application.conf* in the *conf* directory.

~~~
db.default.driver=org.postgresql.Driver
db.default.url="jdbc:postgresql://localhost:5432/cros-web"
db.default.user=cros
db.default.password="password"
~~~

The server can now be started:
~~~bash
/opt/cros/universal/stage/bin/cros-core -Dhttp.port=9000 -DapplyEvolutions.default=true </dev/null >play.out 2>&1 &
~~~

nginx can be used as a reverse proxy in combination with the frontend. For more information, [please checkout the frontend documentation](https://github.com/ugent-cros/cros-admin/blob/master/README.md).

Code samples
============
* An ArDrone2 example can be found here:
[ArDrone2](samples/ArDrone2Example.java)

Drone Misc
==========
* [Configuring to join a network](docs/access_point.md)
* [ArDrone3 protocol specification](https://github.com/Zepheus/ardrone3-pcap/blob/master/README.md)
