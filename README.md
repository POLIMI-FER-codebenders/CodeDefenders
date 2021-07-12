# Code Defenders

This is Code Defenders, a mutation testing game. Publicly available at [code-defenders.org](<http://code-defenders.org>).

For information regarding running Code Defenders via docker see [the corresponding documentation](docs/Docker.md).

## Installation & Configuration

### Software Requirements

- Java (Version 8) <!-- 9 Doesn't count as it is EOL-->
- Tomcat Server (Version 9)
- Apache Maven
- Apache Ant
- MySQL (e.g. [MariaDB](https://mariadb.org/))

### Setup

This guide assumes:
 - you are running Debian 10 as OS
 - you installed all the required software

#### Modify the configuration

Copy the `example.codedefenders.properties` file to `/var/lib/tomcat9/conf/codedefenders.properties` and set the values to match your environment.  
The only required value is `data.dir`, all other properties have sensible default values.

#### Adapt the systemd service

On Debian 10 tomcat9 by default has only very limited write permissions.  
To allow the `data.dir` to be located outside of the `/var/lib/tomcat9/webapps/` folder hierarchie you have to `systemctl edit tomcat9` and add the following code:  
```ini
[Service]
ReadWritePaths="<data.dir path>"
```

#### Setup the database

The MySQL or MariaDB database and the user to access the database have to be created before the first run.

#### Create the data.dir and run the installation script

Create the `data.dir` folder with the appropriate owner/group and permissions.  
CodeDefenders (resp. the tomcat user) requires write permissions to the folder to create missing directories/files and to save the source code files.

To install Code Defenders automatically, execute the `setup.sh` script under the `installation` folder with the appropriate user and pass the `codedefenders.properties` file as input.

```bash
cd installation
./setup.sh /var/lib/tomcat9/conf/codedefenders.properties
```

The script performs a basic availability check of required software. The data directory folder structures created. All the required dependencies and files are automatically downloaded.

If any installation step fails, the installation process aborts and prints an error message.

**Note:** Depending on the chosen data directory and Tomcat installation in place, root access may be necessary to create required folders. Similarly, additional configurations might be needed. For example, if Tomcat runs under a different user, data directory accesses and ownership might be need to be adjusted.

**Note:** Code Defenders also requires that its MySQL user owns specific privileges to create databases and tables. Additionally, it requires INDEX privileges, otherwise the installation fails with an error message similar to:

```ERROR 1142 (42000) at line 183: INDEX command denied to user```


#### Set up Code Defenders admin users
Code Defenders relies on the Tomcat authentication system to identify admin users, who can access protected pages and customize Code Defenders settings.
Access control is enforced through Tomcat using Basic Authentication in the browser.
Adding a tomcat admin can be done by applying the `manager-gui` role to a user. 
This ```tomcat-users.xml``` file is per default located in ```$CATALINA_HOME/conf```.
```xml
<role rolename="manager-gui"/>
<user username="<MY_ADMIN_USER>" password="<MY_ADMIN_PWD>" roles="manager-gui"/>
```

All system configuration and privileged features are accessible for admin users under the `/admin` page. Configurations are organized in three groups:

* Game management: Create bulk games for an entire class, distribute students among the games, and assign roles to students.
* User management: Check and update users settings, and forcefully reset passwords.
* System settings: Customize technical aspects of Code Defenders and include several advanced settings.


### Build and Deployment

For successful deployment, both Tomcat and MySQL services must be running.

Tests can be skipped via `-DskipTests`  
If you want to run the tests be sure the library `libncurses.so.5` is present on your system as the database tests depend on it.

For additional information on the system- and integration-tests see the [Testing document](docs/Testing.md).

#### Manual

To deploy you simply need to copy the `codedefenders.war` file to the tomcat `webapps` directory, with the context path as name.  
E.g to deploy codedefenders at the tomcat root path (`http://localhost:8080/`) you can simply `cp target/codedefenders.war /var/lib/tomcat9/webapps/ROOT.war`

You can either download the `codedefenders.war` file from the latest [release](https://github.com/CodeDefenders/CodeDefenders/releases/) or build it yourself with `mvn clean package -DskipTests`.

#### Automatic Deployment via Maven

Alternatively you can automatically deploy with maven. For this, Tomcat requires a user with `manager-script` role, which can be configured in `$CATALINA_BASE/conf/tomcat-users.xml` (`CATALINA_BASE` is the Tomcat installation directory).

The maven deployment is configured via a `config.properties` file. Simply copy the `example.config.properties` file over and adjust the values to your needs.

```xml
<role rolename="manager-script"/>
<user username="<MY_USER>" password="<MY_USER_PASSWORD>" roles="manager-script"/>
```

**Note:** This requires the tomcat manager application to be installed, which on Debian is provided by the `tomcat9-admin` package.

Code Defenders is built and deployed with Maven using the following commands.

```bash
mvn clean deploy -DskipTests
```

### Supporters

This project is supported [IMPRESS](https://impress-project.eu/).
Check the [official website](https://code-defenders.org/about) for a detailed
list of supporters.
