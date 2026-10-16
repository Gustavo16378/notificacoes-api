# Notificações API

Microsserviço de envio de notificações por e-mail, feito para trabalhar junto com a [Orçamento API](https://github.com/Gustavo16378/orcamento-api). Quando algo acontece com um orçamento, a Orçamento API publica um evento no RabbitMQ, este serviço consome a mensagem, registra a notificação no banco e dispara o e-mail em HTML.

Também expõe uma API REST para criar, consultar e gerenciar notificações manualmente.

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.5 |
| Mensageria | RabbitMQ (Spring AMQP) |
| Banco | PostgreSQL 16 |
| Persistência | Spring Data JPA + Hibernate |
| Migrations | Flyway |
| E-mail | Spring Mail (SMTP) |
| Documentação | springdoc-openapi (Swagger UI) |
| Infra | Docker + Docker Compose |

## Como funciona

```
Orçamento API ──evento──▶ RabbitMQ (fila "notifications") ──▶ Notificações API ──▶ SMTP ──▶ cliente
                                                                    │
                                                                    ▼
                                                               PostgreSQL
```

1. A Orçamento API publica um evento na fila `notifications` com destinatário, assunto e corpo em HTML.
2. O `NotificationConsumer` recebe a mensagem e salva a notificação com status `PENDING`.
3. O `EmailNotificationService` envia o e-mail e atualiza o status para `SENT`, ou para `FAILED` guardando a mensagem de erro.

Cada notificação guarda o `externalReferenceId`, que é o ID do orçamento na Orçamento API. Assim dá para rastrear todos os e-mails de um mesmo orçamento.

## Recursos

- Consumo assíncrono de eventos via RabbitMQ
- Envio de e-mails em HTML com rastreamento de status (`PENDING`, `SENT`, `FAILED`)
- Registro do erro quando o envio falha, para facilitar o debug
- CRUD REST com validação dos dados de entrada
- Soft delete, com endpoint próprio para listar as notificações removidas
- Paginação nas listagens
- Auditoria com `created_at` e `updated_at`, este mantido por trigger no banco
- Schema versionado com Flyway

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/notifications` | Cria uma notificação e envia o e-mail |
| `PUT` | `/notifications/{id}` | Atualiza uma notificação |
| `GET` | `/notifications/{id}` | Busca uma notificação por ID |
| `GET` | `/notifications/getAll` | Lista as notificações ativas, com paginação |
| `DELETE` | `/notifications/{id}` | Remove uma notificação (soft delete) |
| `GET` | `/notifications/deleted` | Lista as notificações removidas, com paginação |

A documentação completa fica no Swagger UI, em `/swagger-ui.html`.

### Exemplo de requisição

```json
POST /notifications
{
  "externalReferenceId": "f1e2d3c4-b5a6-7890-1234-567890abcdef",
  "recipientEmail": "cliente@email.com",
  "recipientName": "João da Silva",
  "subject": "Seu orçamento foi criado",
  "bodyHtml": "<h1>Olá, João!</h1><p>Seu orçamento foi criado com sucesso.</p>"
}
```

## Como rodar

### Com Docker

Este serviço usa o RabbitMQ que sobe junto com a Orçamento API, pela rede Docker compartilhada `ms-shared`. Por isso, suba a Orçamento API primeiro.

```bash
# cria a rede compartilhada (só na primeira vez)
docker network create ms-shared

# sobe o banco e a API
docker compose up -d --build
```

A API fica disponível em `http://localhost:8081` e o banco em `localhost:5433`.

### Localmente

Pré-requisitos: Java 21, PostgreSQL rodando na porta 5433 e um RabbitMQ acessível.

```bash
./mvnw spring-boot:run
```

### Configuração de e-mail

O envio usa SMTP. Configure as credenciais por variáveis de ambiente, sem deixar senha no código:

```bash
SPRING_MAIL_USERNAME=seu-email@gmail.com
SPRING_MAIL_PASSWORD=sua-senha-de-app
SPRING_MAIL_FROM=seu-email@gmail.com
```

No Gmail, use uma [senha de app](https://myaccount.google.com/apppasswords) em vez da senha da conta.

## Estrutura

```
src/main/java/com/orcamento/notification
├── config/        configuração do RabbitMQ
├── controller/    endpoints REST
├── dto/           objetos de entrada, saída e evento
├── entity/        entidade Notification e enum de status
├── messaging/     consumidor da fila
├── repository/    acesso a dados
└── service/       regras de negócio e envio de e-mail

src/main/resources/db/migration
├── V1__create_schema.sql
├── V2__triggers.sql
└── V3__seed_dev_data.sql
```

## Autor

Desenvolvido por [Gustavo Barbosa Lima](https://gustavodev.dev).
