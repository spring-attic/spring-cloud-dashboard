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
import org.springframework.cloud.deployer.admin.rest.util.DeploymentPropertiesUtils;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation for {@link ApplicationOperations}.
 *
 * @author Janne Valkealahti
 */
public class ApplicationTemplate implements ApplicationOperations {

	public static final String DEFINITIONS_REL = "applications/definitions";
	private static final String DEFINITION_REL = "applications/definitions/definition";
	private static final String DEPLOYMENTS_REL = "applications/deployments";
	private static final String DEPLOYMENT_REL = "applications/deployments/deployment";
	private final RestTemplate restTemplate;
	private final Link definitionsLink;
	private final Link definitionLink;
	private final Link deploymentsLink;
	private final Link deploymentLink;

	ApplicationTemplate(RestTemplate restTemplate, ResourceSupport resources) {
		Assert.notNull(restTemplate, "RestTemplate can't be null");
		Assert.notNull(resources, "URI Resources can't be null");
		Assert.notNull(resources.getLink(DEFINITIONS_REL), "Definitions relation is required");
		Assert.notNull(resources.getLink(DEFINITION_REL), "Definition relation is required");
		this.restTemplate = restTemplate;
		this.definitionsLink = resources.getLink(DEFINITIONS_REL);
		this.definitionLink = resources.getLink(DEFINITION_REL);
		this.deploymentsLink = resources.getLink(DEPLOYMENTS_REL);
		this.deploymentLink = resources.getLink(DEPLOYMENT_REL);
	}

	@Override
	public PagedResources<ApplicationDefinitionResource> list() {
		String uriTemplate = definitionsLink.expand().getHref();
		uriTemplate = uriTemplate + "?size=10000";
		return restTemplate.getForObject(uriTemplate, ApplicationDefinitionResource.Page.class);
	}

	@Override
	public ApplicationDefinitionResource createApplication(String name, String definition, boolean deploy) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		values.add("name", name);
		values.add("definition", definition);
		values.add("deploy", Boolean.toString(deploy));
		ApplicationDefinitionResource stream = restTemplate.postForObject(definitionsLink.expand().getHref(), values,
				ApplicationDefinitionResource.class);
		return stream;
	}

	@Override
	public void deploy(String name, Map<String, String> properties) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		values.add("properties", DeploymentPropertiesUtils.format(properties));
		restTemplate.postForObject(deploymentLink.expand(name).getHref(), values, Object.class);
	}

	@Override
	public void undeploy(String name) {
		restTemplate.delete(deploymentLink.expand(name).getHref());
	}

	@Override
	public void undeployAll() {
		restTemplate.delete(deploymentsLink.getHref());
	}

	@Override
	public void destroy(String name) {
		restTemplate.delete(definitionLink.expand(name).getHref());
	}

	@Override
	public void destroyAll() {
		restTemplate.delete(definitionsLink.getHref());
	}
}
