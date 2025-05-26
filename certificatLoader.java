import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.security.x509.X509Credential;

import java.io.FileReader;
import java.io.IOException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class CertificateLoader {

    public static X509Credential loadX509CredentialFromPEM(String certPath) throws Exception {
        try (FileReader fileReader = new FileReader(certPath);
             PEMParser pemParser = new PEMParser(fileReader)) {

            Object object = pemParser.readObject();

            if (!(object instanceof X509CertificateHolder)) {
                throw new IllegalArgumentException("Le fichier ne contient pas un certificat X.509 valide.");
            }

            X509CertificateHolder certHolder = (X509CertificateHolder) object;

            // Convertir en X509Certificate standard Java
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate)
                    certFactory.generateCertificate(
                            new java.io.ByteArrayInputStream(certHolder.getEncoded())
                    );

            // Créer et retourner un BasicX509Credential
            BasicX509Credential credential = new BasicX509Credential(certificate);
            return credential;
        }
    }
}