*Čeština | [English](README.en.md)*

# Identity Server

Vlastní OpenID Connect / OAuth2 identity provider postavený na **Spring Authorization
Serveru** (Spring Boot 4.1, Spring Security 7.1), s malým Next.js klientem, který živě
předvádí celý přihlašovací flow.

Je to portfolio projekt, ne generický klon "JWT auth tutoriálu". Cílem bylo ukázat
skutečnou hloubku pochopení OAuth2/OIDC protokolu a schopnost rozšířit produkční framework
— ne rolovat bezpečnostně kritickou implementaci protokolu od nuly. Celý samotný
authorization code flow, vydávání tokenů i validace PKCE je čistě Springovo, nijak
neupravované. Vlastní je všechno kolem: login/admin UI, datový model uživatelů a MFA,
audit log a vynucování druhého faktoru napojené na nový multi-factor mechanismus Spring
Security 7.1.

## Architektura

```
┌─────────────┐      OAuth2 / OIDC       ┌────────────────────┐      JDBC      ┌───────┐
│ Next.js demo│ ───────────────────────► │  Identity Server    │ ─────────────► │ MySQL │
│   client    │ ◄─────────────────────── │ (Spring Auth Server) │ ◄───────────── │       │
└─────────────┘   authorization_code     └────────────────────┘                └───────┘
                    + PKCE, JWKS
```

- **identity-server** — samotný OIDC provider. Dva security filter chainy: jeden pro
  endpointy `/oauth2/*` a `/.well-known/*` (vlastní od Spring Authorization Serveru), druhý
  pro všechno ostatní (login stránka, správa účtu/MFA, admin panel).
- **demo-client** — confidential OAuth2 klient (Next.js, veškerá OAuth logika jen v
  server-side route handlerech, žádné tokeny v prohlížeči) předvádějící celý flow:
  authorization code + PKCE, rotaci refresh tokenu, revoke, ověření ID tokenu přes JWKS.
- **MySQL** — `oauth2_registered_client`, `oauth2_authorization`,
  `oauth2_authorization_consent` používají standardní Springovo schéma (nepřepisované).
  Vlastní tabulky: `app_user`, `user_role`, `mfa_secret`, `mfa_recovery_code`, `audit_log`.

## Funkce

- **Core OIDC**: authorization_code grant s povinným PKCE, vlastní login stránka, JWKS
  endpoint s RSA-podepsaným JWT.
- **Životní cyklus tokenů**: rotace refresh tokenu (`reuseRefreshTokens=false`), revoke
  endpoint napojený na odhlášení klienta.
- **Admin panel**: CRUD nad OAuth klienty a uživateli, omezeno na `ROLE_ADMIN`, s pojistkami
  proti tomu, aby se admin sám zamkl mimo (nemůže si odebrat vlastní admin roli, deaktivovat
  se ani smazat vlastní účet).
- **MFA (TOTP + recovery kódy)**: vynucováno při loginu pomocí *nativních* primitiv pro
  multi-factor autentizaci ze Spring Security 7.1 (`FactorGrantedAuthority`,
  `AuthorizationManagerFactories.multiFactor()`), ne ručně vyrobeného mechanismu se session
  flagem — podmíněně podle uživatele (vyzváni jsou jen ti, co mají MFA zapnuté), ne globální
  přepínač. Recovery kódy jsou jednorázové a hashované; TOTP secret je šifrovaný (ne
  hashovaný), protože se musí dát zpátky přečíst kvůli ověřování kódů.
- **Audit log**: každý pokus o login i MFA (úspěšný i neúspěšný) se zaznamenává, včetně
  neúspěšných pokusů proti emailu bez odpovídajícího účtu (`user_id` je proto nullable) —
  řízeno listenerem na vlastních autentizačních eventech Spring Security, ne roztroušenými
  ručními log voláními. K vidění v admin panelu, a není to jen pasivní záznam: opakované
  selhání proti stejnému účtu (5x během 15 minut) zamkne další pokusy o heslo *i* TOTP kód
  — využívá se tu stejný log jako zdroj pravdy, místo aby se pokusy počítaly zvlášť.
- **Vypnutí MFA vyžaduje znovu prokázat druhý faktor.** Autentizovaná, ale ještě
  TOTP-neověřená session (stav hned po přihlášení heslem, před MFA výzvou) se dostane na
  stránku správy MFA — záměrně, aby dokončení enrollmentu nezamklo čerstvou session mimo
  vlastní recovery kódy — ale samotné vypnutí MFA pořád vyžaduje aktuální TOTP kód nebo
  recovery kód, takže ukradená, jen-heslem-ověřená session nemůže MFA z účtu jen tak sundat.
- **Testy nad kritickými cestami**: celý OIDC flow, admin RBAC + pojistky proti
  self-lockoutu, vynucování MFA (včetně případů špatný kód/recovery kód/gating admin rout)
  a správnost audit logu — běží přes MockMvc proti skutečné Testcontainers MySQL, bez
  závislosti na už běžící databázi.
- **Lokální stack na jeden příkaz**: `docker compose up` sestaví a spustí MySQL, identity
  server i demo klienta dohromady.
- **CI**: GitHub Actions při každém pushi spustí backendovou testovací sadu a
  frontendový lint + build.

## Rychlý start

```bash
cp .env.example .env                        # pro reálné nasazení vygeneruj skutečné hodnoty
cp demo-client/.env.example demo-client/.env.local
docker compose up --build
```

Pak otevři **http://localhost:3000** a proklikej si přihlašovací flow. Demo admin účet
(nasazený přes Flyway) je `admin@identity-server.dev` / `admin123` — než tohle pustíš
kdekoliv jinde než na vlastním stroji, změň ho nebo smaž.

### Bez Dockeru

```bash
docker compose up mysql              # jen databáze
mvn spring-boot:run                  # identity server na :9000
cd demo-client && npm install && npm run dev   # demo klient na :3000
```

### Testy

```bash
mvn test
```

Používá Testcontainers na spuštění jednorázové MySQL — žádná běžící databáze není potřeba,
ověřeno i s vypnutým docker-compose stackem.

## Zajímavá návrhová rozhodnutí

- **Postaveno na Spring Authorization Serveru, ne na vlastní implementaci OAuth2.**
  Vlastní authorization code flow je známý bezpečnostní risk a neobhajitelná volba na
  pohovoru ("proč sis nevzal prověřenou knihovnu"). Hodnota, co se tu předvádí, je správné
  rozšíření a konfigurace produkčního frameworku, ne reimplementace protokolu.
- **demo-client je confidential klient, ne veřejná SPA.** Veškerá OAuth volání se dějí v
  Next.js route handlerech (server-side), takže client secret může být bezpečně uložen.
  To zároveň odemyká refresh tokeny — Spring Authorization Server je nikdy nevydá klientovi
  s `client_secret: none`, protože takový klient nedokáže při uplatnění tokenu prokázat,
  že je tou samou stranou, které byl token vydán.
- **Vynucování MFA používá vestavěné multi-factor primitivy ze Spring Security 7.1.**
  Přišel jsem tím na reálné, málo zjevné chování frameworku: registrace vlastního beanu
  `AuthorizationManagerFactory` přesměruje *úplně každé* obyčejné volání `.authenticated()`
  / `.hasRole()` v celé appce přes sebe — jakmile ten bean existuje, neexistuje nic jako
  "nefaktorovaná" kontrola. Routy, které mají zůstat dostupné jen s heslem (např. vlastní
  stránka nastavení MFA, než uživatel dokončí výzvu), musí factory explicitně obejít přes
  `AuthenticatedAuthorizationManager`.
- **`audit_log.user_id` je nullable záměrně.** Neúspěšný login proti emailu bez
  odpovídajícího účtu se pořád musí zaznamenat (je to bezpečnostně relevantní signál), a
  není k čemu ho připojit.
- **Docker Compose potřeboval pro demo klienta dvě různé base URL**, ne jednu: prohlížeč
  musí být přesměrován na `/oauth2/authorize` na adrese dostupné z hostitele
  (`localhost:9000`), zatímco volání token/JWKS/revoke, co dělá sám demo klient, potřebují
  identity server dosáhnout přes interní síť Compose (`identity-server:9000`). Claim `iss`
  zapečený v každém tokenu zůstává vždy ta veřejná adresa, protože to je to, co je doopravdy
  podepsané v JWT bez ohledu na to, přes jakou URL byl token získán.

## Známá omezení (záměrná, ne přehlédnutá)

- **JWT podpisový klíč se generuje v paměti při každém restartu.** Pro demo v pořádku;
  reálné nasazení by potřebovalo persistentní (a rotovatelný) klíč, nebo KMS. Restart
  serveru zneplatní všechny dosud vydané tokeny.
- **Životnost access/refresh tokenů je krátká** (1 minuta / 30 minut) záměrně, aby šlo
  refresh-po-vypršení pozorovat v živém demu bez čekání — není to produkční hodnota.
- **Nasazený admin účet a výchozí secrety** (`DEMO_CLIENT_SECRET`, `MFA_ENCRYPTION_KEY`)
  jsou dev-only placeholdery commitnuté do `.env.example` kvůli bezproblémovému
  `docker compose up`. Než tohle poběží kdekoliv dostupném komukoliv jinému, vyměň je
  všechny.

## Roadmapa — co dál

- Multi-tenant retrofit dalších projektů (např. Monitoring Dashboard appky) jako OAuth2
  klientů tohoto identity serveru — záměrně mimo scope MVP, ne blocker.
- Persistentní/rotovatelný JWK podpisový klíč pro reálné nasazení.
- WebAuthn/passkeys jako další MFA faktor vedle TOTP.
