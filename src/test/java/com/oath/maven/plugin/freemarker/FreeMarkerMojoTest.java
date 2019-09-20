// Copyright 2018, Oath Inc.
// Licensed under the terms of the Apache 2.0 license. See the LICENSE file in the project root for terms.

package com.oath.maven.plugin.freemarker;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;

public class FreeMarkerMojoTest extends Assert {

  public static final File testOutputDir = new File("target/test-output/freemarker-mojo");
  
  @BeforeClass
  public static void beforeClass() throws IOException {
    // Clean output dir before each run.
    if (testOutputDir.exists()) {
      // Recursively delete output from previous run.
      Files.walk(testOutputDir.toPath())
       .sorted(Comparator.reverseOrder())
       .map(Path::toFile)
       .forEach(File::delete);
    }
  }

  @Test
  public void executeTest(
      @Mocked MavenSession session,
      @Mocked MavenProject project,
      @Mocked MojoExecution mojoExecution,
      @Mocked GeneratingFileVisitor generatingFileVisitor,
      @Mocked Files files
      ) throws MojoExecutionException, MojoFailureException, IOException {

    new Expectations(mojoExecution, generatingFileVisitor) {{
      mojoExecution.getLifecyclePhase(); result = "generate-sources";
      session.getCurrentProject(); result = project;
    }};

    FreeMarkerMojo mojo = new FreeMarkerMojo();
    
    // Validate freeMarkerVersion is required.
    assertThatExceptionOfType(MojoExecutionException.class).isThrownBy(() -> {
      mojo.execute();
    }).withMessage("freeMarkerVersion is required");
    
    mojo.setFreeMarkerVersion("");
    assertThatExceptionOfType(MojoExecutionException.class).isThrownBy(() -> {
      mojo.execute();
    }).withMessage("freeMarkerVersion is required");

    File testCaseOutputDir = new File(testOutputDir, "executeTest");
    mojo.setFreeMarkerVersion("2.3.23");
    mojo.setSourceDirectory(testCaseOutputDir);
    mojo.setTemplateDirectory(new File(testCaseOutputDir, "template"));
    mojo.setGeneratorDirectory(new File(testCaseOutputDir, "data"));
    mojo.setOutputDirectory(new File(testCaseOutputDir, "generated-files"));
    mojo.setMojo(mojoExecution);
    mojo.setSession(session);

    // Validate source directory.
    assertThatExceptionOfType(MojoExecutionException.class).isThrownBy(() -> {
      mojo.execute();
    }).withMessage("Required directory does not exist: target/test-output/freemarker-mojo/executeTest/data");
    
    new File(testCaseOutputDir, "data").mkdirs();
    assertThatExceptionOfType(MojoExecutionException.class).isThrownBy(() -> {
      mojo.execute();
    }).withMessage("Required directory does not exist: target/test-output/freemarker-mojo/executeTest/template");
    new File(testCaseOutputDir, "template").mkdirs();
    
    // Validate minimum configuration.
    mojo.execute();
    
    new Verifications() {{
      project.addCompileSourceRoot("target/test-output/freemarker-mojo/executeTest/generated-files"); times = 1;

      Configuration config;
      MavenSession capturedSession;
      @SuppressWarnings("unused")
      Map<String, OutputGeneratorPropertiesProvider> builders;

      GeneratingFileVisitor.create(
          config = withCapture(), 
          capturedSession = withCapture(), 
          builders = withCapture()); times = 1;

      assertEquals("UTF-8", config.getDefaultEncoding());
      assertEquals(session, capturedSession);
      TemplateLoader loader = config.getTemplateLoader();
      assertTrue(loader instanceof FileTemplateLoader);

      Path path;
      FileVisitor<Path> fileVisitor;
      
      Files.walkFileTree(path = withCapture(), fileVisitor = withCapture()); times = 1;
      
      assertEquals(new File(testCaseOutputDir, "data").toPath(), path);
      assertTrue(fileVisitor instanceof GeneratingFileVisitor);
    }};
  }
  
  @Test
  public void execute_generateTestSourceTest(
      @Mocked MavenSession session,
      @Mocked MavenProject project,
      @Mocked MojoExecution mojoExecution,
      @Mocked GeneratingFileVisitor generatingFileVisitor,
      @Mocked Files files
      ) throws MojoExecutionException, MojoFailureException, IOException {

    new Expectations(mojoExecution, generatingFileVisitor) {{
      mojoExecution.getLifecyclePhase(); result = "generate-test-sources";
      session.getCurrentProject(); result = project;
    }};

    FreeMarkerMojo mojo = new FreeMarkerMojo();
    
    File testCaseOutputDir = new File(testOutputDir, "generateTestSourceTest");
    mojo.setFreeMarkerVersion("2.3.23");
    mojo.setSourceDirectory(testCaseOutputDir);
    mojo.setTemplateDirectory(new File(testCaseOutputDir, "template"));
    mojo.setGeneratorDirectory(new File(testCaseOutputDir, "data"));
    mojo.setOutputDirectory(new File(testCaseOutputDir, "generated-files"));
    mojo.setMojo(mojoExecution);
    mojo.setSession(session);

    new File(testCaseOutputDir, "data").mkdirs();
    new File(testCaseOutputDir, "template").mkdirs();
    
    mojo.execute();
    
    new Verifications() {{
      project.addTestCompileSourceRoot("target/test-output/freemarker-mojo/generateTestSourceTest/generated-files"); times = 1;
    }};
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Test
  public void execute_walkFileTreeExceptionTest(
      @Mocked MavenSession session,
      @Mocked MavenProject project,
      @Mocked MojoExecution mojoExecution,
      @Mocked GeneratingFileVisitor generatingFileVisitor,
      @Mocked Files files
      ) throws MojoExecutionException, MojoFailureException, IOException {

    new Expectations(mojoExecution, generatingFileVisitor) {{
      mojoExecution.getLifecyclePhase(); result = "generate-test-sources";
      session.getCurrentProject(); result = project;
      Files.walkFileTree((Path) any,(FileVisitor) any); result = new RuntimeException("test exception");
    }};

    FreeMarkerMojo mojo = new FreeMarkerMojo();
    
    File testCaseOutputDir = new File(testOutputDir, "generateTestSourceTest");
    mojo.setFreeMarkerVersion("2.3.23");
    mojo.setSourceDirectory(testCaseOutputDir);
    mojo.setTemplateDirectory(new File(testCaseOutputDir, "template"));
    mojo.setGeneratorDirectory(new File(testCaseOutputDir, "data"));
    mojo.setOutputDirectory(new File(testCaseOutputDir, "generated-files"));
    mojo.setMojo(mojoExecution);
    mojo.setSession(session);

    new File(testCaseOutputDir, "data").mkdirs();
    new File(testCaseOutputDir, "template").mkdirs();
    
    assertThatExceptionOfType(MojoExecutionException.class).isThrownBy(() -> {
      mojo.execute();
    }).withMessage("Failed to process files in generator dir: target/test-output/freemarker-mojo/generateTestSourceTest/data");
  }
  
  @Test
  public void execute_setTemplateLoaderExceptionTest(
      @Mocked MavenSession session,
      @Mocked MavenProject project,
      @Mocked MojoExecution mojoExecution,
      @Mocked FactoryUtil factoryUtil,
      @Mocked Configuration config) {
    
    new Expectations(config, FactoryUtil.class) {{
      FactoryUtil.createConfiguration("2.3.23"); result = config;
      config.setTemplateLoader((TemplateLoader) any); result = new RuntimeException("test exception");
    }};

    FreeMarkerMojo mojo = new FreeMarkerMojo();

    File testCaseOutputDir = new File(testOutputDir, "setTemplateLoaderException");

    mojo.setFreeMarkerVersion("2.3.23");
    mojo.setSourceDirectory(testCaseOutputDir);
    mojo.setTemplateDirectory(new File(testCaseOutputDir, "template"));
    mojo.setGeneratorDirectory(new File(testCaseOutputDir, "data"));
    mojo.setOutputDirectory(new File(testCaseOutputDir, "generated-files"));
    mojo.setMojo(mojoExecution);
    mojo.setSession(session);

    new File(testCaseOutputDir, "data").mkdirs();
    new File(testCaseOutputDir, "template").mkdirs();

    assertThatExceptionOfType(MojoExecutionException.class).isThrownBy(() -> {
      mojo.execute();
    }).withMessage("Could not establish file template loader for directory: target/test-output/freemarker-mojo/setTemplateLoaderException/template");
  }
  
  @Test
  public void execute_loadFreemarkerPropertiesTest(
      @Mocked MavenSession session,
      @Mocked MavenProject project,
      @Mocked MojoExecution mojoExecution,
      @Mocked Configuration config) throws Exception {
    
    FreeMarkerMojo mojo = new FreeMarkerMojo();

    File sourceDirectory = new File("src/test/data/freemarker-mojo");
    File testCaseOutputDir = new File(testOutputDir, "loadFreemarkerProperties");

    mojo.setFreeMarkerVersion("2.3.23");
    mojo.setSourceDirectory(sourceDirectory);
    mojo.setTemplateDirectory(new File( sourceDirectory, "template"));
    mojo.setGeneratorDirectory(new File( sourceDirectory, "data"));
    mojo.setOutputDirectory(new File(testCaseOutputDir, "generated-files"));
    mojo.setMojo(mojoExecution);
    mojo.setSession(session);

    mojo.execute();
    
    new Verifications() {{
      Properties properties;
      
      config.setSettings(properties = withCapture()); times = 1;
      
      assertEquals("T,F", properties.getProperty("boolean_format"));
    }};
  }
  
  @Test
  public void execute_loadFreemarkerPropertiesExceptionTest(
      @Mocked MavenSession session,
      @Mocked MavenProject project,
      @Mocked MojoExecution mojoExecution,
      @Mocked FactoryUtil factoryUtil,
      @Mocked Configuration config) throws Exception {
    
    new Expectations(FactoryUtil.class) {{
      FactoryUtil.createFileInputStream((File) any); result = new RuntimeException("test exception");
    }};
    
    FreeMarkerMojo mojo = new FreeMarkerMojo();

    File sourceDirectory = new File("src/test/data/freemarker-mojo");
    File testCaseOutputDir = new File(testOutputDir, "loadFreemarkerPropertiesExceptionTest");

    mojo.setFreeMarkerVersion("2.3.23");
    mojo.setSourceDirectory(sourceDirectory);
    mojo.setTemplateDirectory(new File( sourceDirectory, "template"));
    mojo.setGeneratorDirectory(new File( sourceDirectory, "data"));
    mojo.setOutputDirectory(new File(testCaseOutputDir, "generated-files"));
    mojo.setMojo(mojoExecution);
    mojo.setSession(session);

    System.out.println("==== before mojo execute");
    try {
    assertThatExceptionOfType(MojoExecutionException.class).isThrownBy(() -> {
      mojo.execute();
    }).withMessage("Failed to load src/test/data/freemarker-mojo/freemarker.properties");
    } catch ( Throwable t) {
      t.printStackTrace();
    }
  }

  @Test
  public void execute_setSettingsExceptionTest(
      @Mocked MavenSession session,
      @Mocked MavenProject project,
      @Mocked MojoExecution mojoExecution,
      @Mocked Configuration config) throws Exception {
    
    new Expectations() {{
      config.setSettings((Properties) any); result = new RuntimeException("test exception");
    }};
    
    FreeMarkerMojo mojo = new FreeMarkerMojo();

    File sourceDirectory = new File("src/test/data/freemarker-mojo");
    File testCaseOutputDir = new File(testOutputDir, "loadFreemarkerProperties");

    mojo.setFreeMarkerVersion("2.3.23");
    mojo.setSourceDirectory(sourceDirectory);
    mojo.setTemplateDirectory(new File( sourceDirectory, "template"));
    mojo.setGeneratorDirectory(new File( sourceDirectory, "data"));
    mojo.setOutputDirectory(new File(testCaseOutputDir, "generated-files"));
    mojo.setMojo(mojoExecution);
    mojo.setSession(session);

    assertThatExceptionOfType(MojoExecutionException.class).isThrownBy(() -> {
      mojo.execute();
    }).withMessage("Invalid setting(s) in src/test/data/freemarker-mojo/freemarker.properties");
  }
  
}
