keytool -exportcert \
 -keystore monkeystore.jks \
 -alias monalias \
 -file certificat.der \
 -storepass motdepasse

openssl x509 -inform der -in certificat.der -out certificat.pem