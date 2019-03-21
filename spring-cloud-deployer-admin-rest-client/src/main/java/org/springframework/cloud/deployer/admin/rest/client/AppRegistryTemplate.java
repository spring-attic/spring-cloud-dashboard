/*
 * Copyright 2015-2017 the original author or authors.
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

import java.util.Properties;

import org.springframework.cloud.deployer.admin.rest.resource.AppRegistrationResource;
import org.springframework.cloud.deployer.admin.rest.resource.DetailedAppRegistrationResource;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.UriTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of {@link AppRegistryOperations} that uses {@link RestTemplate}
 * to issue commands to the Data Flow server.
 *
 * @author Eric Bottard
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Patrick Peralta
 */
public class AppRegistryTemplate implements AppRegistryOperations {

	/**
	 * Template used for http interaction.
	 */
	protected RestTemplate restTemplate;

	/**
	 * Template for URI creation.
	 */
	private final UriTemplate uriTemplate;

	/**
	 * Construct a {@code AppRegistryTemplate} object.
	 *
	 * @param restTemplate template for HTTP/rest commands
	 * @param resourceSupport HATEOAS link support
	 */
	public AppRegistryTemplate(RestTemplate restTemplate, ResourceSupport resourceSupport) {
		this.restTemplate = restTemplate;
		this.uriTemplate = new UriTemplate(resourceSupport.getLink("apps").getHref());
	}

	@Override
	public PagedResources<AppRegistrationResource> list() {
		return list(null);
	}

	@Override
	public PagedResources<AppRegistrationResource> list(String type) {
		String uri = uriTemplate + "?size=10000" + ((type == null) ? "" : "&type=" + type);
		return restTemplate.getForObject(uri, AppRegistrationResource.Page.class);
	}

	@Override
	public void unregister(String name, String applicationType) {
		String uri = uriTemplate.toString() + "/{type}/{name}";
		restTemplate.delete(uri, applicationType, name);
	}

	@Override
	public DetailedAppRegistrationResource info(String name, String type) {
		String uri = uriTemplate.toString() + "/{type}/{name}";
		return restTemplate.getForObject(uri, DetailedAppRegistrationResource.class, type, name);
	}

	@Override
	public AppRegistrationResource register(String name, String type,
			String uri, boolean force) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("uri", uri);
		values.add("force", Boolean.toString(force));

		return restTemplate.postForObject(uriTemplate.toString() + "/{type}/{name}", values,
				AppRegistrationResource.class, type, name);
	}

	@Override
	public PagedResources<AppRegistrationResource> importFromResource(String uri, boolean force) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("uri", uri);
		values.add("force", Boolean.toString(force));
		return restTemplate.postForObject(uriTemplate.toString(), values,
				AppRegistrationResource.Page.class);
	}

	@Override
	public PagedResources<AppRegistrationResource> registerAll(Properties apps, boolean force) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		StringBuffer buffer = new StringBuffer();
		for (String key : apps.stringPropertyNames()) {
			buffer.append(String.format("%s=%s\n", key, apps.getProperty(key)));
		}
		values.add("apps", buffer.toString());
		values.add("force", Boolean.toString(force));
		return restTemplate.postForObject(uriTemplate.toString(), values,
				AppRegistrationResource.Page.class);
	}
}
