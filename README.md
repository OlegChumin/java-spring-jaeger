# java-spring-jaeger

Spring Boot starter для подключения Jaeger как реализации OpenTracing `io.opentracing.Tracer`.

Проект основан на старом `opentracing-contrib/java-spring-jaeger`, но теперь развивается как отдельная ветка. Исходный upstream давно не обновлялся, поэтому этот репозиторий переведен на Gradle и подготовлен для дальнейшей поддержки внутри нашей вилки.

## Состояние проекта

В текущей версии репозитория:

* сборка переведена с Maven на Gradle;
* добавлен Gradle Wrapper;
* подключены Checkstyle и JaCoCo `0.8.14`;
* JaCoCo генерирует XML и HTML отчеты;
* тесты запускаются через JUnit Platform с поддержкой JUnit Vintage;
* интеграционные тесты с Jaeger запускаются отдельным флагом Gradle;
* JavaDoc и описания тестов переведены на русский.

## Модули

* `opentracing-spring-jaeger-starter` - базовый starter, который создает Jaeger-реализацию OpenTracing `Tracer`.
* `opentracing-spring-jaeger-web-starter` - удобный web-starter, который подключает `opentracing-spring-jaeger-starter` и `opentracing-spring-web-starter`.
* `opentracing-spring-jaeger-cloud-starter` - удобный cloud-starter, который подключает `opentracing-spring-jaeger-starter` и `opentracing-spring-cloud-starter`.
* `opentracing-spring-jaeger-web-starter-it` - интеграционные тесты для web-starter.

## Версии библиотек

Исторически версии `1.x.y` были рассчитаны на Spring Boot 2.x, а версии `0.x.y` - на Spring Boot 1.5.

Текущая Gradle-сборка использует Spring Boot dependency management `2.3.4.RELEASE` и Java 8 source/target compatibility.

## Подключение в приложение

Для web-приложения обычно достаточно подключить `opentracing-spring-jaeger-web-starter`.

Gradle:

```groovy
dependencies {
    implementation 'io.opentracing.contrib:opentracing-spring-jaeger-web-starter'
}
```

Maven:

```xml
<dependency>
  <groupId>io.opentracing.contrib</groupId>
  <artifactId>opentracing-spring-jaeger-web-starter</artifactId>
</dependency>
```

Для Spring Cloud можно использовать `opentracing-spring-jaeger-cloud-starter`.

Gradle:

```groovy
dependencies {
    implementation 'io.opentracing.contrib:opentracing-spring-jaeger-cloud-starter'
}
```

Maven:

```xml
<dependency>
  <groupId>io.opentracing.contrib</groupId>
  <artifactId>opentracing-spring-jaeger-cloud-starter</artifactId>
</dependency>
```

Любая из этих зависимостей включает автонастройку Jaeger-реализации OpenTracing `Tracer` при старте Spring Boot приложения.

## Базовая настройка

Если настройки не менять, spans отправляются через UDP на `localhost:6831`.

Для UDP sender:

```properties
opentracing.jaeger.udp-sender.host=jaegerhost
opentracing.jaeger.udp-sender.port=6831
```

Для HTTP sender:

```properties
opentracing.jaeger.http-sender.url=http://jaegerhost:14268/api/traces
```

Если HTTP sender настроен, UDP sender не используется.

## Параметры конфигурации

Все параметры конфигурации описаны в [`JaegerConfigurationProperties`](opentracing-spring-jaeger-starter/src/main/java/io/opentracing/contrib/java/spring/jaeger/starter/JaegerConfigurationProperties.java).

Префикс всех свойств:

```properties
opentracing.jaeger
```

Имя сервиса можно задать через стандартное свойство Spring:

```properties
spring.application.name=my-service
```

Также можно задать его напрямую:

```properties
opentracing.jaeger.service-name=my-service
```

Для camelCase-полей в `JaegerConfigurationProperties` используйте стандартный Spring Boot relaxed binding:

* в `properties` и `yaml` - kebab-case, например `opentracing.jaeger.log-spans=true`;
* в переменных окружения - underscore-case, например `OPENTRACING_JAEGER_LOG_SPANS`.

## Значения по умолчанию

Если пользователь не переопределяет beans, автонастройка создает следующие компоненты:

* service name: `unknown-spring-boot`, если не заданы `spring.application.name` или `opentracing.jaeger.service-name`;
* `CompositeReporter`, который содержит:
  * `LoggingReporter` для вывода spans в консоль;
  * `RemoteReporter` с `UdpSender`, отправляющим spans на `localhost:6831`;
* `ConstSampler` со значением `true`, то есть по умолчанию сэмплируются все traces;
* `NoopMetricsFactory`, то есть метрики Jaeger-клиента не собираются.

## Senders

### HTTP Sender

```properties
opentracing.jaeger.http-sender.url=http://jaegerhost:14268/api/traces
```

Аутентификация по username/password:

```properties
opentracing.jaeger.http-sender.username=username
opentracing.jaeger.http-sender.password=password
```

Аутентификация через bearer token:

```properties
opentracing.jaeger.http-sender.authtoken=token
```

### UDP Sender

```properties
opentracing.jaeger.udp-sender.host=jaegerhost
opentracing.jaeger.udp-sender.port=6831
```

## Частые сценарии

### Задать имя сервиса

```properties
spring.application.name=my-service
```

или:

```properties
opentracing.jaeger.service-name=my-service
```

### Отключить логирование spans в консоль

По умолчанию spans логируются в консоль. Отключение:

```properties
opentracing.jaeger.log-spans=false
```

### Добавить дополнительные reporters

Можно объявить bean типа `ReporterAppender`. Он получает список reporters, созданных автонастройкой, и может добавить в него дополнительные reporters.

### Sampling

Const sampler:

```properties
opentracing.jaeger.const-sampler.decision=true
```

Probabilistic sampler:

```properties
opentracing.jaeger.probabilistic-sampler.sampling-rate=0.5
```

Значение должно быть в диапазоне от `0.0` до `1.0`.

Rate-limiting sampler:

```properties
opentracing.jaeger.rate-limiting-sampler.max-traces-per-second=2.0
```

Remote sampler:

```properties
opentracing.jaeger.remote-controlled-sampler.host-port=localhost:5778
```

Эти sampler-настройки взаимоисключающие. При необходимости можно объявить собственный bean типа `io.jaegertracing.spi.Sampler`.

### Распространять заголовки в B3-формате

Для совместимости с Zipkin:

```properties
opentracing.jaeger.enable-b3-propagation=true
```

### Распространять заголовки в W3C Trace Context

```properties
opentracing.jaeger.enable-w3c-propagation=true
```

### Включить 128-bit trace id

```properties
opentracing.jaeger.enable-128-bit-traces=true
```

### Добавить tags из переменной окружения Jaeger

```properties
opentracing.jaeger.include-jaeger-env-tags=true
```

Значение читается из `JAEGER_TAGS` или соответствующего системного свойства Jaeger.

## Расширенная настройка

### Ручное объявление beans

Приложение может самостоятельно объявить следующие beans. В этом случае они будут использованы вместо автоматически созданных:

* `io.jaegertracing.spi.Sampler`;
* `io.jaegertracing.spi.MetricsFactory`;
* `io.jaegertracing.spi.Reporter`;
* `io.opentracing.Tracer`.

### Кастомизация `JaegerTracer.Builder`

Если нужно донастроить `JaegerTracer.Builder`, но сохранить остальную автонастройку, используйте `TracerBuilderCustomizer`.

Примеры таких customizer-классов:

* `B3CodecTracerBuilderCustomizer`;
* `TraceContextCodecTracerBuilderCustomizer`;
* `ExpandExceptionLogsTracerBuilderCustomizer`;
* `HigherBitTracerBuilderCustomizer`.

## Важное предупреждение

### Default sampler небезопасен для production

По умолчанию используется `ConstSampler=true`, поэтому сэмплируется каждый request. В high traffic окружении это может создать большую нагрузку. Для production лучше явно настроить probabilistic, rate-limiting или remote sampler.

## Разработка

Проект собирается Gradle. Maven wrapper и `pom.xml` удалены.

Для Windows:

```powershell
.\gradlew.bat clean build
```

Для Linux/macOS:

```shell
./gradlew clean build
```

Эта команда запускает компиляцию, unit-тесты, Checkstyle и JaCoCo report.

### Интеграционные тесты

Интеграционные тесты запускаются явно, потому что им нужен Docker и Jaeger testcontainer.

Windows:

```powershell
.\gradlew.bat clean build -PrunIntegrationTests
```

Linux/macOS:

```shell
./gradlew clean build -PrunIntegrationTests
```

### JaCoCo

JaCoCo подключен во всех subprojects с версией `0.8.14`.

После тестов отчеты появляются в директориях модулей:

```text
build/reports/jacoco/test/html/index.html
build/reports/jacoco/test/jacocoTestReport.xml
```

Для модулей без тестов отчет может быть пропущен.

### Checkstyle

Checkstyle использует конфигурацию:

```text
config/checkstyle/checkstyle.xml
```

## Полное отключение tracing

Иногда tracing нужно отключить полностью, например в тестовом окружении. Простого `opentracing.jaeger.enabled=false` может быть недостаточно, потому что другие OpenTracing auto-configurations ожидают bean типа `io.opentracing.Tracer`.

Один из вариантов - объявить noop tracer:

```java
@ConditionalOnProperty(value = "opentracing.jaeger.enabled", havingValue = "false", matchIfMissing = false)
@Configuration
public class MyTracerConfiguration {

    @Bean
    public io.opentracing.Tracer jaegerTracer() {
        return io.opentracing.noop.NoopTracerFactory.create();
    }
}
```

Такой tracer сохраняет корректную Spring-конфигурацию, но фактически не отправляет traces.

## Trace id не пробрасывается через Feign client

Если используется Feign, иногда нужно явно объявить Feign client в Spring-конфигурации, чтобы `uber-trace-id` корректно пробрасывался:

```java
@Bean
public Client feignClient() {
    return new Client.Default(null, null);
}
```

## Лицензия

[Apache 2.0 License](./LICENSE).
