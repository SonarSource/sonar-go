package org.sonarsource.apex.parser.poc.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class TestUtils {

  private static final String BASE_FOLDER = "src/test/java/org/sonarsource/apex/parser/poc/";

  public static String readFile(String path) throws IOException {
    return new String(Files.readAllBytes(new File("src/test/java/org/sonarsource/apex/parser/poc/" + path).toPath()), UTF_8);
  }

  public static void assertFileContent(String expectedContent, String path) throws IOException {
    File reportFile = new File(BASE_FOLDER + path);
    if (!reportFile.exists()) {
      throw new IOException("Expected file '" + path + "' not found, expecting content:\n" +
          "_____________________________________________________\n" +
          expectedContent +
          "_____________________________________________________\n");
    }
    assertThat(expectedContent).isEqualTo(new String(Files.readAllBytes(reportFile.toPath()), UTF_8));
  }

}
