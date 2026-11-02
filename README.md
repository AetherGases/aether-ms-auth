# Aether MS Auth

Microsserviço de identidade do [Aether](https://github.com/AetherGases): autentica usuários, emite tokens JWT, mantém o perfil e as notificações e recebe mensagens de suporte.

Faz parte do ecossistema descrito no [perfil da organização](https://github.com/AetherGases). O serviço fica atrás do [Kong](https://github.com/AetherGases/aether-kong-gateway), que encaminha `/api/auth` e `/api/profile` para ele.

## Responsabilidades

| Módulo | Rotas | O que faz |
| --- | --- | --- |
| `auth` | `POST /api/auth/login`, `PUT /api/auth/refresh/{email}` | Login por e-mail e senha, emissão de access e refresh token |
| `auth` | `POST /api/auth/reset-password/{send-code,resend-code,validate-code,change-password}` | Recuperação de senha com código enviado por e-mail (Brevo) |
| `profile` | `GET` e `PATCH /api/profile` | Perfil do usuário autenticado |
| `notification` | `GET /api/profile/notifications` | Notificações do usuário |
| `support` | `POST /api/support` | Registro de um problema reportado no sistema |

Os serviços `calculator` e `cloudinary` não consultam usuários: validam o JWT de forma stateless (assinatura, expiração e claims) com o mesmo `JWT_SECRET`. O `inventory` valida o JWT e carrega as permissões do funcionário no banco.

## Stack

- Java 21, Spring Boot 4.1 (Web MVC, Security, Data JPA, Data MongoDB, Data Redis), Maven
- PostgreSQL (usuários, empresas, permissões, planos), MongoDB (notificações) e Redis (códigos de recuperação de senha)
- Brevo (e-mail transacional) e Cloudinary (configuração de arquivos de perfil)
- SpringDoc OpenAPI, Lombok, Bean Validation, JUnit 5 e Mockito

## Arquitetura

Camadas por módulo de funcionalidade, mais um módulo `shared`:

```text
src/main/java/com/aether/ms_auth
├── auth/ · profile/ · notification/ · support/   # cada módulo: controllers, dto (input/output/request), mappers, services
└── shared/
    ├── config/        # Security, CORS, Redis, Brevo, Cloudinary, OpenAPI, Jackson
    ├── persistence/   # postgres/entities, mongo/documents, redis/entities (+ repositories)
    ├── exceptions/ · handlers/   # exceções de domínio e GlobalExceptionHandler
    ├── docs/          # interfaces *ControllerDocs com as anotações do Swagger
    └── helpers/ · enums/
```

Convenções que se repetem nos microsserviços Java:

- Controller recebe `*RequestDTO`, converte para `*InputDTO` pelo mapper, chama o service e devolve um `*OutputDTO`.
- As anotações do Swagger ficam em interfaces `*ControllerDocs`, não nos controllers.
- Mensagens de erro vêm de `messages/messages.properties`.
- O schema é gerenciado pelos scripts SQL do [aether-toolskit](https://github.com/AetherGases/aether-toolskit). O Hibernate roda com `ddl-auto: validate` e nunca altera o banco.

## Executando localmente

Pré-requisitos: Java 21, Maven 3.9+ e os bancos do [aether-toolskit](https://github.com/AetherGases/aether-toolskit) (`docker compose up -d` lá sobe PostgreSQL, MongoDB e Redis com o schema).

```bash
git clone https://github.com/AetherGases/aether-ms-auth.git
cd aether-ms-auth
cp .env.example .env     # preencha os valores; veja a tabela abaixo
mvn spring-boot:run
```

O `.env` é lido pelo próprio Spring (`spring.config.import`), então não precisa exportar variáveis. A API sobe em `http://localhost:${API_PORT}` (padrão `8080`) e o Swagger fica em `/swagger-ui/index.html`.

### Variáveis de ambiente

| Variável | Descrição |
| --- | --- |
| `API_PORT`, `SPRING_PROFILES_ACTIVE` | Porta (padrão `8080`) e perfil (`dev`) |
| `POSTGRES_HOST/PORT/USER/PASSWORD/DB` | PostgreSQL B (`dbAether2Year`) |
| `MONGO_URI` | URI do MongoDB (notificações). As variáveis `MONGO_HOST/PORT/USER/PASSWORD/DB` do exemplo não são usadas pelo `application.yml` |
| `REDIS_HOST/PORT/USER/PASSWORD` | Redis (códigos de recuperação) |
| `BREVO_API_KEY`, `BREVO_SENDER_EMAIL`, `BREVO_SENDER_NAME` | E-mail transacional |
| `CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET` | Credenciais do Cloudinary |
| `JWT_SECRET` | Segredo HMAC. **Precisa ser o mesmo** em inventory, calculator e cloudinary |
| `JWT_TOKEN_EXPIRE_LENGTH`, `JWT_REFRESH_EXPIRE_LENGTH` | Validade em ms (padrões: 1 h e 24 h) |
| `CORS_ORIGIN_PATTERNS` | Origens permitidas |

## Testes

```bash
mvn clean compile test -DskipTests=false
```

O CI (`tests.yml`) roda isso a cada push e PR para `main`, `staging` e `develop`, com PostgreSQL 16.10, MongoDB 8.0.13 e Redis 8.2 como serviços.

## Deploy

Publicar uma **release** dispara `deploy.yml`, que chama o workflow compartilhado da organização ([.github](https://github.com/AetherGases/.github)). Ele roda o gate de CI, constrói a imagem (`Dockerfile` multi-stage, Temurin 21 Alpine), envia ao ECR e atualiza o Deployment no EKS. O container escuta na porta 8080 e as variáveis chegam pelo Secret `aether-env`.

## Pontos de atenção

- Os enums e entidades em `shared/` são cópias de um núcleo comum aos microsserviços Java. Uma mudança no schema precisa ser replicada em cada serviço.
- O `.env.example` traz variáveis de Mongo que o `application.yml` não lê (só `MONGO_URI`). Confira o `application.yml` antes de assumir que uma variável tem efeito.
- Rotas públicas: login, refresh, as quatro rotas de reset de senha e o Swagger. O restante de `/api/**` exige JWT.
