/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.deployer.admin.rest.resource;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.hateoas.PagedResources;

/**
 * Extension of {@link AppRegistrationResource} that contains application options
 * and other detailed application information.
 *
 * @author Eric Bottard
 * @author Gunnar Hillert
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class DetailedAppRegistrationResource extends AppRegistrationResource {

	/**
	 * Optional short description of the application.
	 */
	private String shortDescription;

	/**
	 * List of application options.
	 */
	private final List<ConfigurationMetadataProperty> options = new ArrayList<>();


	/**
	 * Default constructor for serialization frameworks.
	 */
	protected DetailedAppRegistrationResource() {
	}

	/**
	 * Construct a {@code DetailedAppRegistrationResource} object.
	 *
	 * @param name application name
	 * @param type application type
	 * @param coordinates Maven coordinates for the application artifact
	 */
	public DetailedAppRegistrationResource(String name, String type, String coordinates) {
		super(name, type, coordinates);
	}

	/**
	 * Construct a {@code DetailedAppRegistrationResource} object based
	 * on the provided {@link AppRegistrationResource}.
	 *
	 * @param resource {@code AppRegistrationResource} from which to obtain
	 *                 app registration data
	 */
	public DetailedAppRegistrationResource(AppRegistrationResource resource) {
		super(resource.getName(), resource.getType(), resource.getUri());
	}

	/**
	 * Add an application option.
	 *
	 * @param option application option to add
	 */
	public void addOption(ConfigurationMetadataProperty option) {
		options.add(option);
	}

	/**
	 * Return a list of application options.
	 *
	 * @return list of application options
	 */
	public List<ConfigurationMetadataProperty> getOptions() {
		return options;
	}

	/**
	 * Set a description for this application.
	 *
	 * @param shortDescription description for application
	 */
	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}

	/**
	 * Return a description for this application.
	 *
	 * @return description for this application
	 */
	public String getShortDescription() {
		return shortDescription;
	}

	/**
	 * Dedicated subclass to workaround type erasure.
	 */
	public static class Page extends PagedResources<DetailedAppRegistrationResource> {
	}

}
