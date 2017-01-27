/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.deployer.admin.rest.client;

import java.util.Map;

import org.springframework.cloud.deployer.admin.rest.resource.ApplicationDefinitionResource;
import org.springframework.hateoas.PagedResources;

public interface ApplicationOperations {

	/**
	 * List applications known to the system.
	 */
	public PagedResources<ApplicationDefinitionResource> list();

	/**
	 * Create a new application, optionally deploying it.
	 */
	public ApplicationDefinitionResource createApplication(String name, String definition, boolean deploy);

	/**
	 * Deploy an already created application.
	 */
	public void deploy(String name, Map<String, String> properties);

	/**
	 * Undeploy a deployed application, retaining its definition.
	 */
	public void undeploy(String name);

	/**
	 * Undeploy all currently deployed applications.
	 */
	public void undeployAll();

	/**
	 * Destroy an existing application.
	 */
	public void destroy(String name);

	/**
	 * Destroy all applications known to the system.
	 */
	public void destroyAll();
}
