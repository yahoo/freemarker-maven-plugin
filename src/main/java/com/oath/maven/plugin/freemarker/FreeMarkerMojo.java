// Copyright 2018, Oath Inc.
// Licensed under the terms of the Apache 2.0 license. See the LICENSE file in the project root for terms.

package com.oath.maven.plugin.freemarker;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class FreeMarkerMojo extends AbstractMojo {

  /** FreeMarker version string used to build FreeMarker Configuration instance. */
  @Parameter
  private String freeMarkerVersion;

  @Parameter(defaultValue = "src/main/freemarker")
  private File sourceDirectory;

  @Parameter(defaultValue = "src/main/freemarker/template")
  private File templateDirectory;

  @Parameter(defaultValue = "src/main/freemarker/generator")
  private File generatorDirectory;

  @Parameter(defaultValue = "target/generated-sources/freemarker")
  private File outputDirectory;

  @Parameter(defaultValue = "${session}", readonly = true)
  private MavenSession session;

  @Parameter(defaultValue = "${mojoExecution}", readonly = true)
  private MojoExecution mojo;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    if (freeMarkerVersion == null || freeMarkerVersion.length() == 0) {
      throw new MojoExecutionException("freeMarkerVersion is required");
    }

    if (!generatorDirectory.isDirectory()) {
      throw new MojoExecutionException("Required directory does not exist: " + generatorDirectory);
    }

    Configuration config = FactoryUtil.createConfiguration(freeMarkerVersion);

    config.setDefaultEncoding("UTF-8");

    if (!templateDirectory.isDirectory()) {
      throw new MojoExecutionException("Required directory does not exist: " + templateDirectory);
    }
    try {
      config.setTemplateLoader(new FileTemplateLoader(templateDirectory));
    } catch (Throwable t) {
      getLog().error("Could not establish file template loader for directory: " + templateDirectory, t);
      throw new MojoExecutionException("Could not establish file template loader for directory: " + templateDirectory);
    }

    File freeMarkerProps = FactoryUtil.createFile(sourceDirectory, "freemarker.properties");
    if (freeMarkerProps.isFile()) {
      Properties configProperties = new Properties();
      try (InputStream is = FactoryUtil.createFileInputStream(freeMarkerProps)) {
        configProperties.load(is);
      } catch (Throwable t) {
        getLog().error("Failed to load " + freeMarkerProps, t);
        throw new MojoExecutionException("Failed to load " + freeMarkerProps);
      }
      try {
        config.setSettings(configProperties);
      } catch (Throwable t) {
        getLog().error("Invalid setting(s) in " + freeMarkerProps, t);
        throw new MojoExecutionException("Invalid setting(s) in " + freeMarkerProps);
      }
    }
    
    if ("generate-sources".equals(mojo.getLifecyclePhase())) {
      session.getCurrentProject().addCompileSourceRoot(outputDirectory.toString());
    } else if ("generate-test-sources".equals(mojo.getLifecyclePhase())) {
      session.getCurrentProject().addTestCompileSourceRoot(outputDirectory.toString());
    }

    Map<String, OutputGeneratorPropertiesProvider> extensionToBuilders = new HashMap<>(1);
    extensionToBuilders.put(".json", JsonPropertiesProvider.create(generatorDirectory,templateDirectory,outputDirectory));

    GeneratingFileVisitor fileVisitor = GeneratingFileVisitor.create(config, session, extensionToBuilders);
    try {
      Files.walkFileTree(generatorDirectory.toPath(), fileVisitor);
    } catch (Throwable t) {
      getLog().error("Failed to process files in generator dir: " + generatorDirectory, t);
      throw new MojoExecutionException("Failed to process files in generator dir: " + generatorDirectory);
    }
  }
}
