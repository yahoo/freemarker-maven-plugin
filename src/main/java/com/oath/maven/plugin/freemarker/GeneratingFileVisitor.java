// Copyright 2018, Oath Inc.
// Licensed under the terms of the Apache 2.0 license. See the LICENSE file in the project root for terms.

package com.oath.maven.plugin.freemarker;

import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

import org.apache.maven.execution.MavenSession;

import freemarker.template.Configuration;
import org.apache.maven.project.MavenProject;

/**
 * FileVisitor designed to process json data files. The json file parsed into
 * a map and given to FreeMarker to 
 */
public class GeneratingFileVisitor extends SimpleFileVisitor<Path> {

  private final Configuration config;
  private final MavenSession session;
  private final long pomLastModifiedTimestamp;
  private final Map<String, OutputGeneratorPropertiesProvider > extensionToBuilder;

  private GeneratingFileVisitor(Configuration config, MavenSession session, Map<String, OutputGeneratorPropertiesProvider> extensionToBuilder) {
    this.config = config;
    this.session = session;
    this.extensionToBuilder = extensionToBuilder;
    this.pomLastModifiedTimestamp = session.getAllProjects().stream()
        .map(project->project.getFile().lastModified())
        .reduce(Long::max)
        .orElse(0L);
  }

  /**
   * Factory method that calls constructor, added to facilitate testing with jmockit.
   */
  public static GeneratingFileVisitor create(Configuration config, MavenSession session, Map<String, OutputGeneratorPropertiesProvider> extensionToBuilder) {
    return new GeneratingFileVisitor(config, session, extensionToBuilder);
  }

  @Override
  public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
    if (attrs.isRegularFile()) {
      OutputGenerator.OutputGeneratorBuilder builder = OutputGenerator.builder()
          .addGeneratorLocation(path)
          .addPomLastModifiedTimestamp(pomLastModifiedTimestamp);
      String fileName = path.getFileName().toString();
      String extenstion = fileName.substring(fileName.lastIndexOf('.'));
      OutputGeneratorPropertiesProvider pathProcessor = extensionToBuilder.get(extenstion);
      if (pathProcessor == null) {
        throw new RuntimeException("Unknown file extension: " + path);
      }
      pathProcessor.providePropertiesFromFile(path, builder);
      builder.addToDataModel("pomProperties", session.getCurrentProject().getProperties());
      builder.create().generate(config);
    }
    return FileVisitResult.CONTINUE;
  }
}
