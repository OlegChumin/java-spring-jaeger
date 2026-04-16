/**
 * Copyright 2018-2026 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.java.spring.web.jaeger.starter.it;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;


/**
 * Описывает поведение компонента JaegerIntegrationTest.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = DemoSpringBootWebApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = JaegerIntegrationTest.Initializer.class)
@org.junit.jupiter.api.DisplayName("Тесты компонента JaegerIntegrationTest")
public class JaegerIntegrationTest {

  private static final int QUERY_PORT = 16686;
  private static final int COLLECTOR_PORT = 14268;
  private static final String SERVICE_NAME = "spring-boot-test";

  @ClassRule
  public static DockerJaegerResource jaeger = new DockerJaegerResource();

  @Autowired
  private TestRestTemplate testRestTemplate;

  private RestTemplate restTemplate = new RestTemplate();

  @org.junit.jupiter.api.DisplayName("Проверяет тестовый сценарий")
  @Test
  public void testJaegerCollectsTraces() {
    final String operation = "hello";
    Assertions.assertThat(testRestTemplate.getForObject("/" + operation, String.class)).isNotBlank();

    waitJaegerQueryContains(SERVICE_NAME, operation);
  }

  private void waitJaegerQueryContains(String serviceName, String str) {
    Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> {
      try {
        final String output = restTemplate.getForObject(
            String.format(
                "%s/api/traces?service=%s",
                String.format(
                    "http://localhost:%d",
                    jaeger.getQueryPort()
                ),
                serviceName
            ),
            String.class
        );
        return output.contains(str);
      } catch (Exception e) {
        return false;
      }
    });
  }

  //create the opentracing.jaeger properties from the information of the running container
  public static class Initializer implements
      ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(
          String.format(
              "opentracing.jaeger.http-sender.url=http://%s:%d/api/traces",
              "localhost",
              jaeger.getCollectorPort()
          ),
          String.format("spring.application.name=%s", SERVICE_NAME)
      ).applyTo(configurableApplicationContext);
    }
  }

  public static class DockerJaegerResource extends ExternalResource {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final Pattern CONTAINER_ID_PATTERN = Pattern.compile("[a-f0-9]{64}");
    private int collectorPort;
    private int queryPort;
    private String containerId;

    public int getCollectorPort() {
      return collectorPort;
    }

    public int getQueryPort() {
      return queryPort;
    }

    @Override
    protected void before() throws Throwable {
      collectorPort = findAvailablePort();
      queryPort = findAvailablePort();
      containerId = extractContainerId(runDockerCommand(
          Arrays.asList(
              "docker",
              "run",
              "-d",
              "-p",
              String.format("%d:%d", collectorPort, COLLECTOR_PORT),
              "-p",
              String.format("%d:%d", queryPort, QUERY_PORT),
              "jaegertracing/all-in-one:1.4"
          )
      ));
      waitUntilJaegerIsReady();
    }

    @Override
    protected void after() {
      if (containerId != null) {
        try {
          runDockerCommand(Arrays.asList("docker", "rm", "-f", containerId));
        } catch (Exception ignored) {
          // Nothing useful to do during test cleanup.
        }
      }
    }

    private void waitUntilJaegerIsReady() {
      Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> {
        try {
          return restTemplate.getForObject(
              String.format("http://localhost:%d/", queryPort),
              String.class
          ) != null;
        } catch (Exception e) {
          return false;
        }
      });
    }

    private static int findAvailablePort() throws IOException {
      try (ServerSocket socket = new ServerSocket(0)) {
        return socket.getLocalPort();
      }
    }

    private static String runDockerCommand(List<String> command) throws IOException,
        InterruptedException {
      Process process = new ProcessBuilder(command)
          .redirectErrorStream(true)
          .start();
      byte[] output = new byte[8192];
      int length = process.getInputStream().read(output);
      int exitCode = process.waitFor();
      String result = length > 0 ? new String(output, 0, length).trim() : "";
      if (exitCode != 0) {
        throw new IllegalStateException(String.format(
            "Command failed with exit code %d: %s%n%s",
            exitCode,
            command,
            result
        ));
      }
      return result;
    }

    private static String extractContainerId(String output) {
      Matcher matcher = CONTAINER_ID_PATTERN.matcher(output);
      String containerId = null;
      while (matcher.find()) {
        containerId = matcher.group();
      }
      if (containerId == null) {
        throw new IllegalStateException("Could not find Docker container id in: " + output);
      }
      return containerId;
    }
  }

}
