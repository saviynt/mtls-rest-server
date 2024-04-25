# REST API with Mutual TLS
The sample project is created to showcase how we can build a REST API which enforces Mutual TLS authentication. Mutual
TLS authentication requires two-way authentication between the client and the server. In addition to server
authentication (where the client verifies the server's identity), mutual TLS requires client authentication (where the 
server verifies the client's identity). With mutual TLS, clients must present X.509 certificates to verify their
identity to access your API. In microservices architectures, use of mutual TLS can secure service to service
communication, preventing unauthorized access and data breaches.

## Software Used
- [Java 17](https://adoptium.net/temurin/releases/?version=17)
- [Tomcat 10](https://tomcat.apache.org/download-10.cgi)
- OpenSSL

## Generate Key Pairs (Private Key, Public Key, Certificate Signing Request & Certificate)
OpenSSL commands handful to generate Key Pair, Certificate Signing Request and Certificate

To view a Private Key `openssl rsa -in rootCA.key -text`
To view a certificate `openssl x509 -noout -text -in rootCA.pem`

### Root KeyPair for Certificate Authority (CA)

- Generate Private Key
  - with passphrase
    ```
    openssl genrsa -des3 -out rootCA.key 4096
    ```
  - without passphrase
    ```
    openssl genrsa -out rootCA.key 4096
    ```
- Generate Certificate 
  ```
  openssl req -x509 -new -nodes -key rootCA.key -sha512 -days 1825 -out rootCA.crt -subj "/C=US/ST=CA/L=Los Angeles/O=Saviynt Inc/OU=Certificate Authority/CN=saviynt.com/emailAddress=ca@saviynt.com" -addext "subjectAltName = DNS:saviynt.com"
  ```
- Generate Private Key & Certificate using a Single line command
  ```
  openssl req -x509 -newkey rsa:4096 -sha512 -nodes -keyout rootCA.key -out rootCA.crt -days 1825 -subj "/C=US/ST=CA/L=Los Angeles/O=Saviynt Inc/OU=Certificate Authority/CN=saviynt.com/emailAddress=ca@saviynt.com" -addext "subjectAltName = DNS:saviynt.com"
  ```

### Server's Key & Certificate

- Generate Private Key
  - with passphrase 
    ```
    openssl genrsa -des3 -out server.key 4096
    ```
  - without passphrase
    ```
    openssl genrsa -out server.key 4096
    ```
- Generate Certificate Signing Request file
  ```
  openssl req -new -sha512 -key server.key -out server.csr \
  -subj "/C=US/ST=CA/L=Los Angeles/O=Saviynt Inc/OU=Saviynt Server/CN=server.saviynt.com/emailAddress=server@saviynt.com" \
  -addext "subjectAltName = DNS:server.saviynt.com"
  ```
- Generate Certificate 
  ```
  openssl x509 -req -in server.csr -CA rootCA.crt -CAkey rootCA.key -CAcreateserial -out server.crt -days 365 -sha512
  ```
- Create PKCS12 Key Store for the Server component with Key & Certificate. The server is going to use this KeyStore.
  Additionally assigning a alias name to refer easily in case Key Store contains multiple KeyPairs.
  ```
  openssl pkcs12 -export -in server.crt -inkey server.key -out keystore.p12 -name server-primary-alias
  ```

### Client's Key & Certificate

- Generate Private Key
  - with passphrase 
    ```
    openssl genrsa -des3 -out client.key 4096
    ```
  - without passphrase
    ```
    openssl genrsa -out client.key 4096
    ```
- Generate Certificate Signing Request file
  ```
  openssl req -new -sha512 -key client.key -out client.csr \
  -subj "/C=US/ST=CA/L=Los Angeles/O=Saviynt Inc/OU=Saviynt Client/CN=client.saviynt.com/emailAddress=client@saviynt.com" \
  -addext "subjectAltName = DNS:client.saviynt.com"
  ```
- Generate Certificate 
  ```
  openssl x509 -req -in client.csr -CA rootCA.crt -CAkey rootCA.key -CAcreateserial -out client.crt -days 365 -sha512
  ```
- Create PKCS12 Key Store for the Client component with Key & Certificate. The clinet is going to use this KeyStore.
  Additionally assigning a alias name to refer easily in case Key Store contains multiple KeyPairs.
  ```
  openssl pkcs12 -export -in server.crt -inkey server.key -out keystore.p12 -name server-primary-alias
  ```

### Generate TrustStore
In this example, same CA (Certificate Authority) has been used for signing the Server and Client certificates. 
Therefore, a Global trust store is distributed to both server and client. You can use different CA for Server and Client
as well. In that case you have to distribute Trust Store accordingly.

  - Generate TrustStore of PKCS12 type and pass it to server & client.
  ```
  keytool -import -trustcacerts -alias rootCA -file rootCA.crt -keystore truststore.p12 -storetype PKCS12 -storepass changeit
  ```
  N.B. - The TrustStore of PKCS12 type can be created using 
  ```
  openssl pkcs12 -export -nokeys -in rootCA.crt -out truststore.p12 -passout pass:changeit
  ```
  But this can't be read using `keytool -list -storetype PKCS12 -keystore truststore.p12`
  Output looks like below from the above command 
  ```
  Enter keystore password:  
  Keystore type: PKCS12
  Keystore provider: SUN

  Your keystore contains 0 entries
  ```
  Even java code is facing error like following at server startup.
  ```
  Caused by: java.security.InvalidAlgorithmParameterException: the trustAnchors parameter must be non-empty
        at java.base/java.security.cert.PKIXParameters.setTrustAnchors(PKIXParameters.java:200) ~[na:na]
        at java.base/java.security.cert.PKIXParameters.<init>(PKIXParameters.java:157) ~[na:na]
        at java.base/java.security.cert.PKIXBuilderParameters.<init>(PKIXBuilderParameters.java:130) ~[na:na]
        at org.apache.tomcat.util.net.SSLUtilBase.getParameters(SSLUtilBase.java:501) ~[tomcat-embed-core-10.1.11.jar:10.1.11]
        at org.apache.tomcat.util.net.SSLUtilBase.getTrustManagers(SSLUtilBase.java:432) ~[tomcat-embed-core-10.1.11.jar:10.1.11]
        at org.apache.tomcat.util.net.SSLUtilBase.createSSLContext(SSLUtilBase.java:246) ~[tomcat-embed-core-10.1.11.jar:10.1.11]
        at org.apache.tomcat.util.net.AbstractJsseEndpoint.createSSLContext(AbstractJsseEndpoint.java:104) ~[tomcat-embed-core-10.1.11.jar:10.1.11]
        ... 25 common frames omitted

  ```

## Build WAR and Deploy in Tomcat

### Build
This command will compile your source code, package it into a WAR file, and place it in the `build/libs` directory of
your project.
```
./gradlew clean
./gradlew build
```

### Deploy in Tomcat
In this project Tomcat 10 has been used from [Tomcat 10](https://tomcat.apache.org/download-10.cgi). Extract the archive.
Copy the WAR file under `$CATALINA_HOME/webapps/`
Copy the keystore and truststore under `$CATALINA_HOME/conf/` 
Edit `$CATALINA_HOME/conf/server.xml` to add a `Connector` element as follows to enable TLS and Client Authentication
(i.e. M-TLS).

```
<Connector 
        port="8443"
        protocol="org.apache.coyote.http11.Http11NioProtocol"
        maxThreads="150"
        SSLEnabled="true"
        scheme="https"
        secure="true"
        clientAuth="true"
        sslProtocol="TLS"
        maxParameterCount="1000">
        <!-- address="0.0.0.0" -->
        <SSLHostConfig
          certificateVerification="required"
          truststoreFile="<path_to_truststore.p12_file>"
          truststoreType="PKCS12"
          truststorePassword="changeit">
            <Certificate 
                certificateKeystoreFile="<path_to_keystore.p12_file>" 
                certificateKeystoreType="PKCS12" 
                certificateKeystorePassword="changeit" 
                type="RSA" />
                <!-- certificateKeyAlias="server-primary-alias"  -->
        </SSLHostConfig>
    </Connector>
```

Start the server using `$CATALINA_HOME/bin/startup.sh`.

You can even test the MTLS REST API using `curl` command like following.
```
curl -vvv \
--cert store/client.crt \
--key store/client.key \
--cacert store/rootCA.crt \
"https://server.saviynt.com:8443/mtls-rest-server/hello"
```
