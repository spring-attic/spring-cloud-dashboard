/*
 * Copyright 2017 the original author or authors.
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

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;

/**
 * A HATEOAS representation of an application definition.
 *
 * @author Janne Valkealahti
 */
public class ApplicationDefinitionResource extends ResourceSupport {

	private String name;
	private String dslText;
	private String status;

	ApplicationDefinitionResource() {
	}

	public ApplicationDefinitionResource(String name, String dslText) {
		this.name = name;
		this.dslText = dslText;
	}

	public String getName() {
		return name;
	}

	public String getDslText() {
		return dslText;
	}

	/**
	 * Return the status of this task (i.e. running, complete, etc)
	 *
	 * @return task status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Set the status of this task (i.e. running, complete, etc)
	 *
	 * @param status task status
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	public static class Page extends PagedResources<ApplicationDefinitionResource> {
	}
}
