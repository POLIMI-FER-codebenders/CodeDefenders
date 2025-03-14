require 'yaml'

# Path to directory containing the Vagrantfile
vagrant_file_dir = File.expand_path(File.dirname(__FILE__))
# Load settings from file
settings = YAML.load_file("#{vagrant_file_dir}/vagrant.yml") rescue puts("INFO: Could not read vagrant.yml"); Hash.new

# Load settings with default fallback values
vm_memory = (settings['vm']['memory'] rescue puts("INFO: Could not load 'vm.settings' from vagrant.yml. Using default value."); nil) || "2048"
ports_tomcat = (settings['ports']['tomcat'] rescue puts("INFO: Could not load 'ports.tomcat' from vagrant.yml. Using default value."); nil) || 8080
ports_debugging = (settings['ports']['debugging'] rescue puts("INFO: Could not load 'ports.debugging' from vagrant.yml. Using default value."); nil) || 8000
ports_database = (settings['ports']['database'] rescue puts("INFO: Could not load 'ports.database' from vagrant.yml. Using default value."); nil)|| 3306

Vagrant.configure("2") do |config|

  config.vm.define "dev" do |dev|
    dev.vm.box = "debian/buster64"

    # Booting can take a bit longer
    dev.vm.boot_timeout = 600

    # Tomcat
    dev.vm.network :forwarded_port, guest: 8080, host: ports_tomcat, host_ip: "127.0.0.1"
    # Database
    dev.vm.network :forwarded_port, guest: 3306, host: ports_database, host_ip: "127.0.0.1"
    # Remote debugging
    dev.vm.network :forwarded_port, guest: 8000, host: ports_debugging, host_ip: "127.0.0.1"

    dev.vm.synced_folder ".", "/vagrant", disabled: true

    dev.vm.provider "virtualbox" do |vb|
      # Customize the amount of memory on the VM:
      vb.memory = vm_memory
    end

    dev.vm.provision :shell do |s|
      install_pom_xml = File.read("#{vagrant_file_dir}/installation/installation-pom.xml")
      sed_expression = 's|</tomcat-users>|  <role rolename="admin"/>\n  <role rolename="manager-script"/>\n  <role rolename="manager-gui"/>\n  <user username="admin" password="admin" roles="admin"/>\n  <user username="manager" password="manager" roles="manager-gui,manager-script"/>\n</tomcat-users>|'
      tomcat9_overwrite = '[Service]\nEnvironment="CATALINA_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000"\nReadWritePaths="/srv/codedefenders"\n'
      codedefenders_properties = 'data.dir=/srv/codedefenders\ndb.password=codedefenders\n'
      s.inline = <<-SHELL
      export DEBIAN_FRONTEND=noninteractive
      # System update & upgrade
      apt update -y
      apt upgrade -y

      # Install required packages
      apt install -y tomcat9 tomcat9-admin mariadb-server maven ant

      # Remove default tomcat webapp
      rm -r /var/lib/tomcat9/webapps/ROOT

      # Bind mariadb-server to all addresses
      sed -i -E 's|bind-address.*|bind-address = 0.0.0.0|' /etc/mysql/mariadb.conf.d/50-server.cnf
      systemctl restart mariadb

      # Create database and database user
      mysql -e "CREATE DATABASE codedefenders; CREATE USER 'codedefenders'@'%' IDENTIFIED BY 'codedefenders'; GRANT ALL PRIVILEGES ON codedefenders.* TO 'codedefenders'@'%'; FLUSH PRIVILEGES;"

      # Create codedefenders data directory
      mkdir -p /srv/codedefenders
      chown -R tomcat:tomcat /srv/codedefenders

      # Create tomcat users
      sed -i -E '#{sed_expression}' /etc/tomcat9/tomcat-users.xml

      # Configure tomcat systemd service to allow writing to data directory and enable remote debugging
      mkdir -p /etc/systemd/system/tomcat9.service.d/
      printf '#{tomcat9_overwrite}' > /etc/systemd/system/tomcat9.service.d/override.conf

      # Create codedefenders configuration
      printf '#{codedefenders_properties}' > /etc/tomcat9/codedefenders.properties

      systemctl daemon-reload
      systemctl restart tomcat9

      # Install execution dependencies
      mkdir -p /tmp/cdsetup
      printf '#{install_pom_xml}' > /tmp/cdsetup/pom.xml
      cd /tmp/cdsetup
      mvn --batch-mode --quiet clean package -Dconfig.properties="/etc/tomcat9/codedefenders.properties"
      chown -R tomcat:tomcat /srv/codedefenders
      rm -rf /tmp/cdsetup
      SHELL
    end
  end
end

puts ""
