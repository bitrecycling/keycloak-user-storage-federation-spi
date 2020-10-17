# Keycloak User Federation (User Storage SPI) for Sqlserver / any db 

## What Is It?
This project is a howto and implementation of a [Keycloak](https://www.keycloak.org) User Storage SPI, also known as user federation. It enables Keycloak to use an existing database with user credentials (username, password) to authenticate users. The implementation is done for m$ sqlserver, but you can easily change that (see further below) to any other supported db.

This project and document was created and tested for and with Java 11 and Keycloak 10.0.2. It should however be applicable to all 10.x versions of Keycloak.


## Motivation
When I started this, I needed a working keycloak user federation for an existing sqlserver user database. I found that there's many different, partly inconsistent, outdated or incomplete documents on the internet for that subject. Hence this howto shall provide you some understanding on what you need, how to put things together and make them work, all focused on this particular subject.

None of them seemed to work out-of-the-box (like copy paste) without knowing unspecified other things keycloak / wildfly. Since my JEE knowlegde was a bit rusty, I needed to dig in a little deeper than I wish I had to. Maybe this helps you to get going without the same amount of hassle.


## Disclaimer
This is what I found works for me and might work for you as well. I am happy about comments on how to improve or other feedback!

## JEE and Keycloak
Keycloak is implemented "in JEE" (Java Platform Enterprise Edition). That means it not only needs Java to run, but also a JEE compliant Application Server. Keycloak comes bundled with one Application Server, Wildfly in this case, formerly known as JBoss. Things are a little more complex than they
might need to be, because Application Servers are feature-rich and provide functionalities and customizations for many different scenarios. This makes themselves complex and hence there's some complexity if you need to extend or customize such an Application Server and a software that runs on it. But let's start simple and pretend that you only need to do the following:
- connect the Application Server to a db that hosts the existing user/password data.
- get Keycloak to use that connection to authenticate users


## What you need / Requirements
- Java JDK installed (11 but any version >8 should do)
- apache maven installed to build the project
- a keycloak 10.x installation that is meant to be customized to use the db for authentication of users
- editor to modify the keycloak configuration, privileges to read logs, administer Keycloak
- filesystem access to build and deploy the given project components (UserModel and so forth) if necessary
- running sqlserver that contains a table with usernames and passwords. It must be reachable from Keylcoak.
- the hostname, port, schema and db-user of the aforementioned db must be known
- password hashing algorithm of the existing user password in the db

In this expample passwords are hashed with md5 for the sake of simplicity and availability. To adapt you'd have to modify the de.bitrecycling.keycloak.sqlverver.spi.SimpleUserStorageProvider (actually the overridden CredentialInputValidator#isValid)


## Coarse Overview: necessary steps
1. JDBC-Driver has to be downloaded, wrapped as a "module" which in turn needs to be installed (deployed) to Keycloak
2. a datasource that uses this JDBC-Driver-Module needs to be created in Keycloak
3. a User Storage SPI has to be created (customized in your case if you use this project's source)
4. that user storage spi needs to be configured to use the created datasource 
5. the configured user storage spi needs to be deployed to keycloak


# Step by Step


## download JDBC-Driver

This is the necessary db driver, to connect Keycloak (actually wildfly) to the database. 
> **If you do not use ms sqlserver this is where you need to diverge here.**:  Download your database's JDBC driver.


- create a new directory "mssqlserver"
- download mssql jdbc driver from ```https://docs.microsoft.com/de-de/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server?view=sql-server-ver15```
- extract downloaded archive to "mssqlserver" dir.

## create module
- create new file ```module.xml``` in ```mssqlserver```
- edit the file and paste the following contents:

```
<?xml version="1.0" ?>
<module xmlns="urn:jboss:module:1.3" name="com.microsoft.sqlserver"> <!-- replace with your driver's package/class path! -->
  <resources>
    <resource-root path="mssql-jdbc-8.2.2.jre11.jar"/> <!-- replace with your driver's file name! -->
  </resources>
  <dependencies>
    <module name="javax.api"/>
    <module name="javax.transaction.api"/>
  </dependencies>
</module>
```

##  install module and driver in keycloak
copy dir "mssqlserver" from above to
```<path to keycloak>/keycloak-10.0.2/modules/system/layers/base/com/microsoft```

## adapt standalone.xml
- open the config file in editor
- add jdbc driver by pasting the following lines
```
<driver name="sqlserver" module="com.microsoft.sqlserver"> <!-- driver name can be chosen, module is given by vendor (see documentation) -->
    <driver-class>com.microsoft.sqlserver.jdbc.SQLServerDriver</driver-class> <!-- change according to your driver's documentation  -->
    <!-- xa-datasource-class>com.microsoft.sqlserver.jdbc.SQLServerXADataSource</xa-datasource-class --> <!-- not engaged for this example, enable if necessary and change according to your driver's documentation  -->
</driver>
```

## add datasource
- find this text ```jndi-name="java:jboss/datasources/demoDS"``` in configuration file ```<path to keycloak>/keycloak-10.0.2/standalone/standalone.xml```
- change the driver value according to the name given before (sqlserver)
- under ```...<security>...</security>``` change ```<user-name>``` and ```<password>``` to correspond db user credentials
example:
```
<datasource jta="false" jndi-name="java:jboss/datasources/demoDS" pool-name="demoDS">
    <connection-url>jdbc:sqlserver://localhost\DemoDB;DB_CLOSE_DELAY=-1</connection-url>
    <driver>sqlserver</driver> <!-- this must match the jdbc driver name -->
    <pool>
        <min-pool-size>1</min-pool-size>
        <max-pool-size>3</max-pool-size>
        <prefill>true</prefill>
    </pool>
    <security>
        <user-name>demo</user-name> <!-- this is the db user -->
        <password>demo</password> <!-- this is the db user password -->
    </security>
</datasource>
```

## verify successful installation

3) check if sqlserver is running and has a tcp listener (standard port 1433 / 1434)
4) start keycloak and check if log contains something similar like:
```
12:37:49,240 INFO  [org.jboss.as.connector.subsystems.datasources] (ServerService Thread Pool -- 32) WFLYJCA0004: Deploying JDBC-compliant driver class com.microsoft.sqlserver.jdbc.SQLServerDriver (version 8.2)
12:37:49,244 INFO  [org.jboss.remoting] (MSC service thread 1-6) JBoss Remoting version 5.0.17.Final
12:37:49,274 INFO  [org.jboss.as.ejb3] (MSC service thread 1-5) WFLYEJB0482: Strict pool mdb-strict-max-pool is using a max instance size of 32 (per class), which is derived from the number of CPUs on this host.
12:37:49,276 INFO  [org.jboss.as.ejb3] (MSC service thread 1-7) WFLYEJB0481: Strict pool slsb-strict-max-pool is using a max instance size of 128 (per class), which is derived from thread worker pool sizing.
12:37:49,276 INFO  [org.jboss.as.connector.deployers.jdbc] (MSC service thread 1-2) WFLYJCA0018: Started Driver service with driver-name = sqlserver
```

## build and install user-storage spi
Note: if you need to change some details (e.g. the UserModel) to correspond your application's needs or user representation, then you need to change the code first accordingly. 

- build plugin
```mvn clean install```

deploy plugin
copy from ```./target/user-storage-spi.jar``` to 
```<path to keycloak>/keycloak-10.0.2/standalone/deployments```

## check successful deployment
==> log contains sth along those lines:
```
16:25:31,198 INFO  [org.jboss.as.server.deployment] (MSC service thread 1-6) WFLYSRV0027: Starting deployment of "user-storage-spi.jar" (runtime-name: "user-storage-spi.jar")
16:25:31,293 INFO  [org.keycloak.subsystem.server.extension.KeycloakProviderDeploymentProcessor] (MSC service thread 1-4) Deploying Keycloak provider: user-storage-spi.jar
16:25:31,317 INFO  [de.bitrecycling.keycloak.sqlserver.spi.SimpleUserStorageProviderFactory] (MSC service thread 1-4) Configured de.bitrecycling.keycloak.sqlserver.spi.SimpleUserStorageProviderFactory@1515e00f with someProperty: null
16:25:31,384 INFO  [org.jboss.as.server] (DeploymentScanner-threads - 1) WFLYSRV0010: Deployed "user-storage-spi.jar" (runtime-name : "user-storage-spi.jar")
```
==> also there is a new file "user-storage-spi.jar.deployed" in ```<path to keycloak>/keycloak-10.0.2/standalone/deployments```


## you're done
when the steps before were successful, you should be able to use the newly created SPI now: existing user/passwords can be used to login via keycloak.


## links 

###  install jdbc driver (ms sqlserver in this case) in keycloak / wildfly 
- https://www.adam-bien.com/roller/abien/entry/installing_oracle_jdbc_driver_on
- https://www.keycloak.org/docs/latest/server_installation/#_database
- https://www.youtube.com/watch?v=_pLowp3vVFU
- https://stackoverflow.com/questions/52950369/wildfly-14-microsoft-sql-server-configuration
- https://docs.microsoft.com/de-de/sql/connect/jdbc/building-the-connection-url?view=sql-server-ver15
- https://www.mssqltips.com/sqlservertip/2495/identify-sql-server-tcp-ip-port-being-used/

### user federation / user storage spi
https://www.keycloak.org/docs/latest/server_development/index.html#_user-storage-spi
https://www.janua.fr/understanding-keycloak-user-federation/

### other sources 
https://github.com/keycloak/keycloak-quickstarts/tree/latest/user-storage-jpa
https://github.com/thomasdarimont/keycloak-user-storage-provider-demo
