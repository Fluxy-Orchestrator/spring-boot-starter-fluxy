# spring-boot-starter-fluxy

Auto-configuración de Spring Boot 4 para el ecosistema **Fluxy** — motor de ejecución de flows, steps y tasks.

---

## ¿Qué incluye?

| Componente | Descripción |
|---|---|
| `FluxyAutoConfiguration` | Clase principal de auto-configuración, cargada via SPI |
| `FluxyDataAutoConfiguration` | Registra entidades JPA y repositorios de `fluxy-spring` usando el datasource primario |
| `FluxyDedicatedDataSourceAutoConfiguration` | Crea un datasource, EMF y TransactionManager dedicados para Fluxy |
| `FluxyWebAutoConfiguration` | Controladores REST (activos sólo con Spring MVC en classpath) |
| `FluxySpringEventBusAutoConfiguration` | Bus de eventos con Spring Application Events (por defecto) |
| `FluxyAwsSqsEventBusAutoConfiguration` | Bus de eventos con Amazon SQS |
| `FluxyRabbitMqEventBusAutoConfiguration` | Bus de eventos con RabbitMQ |
| `TaskExecutorService` | Bean del core — ejecuta una tarea y publica evento en el bus configurado |
| `StepExecutionService` | Bean del core — orquesta la ejecución secuencial de tareas en un step |
| `FlowExecutor` | Bean del core — orquesta la ejecución de steps dentro de un flow |
| `FluxyExecutionService` | Servicio de ejecución a demanda (inicializar flows, procesar steps, ejecutar tasks) |
| `FluxyTaskRegistry` | Registro en memoria de todos los beans `FluxyTask` del contexto |
| `TaskAutoRegistrationProcessor` | Auto-registra en BD las clases `@Task + FluxyTask` al arrancar |

---

## Dependencia Gradle

```groovy
// En settings.gradle — si usas composite builds locales:
includeBuild('../spring-boot-starter-fluxy')

// En build.gradle:
dependencies {
    implementation 'org.fluxy:spring-boot-starter-fluxy:1.0.0'
}
```

También necesitas en tu aplicación:
```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-web'   // opcional, para los REST endpoints
    runtimeOnly 'com.h2database:h2'   // o tu base de datos preferida
}
```

---

## Configuración completa en `application.yml`

A continuación se detalla el **YAML completo** con todas las propiedades disponibles, sus valores por defecto y su significado:

```yaml
fluxy:

  # ──────────────────────────────────────────────────────────────────────────────
  # DATASOURCE DEDICADO (opcional)
  # ──────────────────────────────────────────────────────────────────────────────
  # Cuando fluxy.datasource.url está definido, el starter crea una
  # infraestructura JPA completamente independiente del datasource primario:
  #   - fluxyDataSource          (HikariCP propio)
  #   - fluxyEntityManagerFactory (EMF dedicado)
  #   - fluxyTransactionManager   (TransactionManager propio)
  #
  # Si NO se define fluxy.datasource.url, Fluxy usa el datasource primario
  # de la aplicación automáticamente — no se necesita configurar nada aquí.
  # ──────────────────────────────────────────────────────────────────────────────
  datasource:
    url: jdbc:postgresql://localhost:5432/fluxydb       # JDBC URL — activa datasource dedicado
    driver-class-name: org.postgresql.Driver             # Driver JDBC (auto-detectado si se omite)
    username: fluxy_user                                 # Usuario de la BD
    password: fluxy_pass                                 # Contraseña de la BD

    hikari:                                              # Configuración del pool HikariCP
      pool-name: FluxyHikariPool                         # Nombre del pool (default: FluxyHikariPool)
      maximum-pool-size: 10                              # Máximo de conexiones (default: 10)
      minimum-idle: 2                                    # Mínimo de conexiones inactivas (default: 2)
      connection-timeout: 30000                          # Timeout para obtener conexión, ms (default: 30000)
      idle-timeout: 600000                               # Tiempo máximo inactiva, ms (default: 600000)
      max-lifetime: 1800000                              # Vida máxima de una conexión, ms (default: 1800000)

  # ──────────────────────────────────────────────────────────────────────────────
  # JPA / HIBERNATE (solo aplica con datasource dedicado)
  # ──────────────────────────────────────────────────────────────────────────────
  # Estas propiedades configuran el EntityManagerFactory dedicado de Fluxy.
  # Si Fluxy usa el datasource primario, se ignoran y aplican las
  # propiedades estándar de spring.jpa.* en su lugar.
  # ──────────────────────────────────────────────────────────────────────────────
  jpa:
    show-sql: false                                      # Mostrar SQL en consola (default: false)
    generate-ddl: false                                  # Generar DDL automáticamente (default: false)
    database-platform: org.hibernate.dialect.PostgreSQLDialect  # Dialecto Hibernate (auto-detectado si se omite)

    hibernate:
      ddl-auto: none                                     # Estrategia de esquema: none | validate | update | create | create-drop
                                                         # (default: none — recomendado para producción)

  # ──────────────────────────────────────────────────────────────────────────────
  # REGISTRO Y VALIDACIÓN DE TASKS
  # ──────────────────────────────────────────────────────────────────────────────
  # Controla el comportamiento al arrancar la aplicación respecto a los beans
  # que extienden FluxyTask y están anotados con @Task.
  # ──────────────────────────────────────────────────────────────────────────────
  task:
    registration:
      auto-register: true                                # Persistir @Task en BD al arrancar (default: true)
                                                         # Si es false, las tareas viven en el contexto Spring
                                                         # pero NO se insertan en la base de datos.

      cleanup-stale:                                     # Detección de tareas huérfanas
        enabled: false                                   # Activar detección (default: false)
                                                         # Detecta tareas que existen en BD pero cuya
                                                         # clase @Task ya no está en el código.
        mode: WARN                                       # Modo de manejo: WARN | FAIL (default: WARN)
                                                         #   WARN — log warning por cada huérfana
                                                         #   FAIL — lanza StaleTaskException, detiene arranque

      validate-steps: false                              # Validar versiones de tasks en steps (default: false)
                                                         # Verifica que cada tarea referenciada por un step
                                                         # exista en BD con la misma versión del código.
                                                         #   - Versión más nueva en BD → emite WARN
                                                         #   - Tarea no existe o versión anterior → lanza
                                                         #     StepTaskVersionMismatchException

  # ──────────────────────────────────────────────────────────────────────────────
  # BUS DE EVENTOS
  # ──────────────────────────────────────────────────────────────────────────────
  # Selecciona la implementación del bus de eventos de Fluxy y configura
  # los parámetros específicos del proveedor elegido.
  # ──────────────────────────────────────────────────────────────────────────────
  eventbus:
    type: SPRING                                         # Implementación: SPRING | SQS | RABBIT
                                                         # (default: SPRING)

    # ── Amazon SQS (solo cuando type = SQS) ─────────────────────────────────
    sqs:
      queue-url: https://sqs.us-east-1.amazonaws.com/123456789012/fluxy-events
      region: us-east-1                                  # Región AWS (usa default del SDK si se omite)

    # ── RabbitMQ (solo cuando type = RABBIT) ─────────────────────────────────
    rabbit:
      host: localhost                                    # Host del broker (default: localhost)
      port: 5672                                         # Puerto AMQP (default: 5672)
      username: guest                                    # Usuario (default: guest)
      password: guest                                    # Contraseña (default: guest)
      virtual-host: /                                    # Virtual host (default: /)
      queue: fluxy-events                                # Cola de consumo (default: fluxy-events)
      exchange: fluxy-exchange                           # Exchange de publicación (default: fluxy-exchange)
      routing-key: fluxy.events                          # Routing key (default: fluxy.events)
```

---

## Referencia detallada de propiedades

### `fluxy.datasource.*` — Datasource dedicado

| Propiedad | Tipo | Default | Descripción |
|-----------|------|---------|-------------|
| `fluxy.datasource.url` | `String` | — | JDBC URL. **Cuando se define**, activa un datasource completamente independiente del primario. |
| `fluxy.datasource.driver-class-name` | `String` | _(auto)_ | Clase del driver JDBC. Se auto-detecta a partir de la URL si no se especifica. |
| `fluxy.datasource.username` | `String` | — | Usuario de la base de datos. |
| `fluxy.datasource.password` | `String` | — | Contraseña de la base de datos. |
| `fluxy.datasource.hikari.pool-name` | `String` | `FluxyHikariPool` | Nombre identificativo del pool HikariCP. |
| `fluxy.datasource.hikari.maximum-pool-size` | `int` | `10` | Número máximo de conexiones simultáneas en el pool. |
| `fluxy.datasource.hikari.minimum-idle` | `int` | `2` | Número mínimo de conexiones inactivas que se mantienen abiertas. |
| `fluxy.datasource.hikari.connection-timeout` | `long` | `30000` | Tiempo máximo (ms) que un hilo espera para obtener una conexión del pool. |
| `fluxy.datasource.hikari.idle-timeout` | `long` | `600000` | Tiempo máximo (ms) que una conexión puede permanecer inactiva antes de cerrarse. |
| `fluxy.datasource.hikari.max-lifetime` | `long` | `1800000` | Tiempo máximo de vida (ms) de una conexión antes de ser reciclada. |

> **Infraestructura requerida:** Una base de datos relacional accesible (PostgreSQL, MySQL, H2, etc.) con el esquema de tablas de Fluxy creado. Cuando no se define `fluxy.datasource.url`, Fluxy reutiliza el datasource primario de Spring Boot y **no se requiere configuración adicional**.

### `fluxy.jpa.*` — JPA/Hibernate dedicado

> Solo tiene efecto cuando `fluxy.datasource.url` está definido. En caso contrario, se aplican las propiedades estándar `spring.jpa.*`.

| Propiedad | Tipo | Default | Descripción |
|-----------|------|---------|-------------|
| `fluxy.jpa.show-sql` | `boolean` | `false` | Muestra las sentencias SQL generadas por Hibernate en la consola. |
| `fluxy.jpa.generate-ddl` | `boolean` | `false` | Permite la generación automática de DDL (esquema) por parte de Hibernate. |
| `fluxy.jpa.database-platform` | `String` | _(auto)_ | Clase del dialecto Hibernate (ej: `org.hibernate.dialect.PostgreSQLDialect`). Se auto-detecta si se omite. |
| `fluxy.jpa.hibernate.ddl-auto` | `String` | `none` | Estrategia de generación de esquema: `none`, `validate`, `update`, `create`, `create-drop`. |

**Valores de `ddl-auto`:**

| Valor | Comportamiento |
|-------|---------------|
| `none` | No realiza cambios en el esquema (**recomendado para producción**). |
| `validate` | Valida que el esquema existente coincida con las entidades. Falla si no coinciden. |
| `update` | Actualiza el esquema incrementalmente según las entidades (útil en desarrollo). |
| `create` | Crea el esquema al iniciar, **destruyendo datos previos**. |
| `create-drop` | Crea el esquema al iniciar y lo elimina al cerrar la aplicación. |

### `fluxy.task.registration.*` — Registro y validación de tasks

| Propiedad | Tipo | Default | Descripción |
|-----------|------|---------|-------------|
| `fluxy.task.registration.auto-register` | `boolean` | `true` | Persiste automáticamente en BD los beans `@Task` que extienden `FluxyTask`. Si es `false`, las tareas solo existen en el contexto Spring. |
| `fluxy.task.registration.cleanup-stale.enabled` | `boolean` | `false` | Activa la detección de tareas huérfanas (en BD pero sin `@Task` en el código). |
| `fluxy.task.registration.cleanup-stale.mode` | `WARN` \| `FAIL` | `WARN` | `WARN`: log warning por cada huérfana. `FAIL`: lanza `StaleTaskException` e impide el arranque. |
| `fluxy.task.registration.validate-steps` | `boolean` | `false` | Valida que cada task referenciada en un step exista en BD con la versión correcta. |

> **Infraestructura requerida:** Base de datos con las tablas `fluxy_task`, `step_task`, etc. ya creadas. Se necesita al menos `spring-boot-starter-data-jpa` y un driver JDBC en el classpath. Los repositorios `FluxyTaskRepository` y `StepTaskRepository` deben estar disponibles en el contexto para que el `TaskAutoRegistrationProcessor` se active.

### `fluxy.eventbus.*` — Bus de eventos

| Propiedad | Tipo | Default | Descripción |
|-----------|------|---------|-------------|
| `fluxy.eventbus.type` | `SPRING` \| `SQS` \| `RABBIT` | `SPRING` | Selecciona la implementación del bus de eventos. |

#### Tipo `SPRING` (Spring Application Events)

> **Infraestructura requerida:** Ninguna adicional. Funciona con Spring Context estándar.

Comunicación local dentro de la misma JVM. Los eventos se publican vía `ApplicationEventPublisher` y se consumen con `@EventListener`. Es la opción por defecto y no requiere ninguna dependencia ni configuración extra.

**Componentes registrados:**
- `SpringFluxyEventBus` — publica `FluxyApplicationEvent` vía `ApplicationEventPublisher`
- `FluxySpringEventListener` — captura con `@EventListener` y despacha a los handlers

#### Tipo `SQS` (Amazon Simple Queue Service)

> **Infraestructura requerida:**
> - Cola SQS creada en AWS
> - Credenciales AWS configuradas (variables de entorno, perfil IAM, `~/.aws/credentials`, etc.)
> - Dependencia `io.awspring.cloud:spring-cloud-aws-sqs` en el classpath

| Propiedad | Tipo | Default | Descripción |
|-----------|------|---------|-------------|
| `fluxy.eventbus.sqs.queue-url` | `String` | — | URL completa de la cola SQS (ej: `https://sqs.us-east-1.amazonaws.com/123456789012/fluxy-events`). **Obligatorio.** |
| `fluxy.eventbus.sqs.region` | `String` | _(SDK default)_ | Región de AWS donde reside la cola (ej: `us-east-1`). Si se omite, usa la región por defecto del AWS SDK. |

**Componentes registrados:**
- `SqsAsyncClient` — cliente AWS SQS dedicado para Fluxy
- `SqsTemplate` — template de Spring Cloud AWS para envío
- `SqsFluxyEventBus` — serializa eventos a JSON y los envía a la cola
- `FluxyAwsSqsListener` — consume mensajes con `@SqsListener`, deserializa y despacha

**Ejemplo mínimo:**
```yaml
fluxy:
  eventbus:
    type: SQS
    sqs:
      queue-url: https://sqs.us-east-1.amazonaws.com/123456789012/fluxy-events
      region: us-east-1
```

**Dependencia Gradle requerida:**
```groovy
implementation 'io.awspring.cloud:spring-cloud-aws-sqs:3.3.1'
```

#### Tipo `RABBIT` (RabbitMQ)

> **Infraestructura requerida:**
> - Servidor RabbitMQ accesible
> - Dependencia `org.springframework.amqp:spring-rabbit` en el classpath

| Propiedad | Tipo | Default | Descripción |
|-----------|------|---------|-------------|
| `fluxy.eventbus.rabbit.host` | `String` | `localhost` | Host del servidor RabbitMQ. |
| `fluxy.eventbus.rabbit.port` | `int` | `5672` | Puerto AMQP del servidor. |
| `fluxy.eventbus.rabbit.username` | `String` | `guest` | Usuario de autenticación. |
| `fluxy.eventbus.rabbit.password` | `String` | `guest` | Contraseña de autenticación. |
| `fluxy.eventbus.rabbit.virtual-host` | `String` | `/` | Virtual host de RabbitMQ. |
| `fluxy.eventbus.rabbit.queue` | `String` | `fluxy-events` | Nombre de la cola donde se consumen los eventos. |
| `fluxy.eventbus.rabbit.exchange` | `String` | `fluxy-exchange` | Exchange de tipo topic donde se publican los eventos. |
| `fluxy.eventbus.rabbit.routing-key` | `String` | `fluxy.events` | Routing key para publicar y enlazar la cola al exchange. |

**Componentes registrados:**
- `CachingConnectionFactory` — connection factory dedicado para Fluxy (aislado del principal)
- `RabbitTemplate` — template dedicado para envío de mensajes
- `Queue` — cola durable para consumo
- `TopicExchange` — exchange de tipo topic
- `Binding` — enlace entre cola y exchange con el routing key configurado
- `RabbitFluxyEventBus` — serializa eventos a JSON y los envía al exchange
- `FluxyRabbitListener` — consume mensajes con `@RabbitListener`, deserializa y despacha

**Ejemplo mínimo:**
```yaml
fluxy:
  eventbus:
    type: RABBIT
    rabbit:
      host: localhost
      port: 5672
      username: guest
      password: guest
```

**Dependencia Gradle requerida:**
```groovy
implementation 'org.springframework.amqp:spring-rabbit:3.2.4'
```

> **Nota:** La infraestructura RabbitMQ de Fluxy (connection factory, template, exchange, cola) es completamente independiente de la configuración RabbitMQ principal de la aplicación. No interfiere con beans existentes de `spring-boot-starter-amqp`.

---

## Auto-registro de Tasks

Cualquier clase que:
1. Extienda `FluxyTask` (de `fluxy-core`)
2. Esté anotada con `@Task` (de `fluxy-spring`)

…será **automáticamente registrada** en la base de datos al arrancar la aplicación (si `fluxy.task.registration.auto-register` es `true`).

### Ejemplo

```java
@Task(name = "enviar-email", description = "Envía un email de notificación", version = 1)
public class EnviarEmailTask extends FluxyTask {

    public EnviarEmailTask() {
        this.name = "enviar-email";
    }

    @Override
    public TaskResult execute(ExecutionContext ctx) {
        String destinatario = ctx.getVariable("email").orElseThrow();
        // lógica de envío...
        return TaskResult.SUCCESS;
    }
}
```

El starter detectará este bean al arrancar y ejecutará (idempotentemente):
```sql
INSERT INTO fluxy_task (id, name) VALUES (gen_random_uuid(), 'enviar-email')
ON CONFLICT DO NOTHING;
```

### Más ejemplos de Tasks por código

#### Task con inyección de dependencias Spring

Las tasks de Fluxy son beans de Spring, por lo que puedes inyectar cualquier servicio o repositorio:

```java
@Task(name = "generar-pdf", description = "Genera un PDF de factura", version = 1)
public class GenerarPdfTask extends FluxyTask {

    private final PdfService pdfService;
    private final FacturaRepository facturaRepository;

    public GenerarPdfTask(PdfService pdfService, FacturaRepository facturaRepository) {
        this.name = "generar-pdf";
        this.pdfService = pdfService;
        this.facturaRepository = facturaRepository;
    }

    @Override
    public TaskResult execute(ExecutionContext ctx) {
        String facturaId = ctx.getVariable("facturaId").orElseThrow();

        Factura factura = facturaRepository.findById(UUID.fromString(facturaId))
                .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

        byte[] pdf = pdfService.generar(factura);

        // Puedes escribir variables de vuelta al contexto
        ctx.setVariable("pdfGenerado", "true");
        ctx.setVariable("pdfSize", String.valueOf(pdf.length));

        return TaskResult.SUCCESS;
    }
}
```

#### Task con manejo de errores y resultado condicional

```java
@Task(name = "notificar-slack", description = "Envía notificación a Slack", version = 2)
public class NotificarSlackTask extends FluxyTask {

    private final SlackClient slackClient;

    public NotificarSlackTask(SlackClient slackClient) {
        this.name = "notificar-slack";
        this.slackClient = slackClient;
    }

    @Override
    public TaskResult execute(ExecutionContext ctx) {
        String canal = ctx.getVariable("slackChannel").orElse("#general");
        String mensaje = ctx.getVariable("mensaje").orElse("Proceso completado");

        try {
            slackClient.send(canal, mensaje);
            return TaskResult.SUCCESS;
        } catch (SlackApiException e) {
            // Registrar el error en el contexto para que tasks posteriores puedan leerlo
            ctx.setVariable("error", e.getMessage());
            return TaskResult.FAILURE;
        }
    }
}
```

#### Task con uso de referencias del contexto

Las referencias permiten vincular la ejecución con entidades externas (IDs de orden, cliente, etc.):

```java
@Task(name = "validar-inventario", description = "Verifica stock disponible", version = 1)
public class ValidarInventarioTask extends FluxyTask {

    private final InventarioService inventarioService;

    public ValidarInventarioTask(InventarioService inventarioService) {
        this.name = "validar-inventario";
        this.inventarioService = inventarioService;
    }

    @Override
    public TaskResult execute(ExecutionContext ctx) {
        // Leer referencias del contexto
        String orderId = ctx.getReference("orderId").orElseThrow();
        String productoId = ctx.getVariable("productoId").orElseThrow();
        int cantidad = Integer.parseInt(ctx.getVariable("cantidad").orElse("1"));

        boolean disponible = inventarioService.verificarStock(productoId, cantidad);
        ctx.setVariable("stockDisponible", String.valueOf(disponible));

        if (!disponible) {
            ctx.setVariable("motivoFallo", "Stock insuficiente para producto " + productoId);
            return TaskResult.FAILURE;
        }

        return TaskResult.SUCCESS;
    }
}
```

> **Tip:** Todas las tasks se registran automáticamente en BD al arrancar si `fluxy.task.registration.auto-register=true`. También puedes crearlas manualmente vía API REST (ver guía paso a paso más adelante).

---

## Bus de eventos — `FluxyEventHandler`

Implementa `FluxyEventHandler` para recibir eventos del bus, independientemente de la implementación seleccionada:

```java
@Component
public class MyFluxyHandler implements FluxyEventHandler {

    @Override
    public void handle(FluxyEvent<?, ?> event) {
        Object payload = event.getPayload();
        ExecutionContext ctx = event.getContext();
        // lógica de negocio
    }
}
```

Para publicar eventos:

```java
@Autowired
private FluxyEventsBus eventBus;

eventBus.publish(new FluxyEvent<>(source, payload, context));
```

---

## Endpoints REST

> Todos bajo el prefijo `/fluxy`. Se activan automáticamente si `spring-boot-starter-web` está en el classpath y los repositorios JPA correspondientes están disponibles.

### FluxyTask — `/fluxy/tasks`

| Método | URL | Descripción |
|--------|-----|-------------|
| `GET` | `/fluxy/tasks` | Lista todas las tareas |
| `GET` | `/fluxy/tasks/{id}` | Busca por ID |
| `GET` | `/fluxy/tasks/name/{name}` | Busca por nombre |
| `POST` | `/fluxy/tasks` | Crea una tarea manualmente |
| `DELETE` | `/fluxy/tasks/{id}` | Elimina una tarea por ID |
| `DELETE` | `/fluxy/tasks/name/{name}` | Elimina una tarea por nombre |

**POST body:** `{ "name": "mi-tarea", "version": 1, "description": "..." }`

### FluxyStep — `/fluxy/steps`

| Método | URL | Descripción |
|--------|-----|-------------|
| `GET` | `/fluxy/steps` | Lista todos los steps |
| `GET` | `/fluxy/steps/{id}` | Busca por ID |
| `GET` | `/fluxy/steps/name/{name}` | Busca por nombre |
| `POST` | `/fluxy/steps` | Crea un step |
| `POST` | `/fluxy/steps/{id}/tasks` | Agrega una tarea al step (por IDs) |
| `POST` | `/fluxy/steps/name/{name}/tasks` | Agrega una tarea al step (por nombres) |
| `DELETE` | `/fluxy/steps/{stepId}/tasks/{taskId}` | Elimina tarea del step (por IDs) |
| `DELETE` | `/fluxy/steps/name/{stepName}/tasks/name/{taskName}` | Elimina tarea del step (por nombres) |
| `DELETE` | `/fluxy/steps/{id}` | Elimina un step por ID |
| `DELETE` | `/fluxy/steps/name/{name}` | Elimina un step por nombre |

**POST /fluxy/steps/{id}/tasks body:** `{ "taskId": "uuid", "order": 1 }`

**POST /fluxy/steps/name/{name}/tasks body:** `{ "taskName": "enviar-email", "order": 1 }`

### FluxyFlow — `/fluxy/flows`

| Método | URL | Descripción |
|--------|-----|-------------|
| `GET` | `/fluxy/flows` | Lista todos los flows |
| `GET` | `/fluxy/flows/{id}` | Busca por ID |
| `GET` | `/fluxy/flows/name/{name}` | Busca por nombre |
| `GET` | `/fluxy/flows/type/{type}` | Filtra por tipo |
| `POST` | `/fluxy/flows` | Crea un flow |
| `POST` | `/fluxy/flows/{id}/steps` | Agrega un step al flow (por IDs) |
| `POST` | `/fluxy/flows/name/{name}/steps` | Agrega un step al flow (por nombres) |
| `DELETE` | `/fluxy/flows/{flowId}/steps/{stepId}` | Elimina step del flow (por IDs) |
| `DELETE` | `/fluxy/flows/name/{flowName}/steps/name/{stepName}` | Elimina step del flow (por nombres) |
| `DELETE` | `/fluxy/flows/{id}` | Elimina un flow por ID |
| `DELETE` | `/fluxy/flows/name/{name}` | Elimina un flow por nombre |

**POST /fluxy/flows body:** `{ "name": "mi-flow", "type": "BATCH", "description": "..." }`

**POST /fluxy/flows/{id}/steps body:** `{ "stepId": "uuid", "order": 1 }`

**POST /fluxy/flows/name/{name}/steps body:** `{ "stepName": "step-notificacion", "order": 1 }`

### Ejecución — `/fluxy/execution`

Endpoints para la ejecución a demanda de flows, steps y tasks. Cada ejecución de tarea se delega al `TaskExecutorService` del core, que publica eventos en el bus configurado (SPRING, SQS o RABBIT).

| Método | URL | Descripción |
|--------|-----|-------------|
| `POST` | `/fluxy/execution/flows/{id}/initialize` | Inicializa un flow por ID |
| `POST` | `/fluxy/execution/flows/name/{name}/initialize` | Inicializa un flow por nombre |
| `POST` | `/fluxy/execution/flows/{id}/process` | Procesa el siguiente step (por ID) |
| `POST` | `/fluxy/execution/flows/name/{name}/process` | Procesa el siguiente step (por nombre) |
| `POST` | `/fluxy/execution/steps/{id}/process` | Procesa un step a demanda (por ID) |
| `POST` | `/fluxy/execution/steps/name/{name}/process` | Procesa un step a demanda (por nombre) |
| `POST` | `/fluxy/execution/tasks/{name}/execute` | Ejecuta una tarea por nombre |

**Body (todos los endpoints):** `ExecutionContextRequest` (opcional)
```json
{
  "type": "mi-tipo",
  "version": "1.0",
  "variables": [
    { "name": "email", "value": "user@example.com" },
    { "name": "nombre", "value": "Juan" }
  ],
  "references": [
    { "type": "orderId", "value": "12345" }
  ]
}
```

**Respuesta de flow (`FlowExecutionResultDto`):**
```json
{
  "flowId": "uuid",
  "flowName": "mi-flow",
  "flowType": "BATCH",
  "steps": [
    {
      "stepId": "uuid",
      "stepName": "step-1",
      "order": 1,
      "stepStatus": "FINISHED",
      "tasks": [
        { "taskName": "enviar-email", "order": 1, "status": "FINISHED", "result": "SUCCESS" }
      ]
    },
    {
      "stepId": "uuid",
      "stepName": "step-2",
      "order": 2,
      "stepStatus": "PENDING",
      "tasks": [
        { "taskName": "generar-pdf", "order": 1, "status": "PENDING", "result": null }
      ]
    }
  ]
}
```

**Respuesta de task (`TaskExecutionResultDto`):**
```json
{
  "taskName": "enviar-email",
  "status": "FINISHED",
  "result": "SUCCESS"
}
```

> **Nota:** La ejecución de flows es **step-by-step**: cada llamada a `/process` ejecuta una sola tarea. Para completar un flow entero, llame repetidamente a `/process` hasta que todos los steps estén en `FINISHED`.

---

## Servicios del motor de ejecución (fluxy-core)

El starter registra automáticamente los servicios del motor de ejecución de `fluxy-core` como beans de Spring, inyectándoles el `FluxyEventsBus` configurado según `fluxy.eventbus.type`:

```java
// Disponibles para inyección directa en tu aplicación:
@Autowired private TaskExecutorService taskExecutorService;
@Autowired private StepExecutionService stepExecutionService;
@Autowired private FlowExecutor flowExecutor;
@Autowired private FluxyEventsBus eventBus;  // la implementación activa
```

| Bean | Descripción |
|------|-------------|
| `TaskExecutorService` | Ejecuta una `FluxyTask` y publica `FluxyEvent` en el bus. |
| `StepExecutionService` | Orquesta la ejecución secuencial de tareas dentro de un step. |
| `FlowExecutor` | Orquesta la ejecución de steps dentro de un flow, evaluando conexiones y condiciones. |

---

## FluxyTaskRegistry

Bean disponible para recuperar instancias vivas de `FluxyTask` desde el contexto de Spring:

```java
@Autowired
private FluxyTaskRegistry registry;

Optional<FluxyTask> task = registry.findByName("enviar-email");
Map<String, FluxyTask> todas = registry.getAll();
```

---

## Guía paso a paso: crear un Flow completo por API REST

Este ejemplo muestra el flujo completo para armar y ejecutar un **flow de procesamiento de pedido** con dos steps y tres tasks, usando exclusivamente los endpoints REST.

> **Prerequisito:** La aplicación Spring Boot con el starter Fluxy corriendo en `http://localhost:8080`.

### Paso 1 — Crear las tasks

Cada task representa una unidad de trabajo. Creamos tres:

**1a. Crear task `enviar-email`:**

```bash
curl -X POST http://localhost:8080/fluxy/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "name": "enviar-email",
    "version": 1,
    "description": "Envía un email de notificación al cliente"
  }'
```

**Respuesta** (`201 Created`):
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "name": "enviar-email",
  "version": 1,
  "description": "Envía un email de notificación al cliente"
}
```

**1b. Crear task `generar-pdf`:**

```bash
curl -X POST http://localhost:8080/fluxy/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "name": "generar-pdf",
    "version": 1,
    "description": "Genera el PDF de factura del pedido"
  }'
```

**Respuesta** (`201 Created`):
```json
{
  "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "name": "generar-pdf",
  "version": 1,
  "description": "Genera el PDF de factura del pedido"
}
```

**1c. Crear task `validar-inventario`:**

```bash
curl -X POST http://localhost:8080/fluxy/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "name": "validar-inventario",
    "version": 1,
    "description": "Verifica stock disponible para el pedido"
  }'
```

**Respuesta** (`201 Created`):
```json
{
  "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
  "name": "validar-inventario",
  "version": 1,
  "description": "Verifica stock disponible para el pedido"
}
```

### Paso 2 — Crear los steps

Los steps agrupan tasks que se ejecutan secuencialmente.

**2a. Crear step `step-notificacion`:**

```bash
curl -X POST http://localhost:8080/fluxy/steps \
  -H "Content-Type: application/json" \
  -d '{
    "name": "step-notificacion"
  }'
```

**Respuesta** (`201 Created`):
```json
{
  "id": "d4e5f6a7-b8c9-0123-defa-234567890123",
  "name": "step-notificacion",
  "tasks": []
}
```

**2b. Crear step `step-documentos`:**

```bash
curl -X POST http://localhost:8080/fluxy/steps \
  -H "Content-Type: application/json" \
  -d '{
    "name": "step-documentos"
  }'
```

**Respuesta** (`201 Created`):
```json
{
  "id": "e5f6a7b8-c9d0-1234-efab-345678901234",
  "name": "step-documentos",
  "tasks": []
}
```

### Paso 3 — Asignar tasks a los steps

**3a. Asignar `enviar-email` → `step-notificacion` (orden 1):**

```bash
curl -X POST http://localhost:8080/fluxy/steps/d4e5f6a7-b8c9-0123-defa-234567890123/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "order": 1
  }'
```

**Respuesta** (`201 Created`):
```json
{
  "id": "11111111-1111-1111-1111-111111111111",
  "taskName": "enviar-email",
  "taskOrder": 1,
  "status": "PENDING"
}
```

**3b. Asignar `generar-pdf` → `step-documentos` (orden 1):**

```bash
curl -X POST http://localhost:8080/fluxy/steps/e5f6a7b8-c9d0-1234-efab-345678901234/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "order": 1
  }'
```

**Respuesta** (`201 Created`):
```json
{
  "id": "22222222-2222-2222-2222-222222222222",
  "taskName": "generar-pdf",
  "taskOrder": 1,
  "status": "PENDING"
}
```

**3c. Asignar `validar-inventario` → `step-documentos` (orden 2):**

```bash
curl -X POST http://localhost:8080/fluxy/steps/e5f6a7b8-c9d0-1234-efab-345678901234/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "order": 2
  }'
```

**Respuesta** (`201 Created`):
```json
{
  "id": "33333333-3333-3333-3333-333333333333",
  "taskName": "validar-inventario",
  "taskOrder": 2,
  "status": "PENDING"
}
```

### Paso 4 — Crear el flow

**4. Crear flow `flow-pedido`:**

```bash
curl -X POST http://localhost:8080/fluxy/flows \
  -H "Content-Type: application/json" \
  -d '{
    "name": "flow-pedido",
    "type": "BATCH",
    "description": "Flow de procesamiento de pedido: notificación al cliente y generación de documentos"
  }'
```

**Respuesta** (`201 Created`):
```json
{
  "id": "f6a7b8c9-d0e1-2345-fabc-456789012345",
  "name": "flow-pedido",
  "type": "BATCH",
  "description": "Flow de procesamiento de pedido: notificación al cliente y generación de documentos",
  "steps": []
}
```

### Paso 5 — Asignar steps al flow

**5a. Asignar `step-notificacion` al flow (orden 1):**

```bash
curl -X POST http://localhost:8080/fluxy/flows/f6a7b8c9-d0e1-2345-fabc-456789012345/steps \
  -H "Content-Type: application/json" \
  -d '{
    "stepId": "d4e5f6a7-b8c9-0123-defa-234567890123",
    "order": 1
  }'
```

**Respuesta** (`201 Created`):
```json
{
  "id": "44444444-4444-4444-4444-444444444444",
  "stepId": "d4e5f6a7-b8c9-0123-defa-234567890123",
  "stepName": "step-notificacion",
  "order": 1,
  "stepStatus": "PENDING"
}
```

**5b. Asignar `step-documentos` al flow (orden 2):**

```bash
curl -X POST http://localhost:8080/fluxy/flows/f6a7b8c9-d0e1-2345-fabc-456789012345/steps \
  -H "Content-Type: application/json" \
  -d '{
    "stepId": "e5f6a7b8-c9d0-1234-efab-345678901234",
    "order": 2
  }'
```

**Respuesta** (`201 Created`):
```json
{
  "id": "55555555-5555-5555-5555-555555555555",
  "stepId": "e5f6a7b8-c9d0-1234-efab-345678901234",
  "stepName": "step-documentos",
  "order": 2,
  "stepStatus": "PENDING"
}
```

### Paso 6 — Verificar el flow completo

```bash
curl http://localhost:8080/fluxy/flows/name/flow-pedido
```

**Respuesta** (`200 OK`):
```json
{
  "id": "f6a7b8c9-d0e1-2345-fabc-456789012345",
  "name": "flow-pedido",
  "type": "BATCH",
  "description": "Flow de procesamiento de pedido: notificación al cliente y generación de documentos",
  "steps": [
    {
      "id": "44444444-4444-4444-4444-444444444444",
      "stepId": "d4e5f6a7-b8c9-0123-defa-234567890123",
      "stepName": "step-notificacion",
      "order": 1,
      "stepStatus": "PENDING"
    },
    {
      "id": "55555555-5555-5555-5555-555555555555",
      "stepId": "e5f6a7b8-c9d0-1234-efab-345678901234",
      "stepName": "step-documentos",
      "order": 2,
      "stepStatus": "PENDING"
    }
  ]
}
```

### Paso 7 — Inicializar el flow

Antes de ejecutar, se inicializa el flow (resetea todos los estados a `PENDING`):

```bash
curl -X POST http://localhost:8080/fluxy/execution/flows/f6a7b8c9-d0e1-2345-fabc-456789012345/initialize \
  -H "Content-Type: application/json" \
  -d '{
    "type": "pedido",
    "version": "1.0",
    "variables": [
      { "name": "email", "value": "cliente@example.com" },
      { "name": "nombre", "value": "Juan Pérez" },
      { "name": "orderId", "value": "ORD-2026-001" }
    ],
    "references": [
      { "type": "orderId", "value": "ORD-2026-001" },
      { "type": "clienteId", "value": "CLI-100" }
    ]
  }'
```

**Respuesta** (`200 OK`):
```json
{
  "flowId": "f6a7b8c9-d0e1-2345-fabc-456789012345",
  "flowName": "flow-pedido",
  "flowType": "BATCH",
  "steps": [
    {
      "stepId": "d4e5f6a7-b8c9-0123-defa-234567890123",
      "stepName": "step-notificacion",
      "order": 1,
      "stepStatus": "PENDING",
      "tasks": [
        { "taskName": "enviar-email", "order": 1, "status": "PENDING", "result": null }
      ]
    },
    {
      "stepId": "e5f6a7b8-c9d0-1234-efab-345678901234",
      "stepName": "step-documentos",
      "order": 2,
      "stepStatus": "PENDING",
      "tasks": [
        { "taskName": "generar-pdf", "order": 1, "status": "PENDING", "result": null },
        { "taskName": "validar-inventario", "order": 2, "status": "PENDING", "result": null }
      ]
    }
  ]
}
```

### Paso 8 — Procesar el flow (step-by-step)

Cada llamada a `/process` ejecuta **una sola tarea**. Hay que llamar repetidamente hasta completar todas.

**8a. Primera llamada** — ejecuta `enviar-email` (task 1 del step 1):

```bash
curl -X POST http://localhost:8080/fluxy/execution/flows/f6a7b8c9-d0e1-2345-fabc-456789012345/process \
  -H "Content-Type: application/json" \
  -d '{
    "type": "pedido",
    "version": "1.0",
    "variables": [
      { "name": "email", "value": "cliente@example.com" },
      { "name": "nombre", "value": "Juan Pérez" }
    ],
    "references": [
      { "type": "orderId", "value": "ORD-2026-001" }
    ]
  }'
```

**Respuesta** — step-notificacion completado, step-documentos pendiente:
```json
{
  "flowId": "f6a7b8c9-d0e1-2345-fabc-456789012345",
  "flowName": "flow-pedido",
  "flowType": "BATCH",
  "steps": [
    {
      "stepId": "d4e5f6a7-b8c9-0123-defa-234567890123",
      "stepName": "step-notificacion",
      "order": 1,
      "stepStatus": "FINISHED",
      "tasks": [
        { "taskName": "enviar-email", "order": 1, "status": "FINISHED", "result": "SUCCESS" }
      ]
    },
    {
      "stepId": "e5f6a7b8-c9d0-1234-efab-345678901234",
      "stepName": "step-documentos",
      "order": 2,
      "stepStatus": "PENDING",
      "tasks": [
        { "taskName": "generar-pdf", "order": 1, "status": "PENDING", "result": null },
        { "taskName": "validar-inventario", "order": 2, "status": "PENDING", "result": null }
      ]
    }
  ]
}
```

**8b. Segunda llamada** — ejecuta `generar-pdf` (task 1 del step 2):

```bash
curl -X POST http://localhost:8080/fluxy/execution/flows/f6a7b8c9-d0e1-2345-fabc-456789012345/process \
  -H "Content-Type: application/json" \
  -d '{
    "type": "pedido",
    "version": "1.0",
    "variables": [
      { "name": "facturaId", "value": "FAC-2026-001" }
    ],
    "references": [
      { "type": "orderId", "value": "ORD-2026-001" }
    ]
  }'
```

**Respuesta** — `generar-pdf` completada, `validar-inventario` pendiente:
```json
{
  "flowId": "f6a7b8c9-d0e1-2345-fabc-456789012345",
  "flowName": "flow-pedido",
  "flowType": "BATCH",
  "steps": [
    {
      "stepId": "d4e5f6a7-b8c9-0123-defa-234567890123",
      "stepName": "step-notificacion",
      "order": 1,
      "stepStatus": "FINISHED",
      "tasks": [
        { "taskName": "enviar-email", "order": 1, "status": "FINISHED", "result": "SUCCESS" }
      ]
    },
    {
      "stepId": "e5f6a7b8-c9d0-1234-efab-345678901234",
      "stepName": "step-documentos",
      "order": 2,
      "stepStatus": "RUNNING",
      "tasks": [
        { "taskName": "generar-pdf", "order": 1, "status": "FINISHED", "result": "SUCCESS" },
        { "taskName": "validar-inventario", "order": 2, "status": "PENDING", "result": null }
      ]
    }
  ]
}
```

**8c. Tercera llamada** — ejecuta `validar-inventario` (task 2 del step 2):

```bash
curl -X POST http://localhost:8080/fluxy/execution/flows/f6a7b8c9-d0e1-2345-fabc-456789012345/process \
  -H "Content-Type: application/json" \
  -d '{
    "type": "pedido",
    "version": "1.0",
    "variables": [
      { "name": "productoId", "value": "PROD-500" },
      { "name": "cantidad", "value": "3" }
    ],
    "references": [
      { "type": "orderId", "value": "ORD-2026-001" }
    ]
  }'
```

**Respuesta** — flow completado, todos los steps en `FINISHED`:
```json
{
  "flowId": "f6a7b8c9-d0e1-2345-fabc-456789012345",
  "flowName": "flow-pedido",
  "flowType": "BATCH",
  "steps": [
    {
      "stepId": "d4e5f6a7-b8c9-0123-defa-234567890123",
      "stepName": "step-notificacion",
      "order": 1,
      "stepStatus": "FINISHED",
      "tasks": [
        { "taskName": "enviar-email", "order": 1, "status": "FINISHED", "result": "SUCCESS" }
      ]
    },
    {
      "stepId": "e5f6a7b8-c9d0-1234-efab-345678901234",
      "stepName": "step-documentos",
      "order": 2,
      "stepStatus": "FINISHED",
      "tasks": [
        { "taskName": "generar-pdf", "order": 1, "status": "FINISHED", "result": "SUCCESS" },
        { "taskName": "validar-inventario", "order": 2, "status": "FINISHED", "result": "SUCCESS" }
      ]
    }
  ]
}
```

### Ejecución de task a demanda (sin flow)

También puedes ejecutar cualquier task directamente por nombre:

```bash
curl -X POST http://localhost:8080/fluxy/execution/tasks/enviar-email/execute \
  -H "Content-Type: application/json" \
  -d '{
    "type": "default",
    "version": "1.0",
    "variables": [
      { "name": "email", "value": "destinatario@example.com" },
      { "name": "asunto", "value": "Confirmación de pedido" }
    ],
    "references": [
      { "type": "orderId", "value": "ORD-2026-999" }
    ]
  }'
```

**Respuesta** (`200 OK`):
```json
{
  "taskName": "enviar-email",
  "status": "FINISHED",
  "result": "SUCCESS"
}
```

### Resumen visual del escenario

```
flow-pedido (BATCH)
├── step-notificacion (orden 1)
│   └── enviar-email (orden 1)
└── step-documentos (orden 2)
    ├── generar-pdf (orden 1)
    └── validar-inventario (orden 2)

Ejecución: initialize → process × 3 → FINISHED
```

---

## Colección Postman

El proyecto incluye una colección Postman lista para importar con todos los endpoints y un escenario de ejemplo completo.

### Importar la colección

1. Abre Postman y haz clic en **Import**.
2. Selecciona el archivo `postman/fluxy-collection.json` de la raíz del proyecto.
3. Crea un **Environment** en Postman con la variable:

| Variable | Valor |
|----------|-------|
| `baseUrl` | `http://localhost:8080` |

### Contenido de la colección

| Carpeta | Descripción | Requests |
|---------|-------------|----------|
| **1 — Tasks** | Crear, listar y buscar tasks | Crear `enviar-email`, `generar-pdf`, `validar-inventario`, listar todas, buscar por ID y nombre |
| **2 — Steps** | Crear steps y asignar tasks | Crear `step-notificacion` y `step-documentos`, asignar tasks, listar y buscar |
| **3 — Flows** | Crear flows y asignar steps | Crear `flow-pedido`, asignar steps, listar, buscar por nombre y tipo |
| **4 — Execution** | Ejecutar el flow completo | Inicializar, procesar (3 llamadas), procesar step a demanda, ejecutar task a demanda |
| **5 — Cleanup** | Limpiar datos de prueba | Eliminar steps del flow, eliminar flow, steps y tasks |

### Scripts de test automáticos

Cada request de creación incluye scripts de **Tests** que:
- ✅ Validan el código de respuesta HTTP (`201 Created`, `200 OK`, `204 No Content`)
- ✅ Verifican los campos de la respuesta (nombre, tipo, orden, etc.)
- ✅ Guardan automáticamente los IDs generados como **variables de entorno** Postman

**Variables auto-guardadas:**

| Variable | Descripción |
|----------|-------------|
| `taskEmailId` | ID de la task `enviar-email` |
| `taskPdfId` | ID de la task `generar-pdf` |
| `taskInventarioId` | ID de la task `validar-inventario` |
| `stepNotifId` | ID del step `step-notificacion` |
| `stepDocsId` | ID del step `step-documentos` |
| `flowPedidoId` | ID del flow `flow-pedido` |

> **Importante:** Ejecuta las requests **en orden** dentro de cada carpeta para que los IDs se encadenen correctamente. Postman usa las variables `{{taskEmailId}}`, `{{stepNotifId}}`, etc. en las URLs y bodies de las requests posteriores.

---

## Estructura del proyecto

```
spring-boot-starter-fluxy/
├── build.gradle
├── settings.gradle
├── postman/
│   └── fluxy-collection.json
└── src/main/
    ├── java/org/fluxy/starter/
    │   ├── FluxyAutoConfiguration.java
    │   ├── autoconfigure/
    │   │   ├── FluxyDataAutoConfiguration.java
    │   │   ├── FluxyDedicatedDataSourceAutoConfiguration.java
    │   │   ├── FluxyWebAutoConfiguration.java
    │   │   ├── FluxySpringEventBusAutoConfiguration.java
    │   │   ├── FluxyAwsSqsEventBusAutoConfiguration.java
    │   │   └── FluxyRabbitMqEventBusAutoConfiguration.java
    │   ├── eventbus/
    │   │   ├── FluxyEventBus.java
    │   │   ├── FluxyEventHandler.java
    │   │   ├── FluxyEventBusObjectMapperFactory.java
    │   │   ├── spring/
    │   │   │   ├── SpringFluxyEventBus.java
    │   │   │   ├── FluxyApplicationEvent.java
    │   │   │   └── FluxySpringEventListener.java
    │   │   ├── sqs/
    │   │   │   ├── SqsFluxyEventBus.java
    │   │   │   └── FluxyAwsSqsListener.java
    │   │   └── rabbit/
    │   │       ├── RabbitFluxyEventBus.java
    │   │       └── FluxyRabbitListener.java
    │   ├── properties/
    │   │   ├── FluxyDataSourceProperties.java
    │   │   ├── FluxyJpaProperties.java
    │   │   ├── FluxyTaskRegistrationProperties.java
    │   │   └── FluxyEventBusProperties.java
    │   ├── registration/
    │   │   ├── FluxyTaskRegistry.java
    │   │   └── TaskAutoRegistrationProcessor.java
    │   ├── service/
    │   │   ├── FluxyTaskService.java
    │   │   ├── FluxyStepService.java
    │   │   ├── FluxyFlowService.java
    │   │   └── FluxyExecutionService.java
    │   ├── web/
    │   │   ├── FluxyTaskController.java
    │   │   ├── FluxyStepController.java
    │   │   ├── FluxyFlowController.java
    │   │   └── FluxyExecutionController.java
    │   ├── dto/
    │   │   ├── ExecutionContextRequest.java
    │   │   ├── FlowExecutionResultDto.java
    │   │   ├── FlowStepExecutionDto.java
    │   │   ├── StepExecutionResultDto.java
    │   │   ├── TaskExecutionDto.java
    │   │   ├── TaskExecutionResultDto.java
    │   │   ├── VariableDto.java
    │   │   ├── ReferenceDto.java
    │   │   └── ... (DTOs de gestión existentes)
    │   └── exception/
    │       ├── StaleTaskException.java
    │       └── StepTaskVersionMismatchException.java
    └── resources/META-INF/
        ├── additional-spring-configuration-metadata.json
        └── spring/
            └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```
