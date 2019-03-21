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

package org.springframework.cloud.deployer.admin.rest.client;

import org.springframework.cloud.deployer.admin.rest.resource.AppStatusResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation for {@link RuntimeOperations}.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
public class RuntimeTemplate implements RuntimeOperations {

	private final RestTemplate restTemplate;

	/**
	 * Uri template for accessing status of all apps.
	 */
	private final Link appStatusesUriTemplate;

	/**
	 * Uri template for accessing status of a single app.
	 */
	private final Link appStatusUriTemplate;

	RuntimeTemplate(RestTemplate restTemplate, ResourceSupport resources) {
		this.restTemplate = restTemplate;
		this.appStatusesUriTemplate = resources.getLink("runtime/apps");
		this.appStatusUriTemplate = resources.getLink("runtime/apps/app");
	}

	@Override
	public PagedResources<AppStatusResource> status() {
		return restTemplate.getForObject(appStatusesUriTemplate.expand().getHref(), AppStatusResource.Page.class);
	}

	@Override
	public AppStatusResource status(String deploymentId) {
		return restTemplate.getForObject(appStatusUriTemplate.expand(deploymentId).getHref(), AppStatusResource.class);
	}
}
