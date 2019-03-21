/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.deployer.admin.configuration.metadata;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.boot.loader.jar.JarFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StringUtils;

/**
 * An {@link ApplicationConfigurationMetadataResolver} that knows how to look inside Spring Boot uber-jars.
 *
 * Supports Boot 1.3 and 1.4+ layouts thanks to a pluggable BootClassLoaderCreation strategy.
 *
 * @author Eric Bottard
 */
public class BootApplicationConfigurationMetadataResolver extends ApplicationConfigurationMetadataResolver {

	private static final String CONFIGURATION_METADATA_PATTERN = "classpath*:/META-INF/spring-configuration-metadata.json";

	private static final String WHITELIST_PROPERTIES = "classpath*:/META-INF/spring-configuration-metadata-whitelist.properties";

	private static final String CONFIGURATION_PROPERTIES_CLASSES = "configuration-properties.classes";

	private static final String CONFIGURATION_PROPERTIES_NAMES = "configuration-properties.names";

	private final Set<String> globalWhiteListedProperties = new HashSet<>();

	private final Set<String> globalWhiteListedClasses = new HashSet<>();

	private final ClassLoader parent;

	public BootApplicationConfigurationMetadataResolver() {
		this(null);
	}

	public BootApplicationConfigurationMetadataResolver(ClassLoader parent) {
		this.parent = parent;
		JarFile.registerUrlProtocolHandler();
		try {
			Resource[] globalResources = new PathMatchingResourcePatternResolver(ApplicationConfigurationMetadataResolver.class.getClassLoader()).getResources(WHITELIST_PROPERTIES);
			loadWhiteLists(globalResources, globalWhiteListedClasses, globalWhiteListedProperties);
		}
		catch (IOException e) {
			throw new RuntimeException("Error reading global white list of configuration properties", e);
		}
	}

	@Override
	public boolean supports(Resource app) {
		try {
			resolveAsArchive(app);
			return true;
		}
		catch (IOException | IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Return metadata about configuration properties that are documented via
	 * <a href="http://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html">
	 * Spring Boot configuration metadata</a> and visible in an app.
	 * @param app a Spring Cloud Stream app; typically a Boot uberjar,
	 *            but directories are supported as well
	 */
	public List<ConfigurationMetadataProperty> listProperties(Resource app, boolean exhaustive) {
		try {
			Archive archive = resolveAsArchive(app);
			return listProperties(archive, exhaustive);
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to list properties for " + app, e);
		}
	}

	public List<ConfigurationMetadataProperty> listProperties(Archive archive, boolean exhaustive) {
		try (URLClassLoader moduleClassLoader = new BootClassLoaderFactory(archive, parent).createClassLoader()) {
			List<ConfigurationMetadataProperty> result = new ArrayList<>();
			ResourcePatternResolver moduleResourceLoader = new PathMatchingResourcePatternResolver(moduleClassLoader);
			Collection<String> whiteListedClasses = new HashSet<>(globalWhiteListedClasses);
			Collection<String> whiteListedProperties = new HashSet<>(globalWhiteListedProperties);
			Resource[] whitelistDescriptors = moduleResourceLoader.getResources(WHITELIST_PROPERTIES);
			boolean include = (whitelistDescriptors.length == 0) || exhaustive; // when no descriptors, return everything
			loadWhiteLists(whitelistDescriptors, whiteListedClasses, whiteListedProperties);
			ConfigurationMetadataRepositoryJsonBuilder builder = ConfigurationMetadataRepositoryJsonBuilder.create();
			for (Resource r : moduleResourceLoader.getResources(CONFIGURATION_METADATA_PATTERN)) {
				builder.withJsonResource(r.getInputStream());
			}
			for (ConfigurationMetadataGroup group : builder.build().getAllGroups().values()) {
				if (include || isWhiteListed(group, whiteListedClasses)) {
					result.addAll(group.getProperties().values());
				} // Props in the root group have an id that looks prefixed itself. Handle here
				else if ("_ROOT_GROUP_".equals(group.getId())) {
					for (ConfigurationMetadataProperty property : group.getProperties().values()) {
						if (isWhiteListed(property, whiteListedProperties)) {
							result.add(property);
						}
					}
				}
				else { // Look for per property WL
					for (ConfigurationMetadataProperty property : group.getProperties().values()) {
						if (isWhiteListed(property, whiteListedProperties)) {
							result.add(property);
						}
					}
				}
			}
			return result;
		}
		catch (Exception e) {
			throw new RuntimeException("Exception trying to list configuration properties for application " + archive, e);
		}
	}

	@Override
	public URLClassLoader createAppClassLoader(Resource app) {
		try {
			return new BootClassLoaderFactory(resolveAsArchive(app), parent).createClassLoader();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Archive resolveAsArchive(Resource app) throws IOException {
			File moduleFile = app.getFile();
			return moduleFile.isDirectory() ? new ExplodedArchive(moduleFile) : new JarFileArchive(moduleFile);
	}

	/**
	 * Loads white lists of properties and group classes and add them to the given collections.
	 */
	private void loadWhiteLists(Resource[] resources, Collection<String> classes, Collection<String> names) throws IOException {
		for (Resource resource : resources) {
			Properties properties = new Properties();
			properties.load(resource.getInputStream());
			classes.addAll(Arrays.asList(StringUtils.delimitedListToStringArray(properties.getProperty(CONFIGURATION_PROPERTIES_CLASSES), ",", " ")));
			names.addAll(Arrays.asList(StringUtils.delimitedListToStringArray(properties.getProperty(CONFIGURATION_PROPERTIES_NAMES), ",", " ")));
		}
	}

	/**
	 * Return whether a single property has been white listed as being a "main" configuration property.
	 */
	private boolean isWhiteListed(ConfigurationMetadataProperty property, Collection<String> properties) {
		return properties.contains(property.getId());
	}

	/**
	 * Return whether a configuration property group (class) has been white listed as being a "main" group.
	 */
	private boolean isWhiteListed(ConfigurationMetadataGroup group, Collection<String> classes) {
		Set<String> sourceTypes = group.getSources().keySet();
		return !sourceTypes.isEmpty() && classes.containsAll(sourceTypes);
	}

}
