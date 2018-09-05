// Copyright 2018, Oath Inc.
// Licensed under the terms of the Apache 2.0 license. See the LICENSE file in the project root for terms.

package com.oath.maven.plugin.freemarker;

import java.nio.file.Path;

public interface OutputGeneratorPropertiesProvider {
	/**
	 * Must add three properties to the builder: the <b>templateLocation</b>, <b>outputLocation</b>, and <b>dataModel</b>
	 * The <b>pom updated timestamp</b> and <b>generatorLocation</b> are added elsewhere.
	 * @param path The path to the generator file, to be used to decide on the three properties above.
	 * @param builder The builder to which to add the properties.
	 */
	public void providePropertiesFromFile(Path path, OutputGenerator.OutputGeneratorBuilder builder);
}
