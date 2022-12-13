# Add to server.xml
# <Connector protocol="org.apache.coyote.http11.Http11NioProtocol" SSLEnabled="true"
# port="443" maxThreads="200"
# scheme="https" secure="true"
# keystoreFile="/etc/tomcat9/new.keystore" keystorePass="changeit"
# sslEnabledProtocols="TLSv1,TLSv1.1,TLSv1.2"
# clientAuth="false" sslProtocol="TLS"/>
cd /etc/tomcat9
sudo certbot certonly --standalone -d codedef2.duckdns.org
sudo openssl pkcs12 -export -in /etc/letsencrypt/live/codedef2.duckdns.org/fullchain.pem -inkey /etc/letsencrypt/live/codedef2.duckdns.org/privkey.pem -out mycert.p12 -name tomcat -password pass:changeit
sudo keytool -importkeystore -srcstorepass changeit -deststorepass changeit -destkeystore new.keystore -srckeystore mycert.p12  -srcstoretype PKCS12 -noprompt