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
| `DELETE` | `/fluxy/tasks/{id}` | Elimina una tarea |

**POST body:** `{ "name": "mi-tarea" }`

### FluxyStep — `/fluxy/steps`

| Método | URL | Descripción |
|--------|-----|-------------|
| `GET` | `/fluxy/steps` | Lista todos los steps |
| `GET` | `/fluxy/steps/{id}` | Busca por ID |
| `GET` | `/fluxy/steps/name/{name}` | Busca por nombre |
| `POST` | `/fluxy/steps` | Crea un step |
| `POST` | `/fluxy/steps/{id}/tasks` | Agrega una tarea al step |
| `DELETE` | `/fluxy/steps/{stepId}/tasks/{taskId}` | Elimina tarea del step |
| `DELETE` | `/fluxy/steps/{id}` | Elimina un step |

**POST /fluxy/steps/{id}/tasks body:** `{ "taskId": "uuid", "order": 1 }`

### FluxyFlow — `/fluxy/flows`

| Método | URL | Descripción |
|--------|-----|-------------|
| `GET` | `/fluxy/flows` | Lista todos los flows |
| `GET` | `/fluxy/flows/{id}` | Busca por ID |
| `GET` | `/fluxy/flows/name/{name}` | Busca por nombre |
| `GET` | `/fluxy/flows/type/{type}` | Filtra por tipo |
| `POST` | `/fluxy/flows` | Crea un flow |
| `POST` | `/fluxy/flows/{id}/steps` | Agrega un step al flow |
| `DELETE` | `/fluxy/flows/{flowId}/steps/{stepId}` | Elimina step del flow |
| `DELETE` | `/fluxy/flows/{id}` | Elimina un flow |

**POST /fluxy/flows body:** `{ "name": "mi-flow", "type": "BATCH", "description": "..." }`

**POST /fluxy/flows/{id}/steps body:** `{ "stepId": "uuid", "order": 1 }`

### Ejecución — `/fluxy/execution`

Endpoints para la ejecución a demanda de flows, steps y tasks. Cada ejecución de tarea se delega al `TaskExecutorService` del core, que publica eventos en el bus configurado (SPRING, SQS o RABBIT).

| Método | URL | Descripción |
|--------|-----|-------------|
| `POST` | `/fluxy/execution/flows/{id}/initialize` | Inicializa un flow (resetea todos los steps/tasks a PENDING) |
| `POST` | `/fluxy/execution/flows/{id}/process` | Procesa el siguiente step del flow (ejecuta su siguiente tarea pendiente) |
| `POST` | `/fluxy/execution/steps/{id}/process` | Procesa un step a demanda (ejecuta su siguiente tarea pendiente) |
| `POST` | `/fluxy/execution/tasks/{name}/execute` | Ejecuta una tarea específica por nombre |

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

## Estructura del proyecto

```
spring-boot-starter-fluxy/
├── build.gradle
├── settings.gradle
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
