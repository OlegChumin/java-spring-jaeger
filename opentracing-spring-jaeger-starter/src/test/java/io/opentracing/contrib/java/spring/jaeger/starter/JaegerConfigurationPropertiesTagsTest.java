/**
 * Copyright 2018-2019 The OpenTracing Authors
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
package io.opentracing.contrib.java.spring.jaeger.starter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import io.jaegertracing.Configuration;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Описывает поведение компонента JaegerConfigurationPropertiesTagsTest.
 */

@org.junit.jupiter.api.DisplayName("Тесты компонента JaegerConfigurationPropertiesTagsTest")
public class JaegerConfigurationPropertiesTagsTest {

  @Before
  @After
  public void clearProperties() {
    System.clearProperty(Configuration.JAEGER_TAGS);
  }

  @org.junit.jupiter.api.DisplayName("Проверяет тестовый сценарий")
  @Test
  public void envTagsNotIncluded() {
    final Map<String, String> tagsInProperties = new HashMap<>();
    tagsInProperties.put("t1", "v1");
    tagsInProperties.put("t2", "v2");
    final JaegerConfigurationProperties properties = new JaegerConfigurationProperties();
    properties.setTags(tagsInProperties);

    assertThat(properties.determineTags()).containsOnly(entry("t1", "v1"), entry("t2", "v2"));
  }

  @org.junit.jupiter.api.DisplayName("Проверяет тестовый сценарий")
  @Test
  public void envTagsIncluded() {
    System.setProperty(Configuration.JAEGER_TAGS, "t3, t4 = v4");

    final Map<String, String> tagsInProperties = new HashMap<>();
    tagsInProperties.put("t1", "v1");
    tagsInProperties.put("t2", "v2");
    final JaegerConfigurationProperties properties = new JaegerConfigurationProperties();
    properties.setTags(tagsInProperties);
    properties.setIncludeJaegerEnvTags(true);

    assertThat(properties.determineTags()).containsOnly(entry("t1", "v1"), entry("t2", "v2"), entry("t4", "v4"));
  }

  @org.junit.jupiter.api.DisplayName("Проверяет тестовый сценарий")
  @Test
  public void envTagsIncludedWithoutConfiguredTags() {
    System.setProperty(Configuration.JAEGER_TAGS, "env = test, malformed");

    final JaegerConfigurationProperties properties = new JaegerConfigurationProperties();
    properties.setIncludeJaegerEnvTags(true);

    assertThat(properties.determineTags()).containsOnly(entry("env", "test"));
  }

  @org.junit.jupiter.api.DisplayName("Проверяет тестовый сценарий")
  @Test
  public void envTagsOverrideConfiguredTags() {
    System.setProperty(Configuration.JAEGER_TAGS, "shared = env, only-env = value");

    final Map<String, String> tagsInProperties = new HashMap<>();
    tagsInProperties.put("shared", "configured");
    final JaegerConfigurationProperties properties = new JaegerConfigurationProperties();
    properties.setTags(tagsInProperties);
    properties.setIncludeJaegerEnvTags(true);

    assertThat(properties.determineTags()).containsOnly(
        entry("shared", "env"),
        entry("only-env", "value")
    );
  }

}
