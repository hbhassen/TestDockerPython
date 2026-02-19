Voici des commandes PowerShell pour lancer `demo2` avec un fichier `saml-config.yml` externe (sans recompiler le code). J’utilise le chemin WildFly déjà présent dans vos docs ; adaptez si besoin.

1. Copier le YAML externe (exemple basé sur celui du projet)  
```powershell
New-Item -ItemType Directory -Force C:\config | Out-Null
Copy-Item .\examples\demo2\src\main\resources\saml-config.yml C:\config\demo2-saml-config.yml -Force
```

2. Construire la librairie + le WAR (si pas encore fait)  
```powershell
mvn install
mvn -f .\examples\demo2\pom.xml package
```

3. Déployer le WAR sur WildFly  
```powershell
Copy-Item .\examples\demo2\target\demo2.war C:\Users\hamdi\Desktop\POC\wildfly-31.0.1.Final\standalone\deployments\demo2.war -Force
```

4. Démarrer WildFly avec la config externe (option **propriété système**)  
```powershell
C:\Users\hamdi\Desktop\POC\wildfly-31.0.1.Final\bin\jboss-cli.bat --connect "/system-property=saml.config.path:add(value=\"C:\\config\\demo2-saml-config.yml\")"
C:\Users\hamdi\Desktop\POC\wildfly-31.0.1.Final\bin\standalone.bat
```

Si la propriété existe déjà, utilisez plutôt :  
```powershell
C:\Users\hamdi\Desktop\POC\wildfly-31.0.1.Final\bin\jboss-cli.bat --connect "/system-property=saml.config.path:write-attribute(name=value,value=\"C:\\config\\demo2-saml-config.yml\")"
```

Option alternative **variable d’environnement** (globale au process) :  
```powershell
$env:SAML_CONFIG_PATH = "C:\config\demo2-saml-config.yml"
C:\Users\hamdi\Desktop\POC\wildfly-31.0.1.Final\bin\standalone.bat
```

Notes utiles :
- Le fichier externe est lu au démarrage. Si vous le modifiez, redémarrez ou redeployez l’application pour recharger la config.
- L’app sera accessible ensuite sur `http://localhost:8080/demo2/api/whoami`.
