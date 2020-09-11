# Keycloak User Federation for Sqlserver / any db (User Storage SPI)

## What Is It?
This project implements a [Keycloak](https://www.keycloak.org) User Storage SPI (also known as user fedaration), which means it enables Keycloak to use an existing (m$ sqlserver in this case, but you can easily change that to any other supported db!) database with user credentials (username
, password) to authenticate users. If this doesn't ring a bell, you're problably wrong here.

This project and document was created and tested for and with Java 11 and Keycloak 10.0.2. It should however be applicable to all 10.x versions of Keycloak.

## This document
This howto shall provide you some understanding how to put all things together and make them work (see next chapters).

## Motivation
- needed a working keycloak user federation for an existing sqlserver user database
- There is lots of different and difficult to understand manuals and howtos floating around, some of them incomplete, mostly referring to outdated keycloak and java versions.
- got none of them to work out-of-the-box (like copy paste)
- needed to dig in a little deeper than I wish I had to
- maybe this helps you to get going without the same amount of hassle


## Disclaimer
This is what I found works for me and might work for you as well. I am happy about comments on how to improve or other feedback!

## JEE and Keycloak
Keycloak is implemented "in JEE" (Java Platform Enterprise Edition). That means it not only needs Java to run, but also a JEE compliant Application Server. Keycloak comes bundled with one Application Server, Wildfly in this case, formerly known as JBoss. Things are a little more complex than they
might need to be, because Application Servers are feature-rich and provide functionalities and customizations for many different scenarios. This makes themselves complex and hence there's some complexity if you need to extend or customize such an Application Server and a software that runs on it. But let's start simple and pretend that you only need to do the following:
- connect the Application Server to a db that hosts the existing user/password data.
- get Keycloak to use that connection to authenticate users


## What you need
- Java (11) JDK installed
- apache maven installed to build the project
- a keycloak 10.x installation that is meant to be customized to use the db for authentication of users
- an existing and running sqlserver that contains a table with usernames and passwords. It must be reachable from Keylcoak.
- the hostname, port, schema and db-user of the aforementioned db must be known
- passwords in db are hashed with md5 for the sake of simplicity in this example. Otherwise you'd have to modify the SimpleUserStorageProvider (actually the overridden CredentialInputValidator#isValid)


## Assumptions 
- you have some knowledge, privileges and tooling to modify the keycloak configuration, read logs, administer Keycloak
- you have some knowledge and tooling to modify, build and deploy the given project components (UserModel and so forth) if necessary
- some understanding which parts need to be changed to fit your needs


## Coarse Overview: what goes where
The following is a 
- JDBC-Driver has to be downloaded, wrapped as a "module" which in turn needs to be installed (deployed) to Keycloak
- a datasource that uses this JDBC-Driver-Module needs to be created in Keycloak
- a User Storage SPI has to be created (customized in your case if you use this project's source)
- that user storage spi needs to use the created datasource 
- the user storage spi needs to be deployed to keycloak


## JDBC-Driver and Module
This is the necessary driver, to connect Keycloak (actually wildfly) to the database. If you do not use ms sqlserver this is where you need to diverge. Search for your database's JDBC driver and documentation.

steps:
- The driver has to be downloaded and copied to a new directory.
- In this directory we also need a "module.xml"-file to be created, that contains some meta-information on the JDBC driver.
- the directory has to be deployed to Keycloak
- the Keycloak config has to be adapted to load the driver
- the Keycloak config has to be adapted to add the db username and password

see https://www.adam-bien.com/roller/abien/entry/installing_oracle_jdbc_driver_on

### download and install sqlserver JDBC-Driver
- download mssql jdbc driver from ```https://docs.microsoft.com/de-de/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server?view=sql-server-ver15```
- copy to (for jre 11) to new dir "mssqlserver".

### create file ```module.xml``` in ```mssqlserver``` with the following contents:
```
<?xml version="1.0" ?>
<module xmlns="urn:jboss:module:1.3" name="com.microsoft.sqlserver">
  <resources>
    <resource-root path="mssql-jdbc-8.2.2.jre11.jar"/>
  </resources>
  <dependencies>
    <module name="javax.api"/>
    <module name="javax.transaction.api"/>
  </dependencies>
</module>
```

###  install module and driver in keycloak
#### copy Dir "mssqlserver" to
```<path to keycloak>/keycloak-10.0.2/modules/system/layers/base/com/microsoft```

#### adapt standalone.xml
##### location of the config file
```<path to keycloak>/keycloak-10.0.2/standalone/standalone.xml```

##### add jdbc driver 
```
<driver name="sqlserver" module="com.microsoft.sqlserver"> <!-- driver name can be chosen, module is given by vendor (see documentation) -->
    <driver-class>com.microsoft.sqlserver.jdbc.SQLServerDriver</driver-class>
    <!-- xa-datasource-class>com.microsoft.sqlserver.jdbc.SQLServerXADataSource</xa-datasource-class -->
</driver>
```
##### add datasource that uses the new JDBC-driver
1) find this text in configuration file ```jndi-name="java:jboss/datasources/demoDS"```
2) under ```...<security>...</security>``` change ```<user-name>``` and ```<password>``` to correspond db user credentials
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

### verify successful installation

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

### build plugin
mvn clean install

### deploy plugin
copy from ./target/user-storage-spi.jar to 
```<path to keycloak>/keycloak-10.0.2/standalone/deployments```

### check successful deployment
==> log contains sth along those lines:
```
16:25:31,198 INFO  [org.jboss.as.server.deployment] (MSC service thread 1-6) WFLYSRV0027: Starting deployment of "user-storage-spi.jar" (runtime-name: "user-storage-spi.jar")
16:25:31,293 INFO  [org.keycloak.subsystem.server.extension.KeycloakProviderDeploymentProcessor] (MSC service thread 1-4) Deploying Keycloak provider: user-storage-spi.jar
16:25:31,317 INFO  [de.bitrecycling.keycloak.sqlserver.spi.SimpleUserStorageProviderFactory] (MSC service thread 1-4) Configured de.bitrecycling.keycloak.sqlserver.spi.SimpleUserStorageProviderFactory@1515e00f with someProperty: null
16:25:31,384 INFO  [org.jboss.as.server] (DeploymentScanner-threads - 1) WFLYSRV0010: Deployed "user-storage-spi.jar" (runtime-name : "user-storage-spi.jar")
```
==> also there is a new file "user-storage-spi.jar.deployed" in ```<path to keycloak>/keycloak-10.0.2/standalone/deployments```


## links 

##  install jdbc driver (ms sqlserver in this case) in keycloak / wildfly 
- https://www.keycloak.org/docs/latest/server_installation/#_database
- https://www.youtube.com/watch?v=_pLowp3vVFU
- https://stackoverflow.com/questions/52950369/wildfly-14-microsoft-sql-server-configuration
- https://docs.microsoft.com/de-de/sql/connect/jdbc/building-the-connection-url?view=sql-server-ver15
- https://www.mssqltips.com/sqlservertip/2495/identify-sql-server-tcp-ip-port-being-used/

### user federation / user storage spi
https://www.keycloak.org/docs/latest/server_development/index.html#_user-storage-spi
https://www.janua.fr/understanding-keycloak-user-federation/

## other sources 
https://github.com/keycloak/keycloak-quickstarts/tree/latest/user-storage-jpa
https://github.com/thomasdarimont/keycloak-user-storage-provider-demo