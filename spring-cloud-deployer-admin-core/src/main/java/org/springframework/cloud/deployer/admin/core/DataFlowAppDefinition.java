/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.deployer.admin.core;

import java.util.Map;

import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.util.Assert;

/**
 * Base class for task and stream app definitions, wrapping the underlying {@link AppDefinition} instance.
 *
 * @author Mark Fisher
 */
abstract class DataFlowAppDefinition {

	/**
	 * The name of the registered app.
	 */
	private volatile String registeredAppName;

	/**
	 * The underlying {@link AppDefinition}.
	 */
	protected volatile AppDefinition appDefinition;

	/**
	 * Construct a {@code DataFlowAppDefinition}.
	 */
	protected DataFlowAppDefinition() {
	}

	/**
	 * Construct a {@code DataFlowAppDefinition}.
	 *
	 * @param registeredAppName name of application in registry
	 * @param label label used for application
	 * @param properties app properties; may be {@code null}
	 */
	protected DataFlowAppDefinition(String registeredAppName, String label, Map<String, String> properties) {
		Assert.notNull(registeredAppName, "registeredAppName must not be null");
		Assert.notNull(label, "label must not be null");
		this.registeredAppName = registeredAppName;
		this.appDefinition = new AppDefinition(label, properties);
	}

	/**
	 * Return the name from the {@link AppDefinition}.
	 * 
	 * @return the name from the {@link AppDefinition}
	 */
	public String getName() {
		return this.appDefinition.getName();
	}

	/**
	 * Return the name of the registered app.
	 *
	 * @return name of app in registry
	 */
	public String getRegisteredAppName() {
		return registeredAppName;
	}

	/**
	 * Set the registered app name. Only intended for subclasses to invoke.
	 *
	 * @param registeredAppName the registered app name
	 */
	protected void setRegisteredAppName(String registeredAppName) {
		this.registeredAppName = registeredAppName;
	}

	/**
	 * Gets the app definition properties. These properties are passed into a running app.
	 *
	 * @return the unmodifiable map of app properties
	 */
	public Map<String, String> getProperties() {
		return this.appDefinition.getProperties();
	}

}
