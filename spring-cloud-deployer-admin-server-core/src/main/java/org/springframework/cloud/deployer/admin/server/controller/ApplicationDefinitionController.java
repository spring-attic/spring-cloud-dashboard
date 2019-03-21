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

package org.springframework.cloud.deployer.admin.server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.admin.core.ApplicationDefinition;
import org.springframework.cloud.deployer.admin.registry.AppRegistry;
import org.springframework.cloud.deployer.admin.rest.resource.ApplicationDefinitionResource;
import org.springframework.cloud.deployer.admin.server.repository.ApplicationDefinitionRepository;
import org.springframework.cloud.deployer.admin.server.repository.DeploymentIdRepository;
import org.springframework.cloud.deployer.admin.server.repository.NoSuchApplicationDefinitionException;
import org.springframework.cloud.deployer.admin.server.repository.support.SearchPageable;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for operations on {@link ApplicationDefinition}.  This includes CRUD operations.
 *
 * @author Janne Valkealahti
 */
@RestController
@RequestMapping("/applications/definitions")
@ExposesResourceFor(ApplicationDefinitionResource.class)
public class ApplicationDefinitionController {

	private static final Logger logger = LoggerFactory.getLogger(ApplicationDefinitionController.class);
	private final Assembler applicationAssembler = new Assembler();
	private final ApplicationDefinitionRepository definitionRepository;
	private final ApplicationDeploymentController deploymentController;
	private final DeploymentIdRepository deploymentIdRepository;
	private final AppDeployer appDeployer;
	private final AppRegistry appRegistry;

	public ApplicationDefinitionController(ApplicationDefinitionRepository definitionRepository,
			DeploymentIdRepository deploymentIdRepository, ApplicationDeploymentController deploymentController,
			AppDeployer appDeployer, AppRegistry appRegistry) {
		Assert.notNull(definitionRepository, "ApplicationDefinitionRepository must not be null");
		Assert.notNull(deploymentIdRepository, "DeploymentIdRepository must not be null");
		Assert.notNull(deploymentController, "ApplicationDeploymentController must not be null");
		Assert.notNull(appDeployer, "AppDeployer must not be null");
		Assert.notNull(appRegistry, "AppRegistry must not be null");
		this.definitionRepository = definitionRepository;
		this.deploymentIdRepository = deploymentIdRepository;
		this.deploymentController = deploymentController;
		this.appDeployer = appDeployer;
		this.appRegistry = appRegistry;
	}

	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<ApplicationDefinitionResource> list(Pageable pageable, @RequestParam(required=false) String search,
			PagedResourcesAssembler<ApplicationDefinition> assembler) {
		if (search != null) {
			final SearchPageable searchPageable = new SearchPageable(pageable, search);
			searchPageable.addColumns("DEFINITION_NAME", "DEFINITION");
			return assembler.toResource(definitionRepository.search(searchPageable), applicationAssembler);
		}
		else {
			return assembler.toResource(definitionRepository.findAll(pageable), applicationAssembler);
		}
	}

	@RequestMapping(value = "", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void save(@RequestParam("name") String name,
					@RequestParam("definition") String dsl,
					@RequestParam(value = "deploy", defaultValue = "false")
					boolean deploy) {
		ApplicationDefinition application = new ApplicationDefinition(name, dsl);
		this.definitionRepository.save(application);
		if (deploy) {
			deploymentController.deploy(name, null);
		}
	}

	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void delete(@PathVariable("name") String name) {
		if (definitionRepository.findOne(name) == null) {
			throw new NoSuchApplicationDefinitionException(name);
		}
		deploymentController.undeploy(name);
		this.definitionRepository.delete(name);
	}

	@RequestMapping(value = "", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void deleteAll() throws Exception {
		deploymentController.undeployAll();
		this.definitionRepository.deleteAll();
	}

	private String calculateApplicationState(String name) {
		ApplicationDefinition application = definitionRepository.findOne(name);
		logger.debug("Calcuating application state for stream " + application.getName());

		String key = forApplicationDefinition(application);
		String id = this.deploymentIdRepository.findOne(key);

		logger.debug("Application Deployment Key = {},  Id = {}", key, id);

		if (id != null) {
			AppStatus status = appDeployer.status(id);
			DeploymentState deploymentState = status.getState();
			logger.debug("Stream Deployment Key = {}, Deployment State = {}", key, deploymentState);
			return deploymentState.toString();
		} else {
			return DeploymentState.unknown.toString();
		}
	}

	public static String forApplicationDefinition(ApplicationDefinition applicationDefinition) {
		Assert.notNull(applicationDefinition, "applicationDefinition must not be null");
		return String.format("%s.%s", applicationDefinition.getRegisteredAppName(), applicationDefinition.getName());
	}

	class Assembler extends ResourceAssemblerSupport<ApplicationDefinition, ApplicationDefinitionResource> {

		public Assembler() {
			super(ApplicationDefinitionController.class, ApplicationDefinitionResource.class);
		}

		@Override
		public ApplicationDefinitionResource toResource(ApplicationDefinition applicationDefinition) {
			return createResourceWithId(applicationDefinition.getName(), applicationDefinition	);
		}

		@Override
		public ApplicationDefinitionResource instantiateResource(ApplicationDefinition application) {
			ApplicationDefinitionResource resource = new ApplicationDefinitionResource(application.getName(), application.getDslText());
			resource.setStatus(calculateApplicationState(application.getName()));
			return resource;
		}
	}
}
