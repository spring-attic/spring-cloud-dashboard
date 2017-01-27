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

package org.springframework.cloud.deployer.admin.server.controller;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.deployer.admin.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.deployer.admin.core.ApplicationDefinition;
import org.springframework.cloud.deployer.admin.core.ApplicationType;
import org.springframework.cloud.deployer.admin.registry.AppRegistration;
import org.springframework.cloud.deployer.admin.registry.AppRegistry;
import org.springframework.cloud.deployer.admin.rest.resource.ApplicationDeploymentResource;
import org.springframework.cloud.deployer.admin.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.deployer.admin.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.deployer.admin.server.repository.ApplicationDefinitionRepository;
import org.springframework.cloud.deployer.admin.server.repository.DeploymentIdRepository;
import org.springframework.cloud.deployer.admin.server.repository.NoSuchApplicationDefinitionException;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.core.io.Resource;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/applications/deployments")
@ExposesResourceFor(ApplicationDeploymentResource.class)
public class ApplicationDeploymentController {

	private static Log logger = LogFactory.getLog(ApplicationDeploymentController.class);

	private final ApplicationDefinitionRepository definitionRepository;
	private final DeploymentIdRepository deploymentIdRepository;
	private final AppDeployer appDeployer;
	private final AppRegistry appRegistry;
	private final WhitelistProperties whitelistProperties;
	private final CommonApplicationProperties commonApplicationProperties;

	public ApplicationDeploymentController(ApplicationDefinitionRepository definitionRepository,
			DeploymentIdRepository deploymentIdRepository, AppDeployer appDeployer, AppRegistry appRegistry,
			ApplicationConfigurationMetadataResolver metadataResolver, CommonApplicationProperties commonProperties) {
		Assert.notNull(definitionRepository, "ApplicationDefinitionRepository must not be null");
		Assert.notNull(deploymentIdRepository, "DeploymentIdRepository must not be null");
		Assert.notNull(appDeployer, "AppDeployer must not be null");
		Assert.notNull(appRegistry, "AppRegistry must not be null");
		Assert.notNull(commonProperties, "CommonApplicationProperties must not be null");
		Assert.notNull(metadataResolver, "MetadataResolver must not be null");
		this.definitionRepository = definitionRepository;
		this.deploymentIdRepository = deploymentIdRepository;
		this.appDeployer = appDeployer;
		this.appRegistry = appRegistry;
		this.whitelistProperties = new WhitelistProperties(metadataResolver);
		this.commonApplicationProperties = commonProperties;
	}

	@RequestMapping(value = "/{name}", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public void deploy(@PathVariable("name") String name,
			@RequestParam(required = false) String properties) {
		ApplicationDefinition application = this.definitionRepository.findOne(name);
		if (application == null) {
			throw new NoSuchApplicationDefinitionException(name);
		}
		String status = calculateApplicationState(name);
		if (DeploymentState.deployed.equals(DeploymentState.valueOf(status))) {
			throw new ApplicationAlreadyDeployedException(name);
		}
		else if (DeploymentState.deploying.equals(DeploymentState.valueOf(status))) {
			throw new ApplicationAlreadyDeployingException(name);
		}
		deployApplication(application, DeploymentPropertiesUtils.parse(properties));
	}

	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void undeploy(@PathVariable("name") String name) {
		ApplicationDefinition stream = this.definitionRepository.findOne(name);
		if (stream == null) {
			throw new NoSuchApplicationDefinitionException(name);
		}
		undeployApplication(stream);
	}

	@RequestMapping(value = "", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void undeployAll() {
		for (ApplicationDefinition stream : this.definitionRepository.findAll()) {
			this.undeployApplication(stream);
		}
	}

	private String calculateApplicationState(String name) {
		Set<DeploymentState> appStates = EnumSet.noneOf(DeploymentState.class);
		ApplicationDefinition application = this.definitionRepository.findOne(name);
		return "unknown";
	}

	private void undeployApplication(ApplicationDefinition application) {
		String key = forApplicationDefinition(application);
		String id = this.deploymentIdRepository.findOne(key);
		if (id != null) {
			AppStatus status = this.appDeployer.status(id);
			if (!EnumSet.of(DeploymentState.unknown, DeploymentState.undeployed)
					.contains(status.getState())) {
				this.appDeployer.undeploy(id);
			}
			this.deploymentIdRepository.delete(key);
		}
	}

	private void deployApplication(ApplicationDefinition application, Map<String, String> applicationDeploymentProperties) {
		logger.info("Deploying application [" + application + "]");
		if (applicationDeploymentProperties == null) {
			applicationDeploymentProperties = Collections.emptyMap();
		}

		AppRegistration registration = this.appRegistry.find(application.getRegisteredAppName(), ApplicationType.generic);

		Map<String, String> deployerDeploymentProperties = DeploymentPropertiesUtils
				.extractAndQualifyDeployerProperties(applicationDeploymentProperties, application.getRegisteredAppName());
		deployerDeploymentProperties.put(AppDeployer.GROUP_PROPERTY_KEY, application.getName());

		Resource resource = registration.getResource();

		AppDefinition revisedDefinition = mergeAndExpandAppProperties(application, resource, applicationDeploymentProperties);
		logger.info("Using AppDefinition [" + revisedDefinition + "]");
		AppDeploymentRequest request = new AppDeploymentRequest(revisedDefinition, resource, deployerDeploymentProperties);
		logger.info("Using AppDeploymentRequest [" + request + "]");

		try {
			String id = this.appDeployer.deploy(request);
			this.deploymentIdRepository.save(forApplicationDefinition(application), id);
		}
		// If the deployer implementation handles the deployment request synchronously, log error message if
		// any exception is thrown out of the deployment and proceed to the next deployment.
		catch (Exception e) {
			logger.error(String.format("Exception when deploying the app %s: %s", application, e.getMessage()), e);
		}

	}

	AppDefinition mergeAndExpandAppProperties(ApplicationDefinition original, Resource resource, Map<String, String> appDeployTimeProperties) {
		Map<String, String> merged = new HashMap<>(original.getProperties());
		merged.putAll(appDeployTimeProperties);
		merged = whitelistProperties.qualifyProperties(merged, resource);
		return new AppDefinition(original.getRegisteredAppName(), merged);
	}

	public static String forApplicationDefinition(ApplicationDefinition applicationDefinition) {
		Assert.notNull(applicationDefinition, "applicationDefinition must not be null");
		return String.format("%s.%s", applicationDefinition.getRegisteredAppName(), applicationDefinition.getName());
	}

}
