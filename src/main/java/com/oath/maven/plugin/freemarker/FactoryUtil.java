// Copyright 2018, Oath Inc.
// Licensed under the terms of the Apache 2.0 license. See the LICENSE file in the project root for terms.

package com.oath.maven.plugin.freemarker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import freemarker.template.Configuration;
import freemarker.template.Version;

/**
 * Simple utility class to call various constructors.
 * Needed because some jmockit features don't work well with constructors.
 */
public class FactoryUtil {

  public static Configuration createConfiguration(String freeMarkerVersion) {
    return new Configuration(new Version(freeMarkerVersion));
  }

  public static File createFile(File parent, String child) {
    return new File(parent, child);
  }

  public static FileInputStream createFileInputStream(File file) throws FileNotFoundException {
    return new FileInputStream(file);
  }
  
  public static File createFile(String name) {
    return new File(name);
  }
}
