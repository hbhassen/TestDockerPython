# Guide complet d'integration `smallibfront`

Ce document est la reference pour integrer `smallibfront` dans un projet Angular et diagnostiquer rapidement les problemes d'interception HTTP et de token.

## 1. Nom du package

Le package cree dans ce repo est:

- `smallibfront`

Si vous utilisez un autre nom (`samlfrontlib`, `samldrontlib`, etc.), adaptez les imports et la dependance `package.json` en consequence.

## 2. Prerequis techniques

- Node.js 20+
- npm 10+
- Angular 19.x
- `@angular/common`, `@angular/core`, `rxjs` compatibles avec les peer dependencies de la lib

Verifier les versions:

```bash
node -v
npm -v
npm ls @angular/core @angular/common rxjs smallibfront
```

## 3. Installer correctement la librairie

### Option A: dependance locale (meme machine/repo)

1. Build de la lib:

```bash
cd examples/smallibfront
npm install
npm run build
```

2. Installation dans le frontend:

```bash
cd ../demo2-front19
npm install
```

3. Verifier la resolution:

```bash
npm ls smallibfront
```

Vous devez voir un lien vers `../smallibfront`.

### Option B: livrer un package `.tgz` (recommande entre PCs)

Sur la machine source:

```bash
cd examples/smallibfront
npm install
npm run build
npm pack
```

Copiez le fichier `smallibfront-<version>.tgz` sur l'autre PC.

Sur la machine cible:

```bash
cd examples/demo2-front19
npm install C:\\path\\to\\smallibfront-<version>.tgz
```

Cette methode evite les problemes de chemin local `file:../smallibfront`.

## 4. Integration Angular (NgModule)

Exemple type:

```ts
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { provideSmallibFront } from 'smallibfront';

@NgModule({
  imports: [BrowserModule, HttpClientModule],
  providers: [
    ...provideSmallibFront({
      requestMatcher: (request) =>
        request.url.startsWith('http://localhost:8080/demo2/api/'),
      token: {
        responseHeaderName: 'X-Auth-Token',
        requestHeaderName: 'Authorization',
        requestHeaderPrefix: 'Bearer',
        storageKey: 'demo2front19.token',
        storageStrategy: 'sessionStorage'
      },
      saml: {
        enabled: true,
        expectedFormInputName: 'SAMLRequest',
        relayStateInputName: 'RelayState',
        overrideRelayStateWithCurrentUrl: true
      },
      onTokenUpdated: (token) => console.debug('[smallibfront] token=', token),
      onSamlAutoSubmit: (details) => console.debug('[smallibfront] saml=', details)
    })
  ]
})
export class AppModule {}
```

## 5. Integration Angular standalone (app.config.ts)

Si votre projet utilise `provideHttpClient`, il faut activer les intercepteurs DI:

```ts
import { ApplicationConfig } from '@angular/core';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideSmallibFront } from 'smallibfront';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(withInterceptorsFromDi()),
    ...provideSmallibFront({
      requestUrlIncludes: ['/api/']
    })
  ]
};
```

Sans `withInterceptorsFromDi()`, les intercepteurs ne seront pas executes.

## 6. Conditions backend obligatoires (souvent la cause du probleme)

Pour que le token fonctionne entre frontend et backend cross-origin:

1. Le backend doit renvoyer le token dans le header configure (par defaut `X-Auth-Token`).
2. Le backend doit exposer ce header:
   - `Access-Control-Expose-Headers: X-Auth-Token`
3. Le backend doit autoriser les headers entrants:
   - `Access-Control-Allow-Headers` doit inclure `Authorization` (ou votre header configure)
4. Si cookies/session:
   - frontend: `withCredentials: true`
   - backend: `Access-Control-Allow-Credentials: true`
   - `Access-Control-Allow-Origin` ne doit pas etre `*` quand credentials=true

Exemple CORS:

```http
Access-Control-Allow-Origin: http://localhost:4200
Access-Control-Allow-Credentials: true
Access-Control-Allow-Headers: Content-Type, Authorization
Access-Control-Expose-Headers: X-Auth-Token
```

## 7. Erreurs frequentes et corrections

### 7.1 Intercepteur jamais appele

Causes:

- `requestMatcher` trop strict (URL ne matche pas)
- mode standalone sans `withInterceptorsFromDi()`
- `provideSmallibFront(...)` non present dans `providers`

Correction rapide:

- tester temporairement:

```ts
requestMatcher: () => true
```

Si ca marche, ajuster le matcher avec la vraie URL backend.

### 7.2 Token non ajoute sur la requete

Causes:

- aucun token capture depuis les reponses precedentes
- extraction token mauvaise (header nom incorrect)
- header de requete different cote backend

Corrections:

- verifier `responseHeaderName`, `requestHeaderName`, `requestHeaderPrefix`
- ajouter `onTokenUpdated` pour confirmer la mise a jour du token

### 7.3 Token non lu alors que le backend le renvoie

Cause la plus frequente:

- header custom non expose via CORS (`Access-Control-Expose-Headers`)

Correction:

- ajouter `X-Auth-Token` dans `Access-Control-Expose-Headers`.

### 7.4 Redirection SAML non declenchee

Causes:

- `saml.enabled=false`
- la reponse n'est pas HTML form SAML
- `expectedFormInputName` incorrect
- requete non incluse par `requestMatcher`

Corrections:

- verifier body network (presence `<form` + input `SAMLRequest`)
- adapter `expectedFormInputName`

### 7.5 Apres refresh, plus de token

Cause:

- `storageStrategy: 'memory'` (token perdu au reload)

Correction:

- utiliser `sessionStorage` ou `localStorage`.

## 8. Checklist de validation (ordre recommande)

1. `npm ls smallibfront` OK.
2. Build frontend OK (`npm run build`).
3. `provideSmallibFront(...)` present dans le bootstrap Angular.
4. Matcher valide pour l'URL backend courante (machine cible).
5. Requete API visible dans DevTools avec header `Authorization`.
6. Reponse backend contient `X-Auth-Token`.
7. CORS expose le header (`Access-Control-Expose-Headers`).
8. Hook `onTokenUpdated` affiche bien un token non vide.
9. Si SAML: hook `onSamlAutoSubmit` s'affiche au moment du challenge.

## 9. Configuration recommandee pour demo2-front19

Fichier cible:

- `src/app/app-module.ts`

Configuration recommandee:

```ts
...provideSmallibFront({
  requestMatcher: (request) => request.url.startsWith('http://localhost:8080/demo2/api/'),
  token: {
    responseHeaderName: 'X-Auth-Token',
    requestHeaderName: 'Authorization',
    requestHeaderPrefix: 'Bearer',
    storageKey: 'demo2front19.token',
    storageStrategy: 'sessionStorage'
  },
  saml: {
    enabled: true,
    expectedFormInputName: 'SAMLRequest',
    relayStateInputName: 'RelayState',
    overrideRelayStateWithCurrentUrl: true
  }
})
```

## 10. Procedure de debug express (5 minutes)

1. Mettre temporairement:
   - `requestMatcher: () => true`
   - hooks `onTokenUpdated` et `onSamlAutoSubmit` avec `console.debug`
2. Ouvrir DevTools > Network sur un appel `/api/...`
3. Verifier:
   - requete sortante contient `Authorization`
   - reponse contient `X-Auth-Token`
   - reponse CORS expose `X-Auth-Token`
4. Si tout est OK, remettre un matcher cible.
5. Si toujours KO, verifier les URLs (localhost/IP/port) entre backend et frontend sur le nouveau PC.

