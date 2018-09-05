// Copyright 2018, Oath Inc.
// Licensed under the terms of the Apache 2.0 license. See the LICENSE file in the project root for terms.

package com.oath.maven.plugin.freemarker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class JsonPropertiesProvider implements OutputGeneratorPropertiesProvider {
	private final Gson gson;
	private final Type stringObjectMap;
	private final File dataDir;
	private final File templateDir;
	private final File outputDir;

	private JsonPropertiesProvider(File dataDir, File templateDir, File outputDir) {
		this.dataDir = dataDir;
		this.templateDir = templateDir;
		this.outputDir = outputDir;
		gson = new GsonBuilder().setLenient().create();
		stringObjectMap = new TypeToken<Map<String, Object>>() { } .getType();
	}

	public static JsonPropertiesProvider create(File dataDir, File templateDir, File outputDir) {
		return new JsonPropertiesProvider(dataDir, templateDir, outputDir);
	}

	@Override
	public void providePropertiesFromFile(Path path, OutputGenerator.OutputGeneratorBuilder builder) {
		File jsonDataFile = path.toFile();
		Map<String,Object> data = parseJson(jsonDataFile);

		Object obj = data.get("dataModel");
		if (obj != null) {
			builder.addDataModel((Map<String, Object>) obj);
		} else {
			builder.addDataModel(new HashMap<String,Object>());
		}

		obj = data.get("templateName");
		if (obj == null) {
			throw new RuntimeException("Require json data property not found: templateName");
		}
		builder.addTemplateLocation(templateDir.toPath().resolve(obj.toString()));

		String dataDirName = dataDir.getAbsolutePath();
		String jsonFileName = jsonDataFile.getAbsolutePath();
		if (!jsonFileName.startsWith(dataDirName)) {
			throw new IllegalStateException("visitFile() given file not in sourceDirectory: " + jsonDataFile);
		}

		String outputFileName = jsonFileName.substring(dataDirName.length()+1);
		outputFileName = outputFileName.substring(0, outputFileName.length() - 5);
		Path outputPath = outputDir.toPath();
		Path resolved = outputPath.resolve(outputFileName);
		builder.addOutputLocation(resolved);
	}

	private Map<String, Object> parseJson(File jsonDataFile) {
		try (JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(jsonDataFile), "UTF-8"))) {
			return gson.fromJson(reader, stringObjectMap);
		} catch (Throwable t) {
			throw new RuntimeException("Could not parse json data file: " + jsonDataFile, t);
		}
	}
}
