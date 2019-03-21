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

package org.springframework.cloud.deployer.admin.server.controller;

import static org.springframework.cloud.deployer.admin.server.controller.UiController.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.admin.rest.resource.AppInstanceStatusResource;
import org.springframework.cloud.deployer.admin.rest.resource.AppRegistrationResource;
import org.springframework.cloud.deployer.admin.rest.resource.AppStatusResource;
import org.springframework.cloud.deployer.admin.rest.resource.ApplicationDefinitionResource;
import org.springframework.cloud.deployer.admin.rest.resource.ApplicationDeploymentResource;
import org.springframework.cloud.deployer.admin.rest.resource.CompletionProposalsResource;
import org.springframework.cloud.deployer.admin.server.config.features.FeaturesProperties;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;

/**
 * Controller for the root resource of the Data Flow server.
 *
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 * @author Mark Fisher
 */
@RestController
@EnableConfigurationProperties(FeaturesProperties.class)
public class RootController {

	@Autowired
	private FeaturesProperties featuresProperties;

	/**
	 * Contains links pointing to controllers backing an entity type
	 * (such as streams).
	 */
	private final EntityLinks entityLinks;

	/**
	 * Construct an {@code RootController}.
	 *
	 * @param entityLinks holder of links to controllers and their associated entity types
	 */
	public RootController(EntityLinks entityLinks) {
		this.entityLinks = entityLinks;
	}

	/**
	 * Return a {@link ResourceSupport} object containing the resources
	 * served by the Data Flow server.
	 *
	 * @return {@code ResourceSupport} object containing the Data Flow server's resources
	 */
	@RequestMapping("/")
	public ResourceSupport info() {
		ResourceSupport resourceSupport = new ResourceSupport();
		resourceSupport.add(new Link(dashboard(""), "dashboard"));
		resourceSupport.add(entityLinks.linkToCollectionResource(AppRegistrationResource.class).withRel("apps"));

		resourceSupport.add(entityLinks.linkToCollectionResource(AppStatusResource.class).withRel("runtime/apps"));
		resourceSupport.add(unescapeTemplateVariables(entityLinks.linkForSingleResource(AppStatusResource.class, "{appId}").withRel("runtime/apps/app")));
		resourceSupport.add(unescapeTemplateVariables(entityLinks.linkFor(AppInstanceStatusResource.class, UriComponents.UriTemplateVariables.SKIP_VALUE).withRel("runtime/apps/instances")));

		resourceSupport.add(entityLinks.linkToCollectionResource(ApplicationDefinitionResource.class).withRel("applications/definitions"));
		resourceSupport.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(ApplicationDefinitionResource.class, "{name}").withRel("applications/definitions/definition")));
		resourceSupport.add(entityLinks.linkToCollectionResource(ApplicationDeploymentResource.class).withRel("applications/deployments"));
		resourceSupport.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(ApplicationDeploymentResource.class, "{name}").withRel("applications/deployments/deployment")));

		String completionStreamTemplated = entityLinks.linkFor(CompletionProposalsResource.class).withSelfRel().getHref() + ("/stream{?start,detailLevel}");
		resourceSupport.add(new Link(completionStreamTemplated).withRel("completions/stream"));
		String completionTaskTemplated = entityLinks.linkFor(CompletionProposalsResource.class).withSelfRel().getHref() + ("/task{?start,detailLevel}");
		resourceSupport.add(new Link(completionTaskTemplated).withRel("completions/task"));
		return resourceSupport;
	}

	// Workaround https://github.com/spring-projects/spring-hateoas/issues/234
	private Link unescapeTemplateVariables(Link raw) {
		return new Link(raw.getHref().replace("%7B", "{").replace("%7D", "}"), raw.getRel());
	}

}
