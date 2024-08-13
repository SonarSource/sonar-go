/*
 * Copyright (C) 2018-2024 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package com.sonarsource.apex.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.container.Server;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.users.CreateRequest;
import org.sonarsource.sonarlint.core.ConnectedSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.analysis.api.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.commons.IssueSeverity;
import org.sonarsource.sonarlint.core.commons.Language;
import org.sonarsource.sonarlint.core.http.HttpClient;
import org.sonarsource.sonarlint.core.http.HttpConnectionListener;
import org.sonarsource.sonarlint.core.serverapi.EndpointParams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SonarLintTest {

    private static final String APEX_FILE = "file.cls";

    @ClassRule
    public static TemporaryFolder temp = new TemporaryFolder();

    @ClassRule
    public static Orchestrator orchestrator = Tests.ORCHESTRATOR;

    private static ConnectedSonarLintEngine sonarlintEngine;
    private static Path baseDir;

    private static final String PROJECT_KEY = "project1";
    private static final String SONARLINT_USER = "sonarlint-user";
    private static final String SONARLINT_PWD = "sonarlint-password";
    private static final HttpClient httpClient = new HttpClientForTests();

    @BeforeClass
    public static void prepare() throws Exception {
        // A standalone engine cannot be used, because it does not feature licence management,
        // so a connected engine is used
        newAdminWsClient().users().create(new CreateRequest()
          .setLogin(SONARLINT_USER)
          .setPassword(SONARLINT_PWD)
          .setName("SonarLint"));
        baseDir = temp.newFolder().toPath();

    }

    @Before
    public void setup() throws IOException {
        Server server = orchestrator.getServer();
        server.provisionProject(PROJECT_KEY, "project 1");

        ConnectedGlobalConfiguration sonarLintConfig = ConnectedGlobalConfiguration.sonarQubeBuilder()
          .setConnectionId("orchestrator")
          .setSonarLintUserHome(temp.newFolder().toPath())
          .setLogOutput((formattedMessage, level) -> {
              /* Don't pollute logs */
          })
          .addEnabledLanguage(Language.APEX)
          .build();
        sonarlintEngine = new ConnectedSonarLintEngineImpl(sonarLintConfig);
        update();
    }

    private static void update() {
        EndpointParams endpointParams = new EndpointParams(orchestrator.getServer().getUrl(), false, null);
        sonarlintEngine.updateProject(endpointParams, httpClient, PROJECT_KEY, null);
        sonarlintEngine.sync(endpointParams, httpClient, Collections.singleton(PROJECT_KEY), null);
    }


    @Test
    public void test_apex() throws Exception {
        Path filePath = Paths.get("projects/SonarLintTest/" + APEX_FILE);
        filePath = Files.copy(filePath, baseDir.resolve(APEX_FILE));
        ClientInputFile inputFile = prepareInputFile(APEX_FILE, filePath, false);

        List<Issue> issues = new ArrayList<>();

        ConnectedAnalysisConfiguration.Builder analysisConfigurationBuilder = ConnectedAnalysisConfiguration.builder()
          .setProjectKey(PROJECT_KEY)
          .setBaseDir(baseDir)
          .addInputFiles(Collections.singletonList(inputFile));

        sonarlintEngine.analyze(
          new ConnectedAnalysisConfiguration(analysisConfigurationBuilder),
          issues::add, null, null
        );

        assertThat(issues).extracting(Issue::getRuleKey, Issue::getStartLine, issue -> issue.getInputFile().getPath(), Issue::getSeverity)
          .containsOnly(
            tuple("apex:S1145", 3, inputFile.getPath(), IssueSeverity.MAJOR),
            tuple("apex:S1481", 4, inputFile.getPath(), IssueSeverity.MINOR)
          );
    }

    private static ClientInputFile prepareInputFile(String relativePath, Path filePath, boolean isTest) {
        return createInputFile(relativePath, filePath, isTest);
    }

    private static ClientInputFile createInputFile(String relativePath, Path path, boolean isTest) {
        return new ClientInputFile() {

            @Override
            public String getPath() {
                return path.toString();
            }

            @Override
            public boolean isTest() {
                return isTest;
            }

            @Override
            public Charset getCharset() {
                return StandardCharsets.UTF_8;
            }

            @Override
            public <G> G getClientObject() {
                return null;
            }

            @Override
            public String contents() throws IOException {
                return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            }

            @Override
            public InputStream inputStream() throws IOException {
                return Files.newInputStream(path);
            }

            @Override
            public String relativePath() {
                return relativePath;
            }

            @Override
            public URI uri() {
                return path.toUri();
            }

        };
    }

    @AfterClass
    public static void stop() {
        sonarlintEngine.stop(true);
    }

    private static WsClient newAdminWsClient() {
        return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
          .url(orchestrator.getServer().getUrl())
          .credentials("admin", "admin")
          .build());
    }

    private static class HttpClientForTests implements HttpClient {
        private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
          .build();

        @Override
        public Response get(String url) {
            Request request = new Request.Builder()
              .url(url)
              .header("Authorization", Credentials.basic(SONARLINT_USER, SONARLINT_PWD))
              .build();
            return executeRequest(request);
        }

        private Response executeRequest(Request request) {
            try {
                return wrap(okHttpClient.newCall(request).execute());
            } catch (IOException e) {
                throw new IllegalStateException("Unable to execute request: " + e.getMessage(), e);
            }
        }

        private Response wrap(okhttp3.Response wrapped) {
            return new Response() {
                @Override
                public String url() {
                    return wrapped.request().url().toString();
                }

                @Override
                public int code() {
                    return wrapped.code();
                }

                @Override
                public void close() {
                    wrapped.close();
                }

                @Override
                public String bodyAsString() {
                    try (ResponseBody body = wrapped.body()) {
                        return body.string();
                    } catch (IOException e) {
                        throw new IllegalStateException("Unable to read response body: " + e.getMessage(), e);
                    }
                }

                @Override
                public InputStream bodyAsStream() {
                    return wrapped.body().byteStream();
                }

                @Override
                public String toString() {
                    return wrapped.toString();
                }
            };
        }

        @Override
        public CompletableFuture<Response> getAsync(String url) {
            Request request = new Request.Builder()
              .url(url)
              .header("Authorization", Credentials.basic(SONARLINT_USER, SONARLINT_PWD))
              .build();
            return executeRequestAsync(request);
        }

        private CompletableFuture<Response> executeRequestAsync(Request request) {
            Call call = okHttpClient.newCall(request);
            CompletableFuture<Response> futureResponse = new CompletableFuture<Response>()
              .whenComplete((response, error) -> {
                  if (error instanceof CancellationException) {
                      call.cancel();
                  }
              });
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    futureResponse.completeExceptionally(e);
                }

                @Override
                public void onResponse(Call call, okhttp3.Response response) {
                    futureResponse.complete(wrap(response));
                }
            });
            return futureResponse;
        }

        @Override
        public AsyncRequest getEventStream(String url, HttpConnectionListener httpConnectionListener, Consumer<String> consumer) {
            // not used from tests
            return null;
        }

        @Override
        public Response post(String url, String contentType, String bodyContent) {
            // not used from tests
            return null;
        }

        @Override
        public CompletableFuture<Response> postAsync(String url, String contentType, String body) {
            return HttpClient.super.postAsync(url, contentType, body);
        }

        @Override
        public Response delete(String url, String contentType, String bodyContent) {
            // not used from tests
            return null;
        }
    }
}
