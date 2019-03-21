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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.cloud.deployer.admin.core.ApplicationDefinition;
import org.springframework.cloud.deployer.admin.rest.resource.AppInstanceStatusResource;
import org.springframework.cloud.deployer.admin.rest.resource.AppStatusResource;
import org.springframework.cloud.deployer.admin.server.repository.ApplicationDefinitionRepository;
import org.springframework.cloud.deployer.admin.server.repository.DeploymentIdRepository;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes runtime status of deployed apps.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Janne Valkealahti
 */
@RestController
@RequestMapping("/runtime/apps")
@ExposesResourceFor(AppStatusResource.class)
public class RuntimeAppsController {

	private static final Comparator<? super AppInstanceStatus> INSTANCE_SORTER = new Comparator<AppInstanceStatus>() {
		@Override
		public int compare(AppInstanceStatus i1, AppInstanceStatus i2) {
			return i1.getId().compareTo(i2.getId());
		}
	};

	/**
	 * The repository this controller will use for deployment IDs.
	 */
	private final DeploymentIdRepository deploymentIdRepository;

	private final ApplicationDefinitionRepository applicationDefinitionRepository;

	/**
	 * The deployer this controller will use to deploy stream apps.
	 */
	private final AppDeployer appDeployer;

	private final ResourceAssembler<AppStatus, AppStatusResource> statusAssembler = new Assembler();

	/**
	 * Instantiates a new runtime apps controller.
	 *
	 * @param streamDefinitionRepository the repository this controller will use for stream CRUD operations
	 * @param deploymentIdRepository the repository this controller will use for deployment IDs
	 * @param appDeployer the deployer this controller will use to deploy stream apps
	 */
	public RuntimeAppsController(ApplicationDefinitionRepository applicationDefinitionRepository,
			DeploymentIdRepository deploymentIdRepository, AppDeployer appDeployer) {
		Assert.notNull(applicationDefinitionRepository, "ApplicationDefinitionRepository must not be null");
		Assert.notNull(deploymentIdRepository, "DeploymentIdRepository must not be null");
		Assert.notNull(appDeployer, "AppDeployer must not be null");
		this.applicationDefinitionRepository = applicationDefinitionRepository;
		this.deploymentIdRepository = deploymentIdRepository;
		this.appDeployer = appDeployer;
	}

	@RequestMapping
	public PagedResources<AppStatusResource> list(PagedResourcesAssembler<AppStatus> assembler) {
		List<AppStatus> values = new ArrayList<>();

		for (ApplicationDefinition applicationDefinition : this.applicationDefinitionRepository.findAll()) {
			String key = forApplicationDefinition(applicationDefinition);
			String id = this.deploymentIdRepository.findOne(key);
			if (id != null) {
				values.add(appDeployer.status(id));
			}
		}

		Collections.sort(values, new Comparator<AppStatus>() {
			@Override
			public int compare(AppStatus o1, AppStatus o2) {
				return o1.getDeploymentId().compareTo(o2.getDeploymentId());
			}
		});
		return assembler.toResource(new PageImpl<>(values), statusAssembler);
	}

	public static String forApplicationDefinition(ApplicationDefinition applicationDefinition) {
		Assert.notNull(applicationDefinition, "applicationDefinition must not be null");
		return String.format("%s.%s", applicationDefinition.getRegisteredAppName(), applicationDefinition.getName());
	}

	@RequestMapping("/{id}")
	public AppStatusResource display(@PathVariable String id) {
		AppStatus status = appDeployer.status(id);
		if (status != null) {
			return statusAssembler.toResource(status);
		}
		throw new ResourceNotFoundException();
	}

	private class Assembler extends ResourceAssemblerSupport<AppStatus, AppStatusResource> {

		public Assembler() {
			super(RuntimeAppsController.class, AppStatusResource.class);
		}

		@Override
		public AppStatusResource toResource(AppStatus entity) {
			return createResourceWithId(entity.getDeploymentId(), entity);
		}

		@Override
		protected AppStatusResource instantiateResource(AppStatus entity) {
			AppStatusResource resource = new AppStatusResource(entity.getDeploymentId(), entity.getState().name());
			List<AppInstanceStatusResource> instanceStatusResources = new ArrayList<>();
			InstanceAssembler instanceAssembler = new InstanceAssembler(entity);
			List<AppInstanceStatus> instanceStatuses = new ArrayList<>(entity.getInstances().values());
			Collections.sort(instanceStatuses, INSTANCE_SORTER);
			for (AppInstanceStatus appInstanceStatus : instanceStatuses) {
				instanceStatusResources.add(instanceAssembler.toResource(appInstanceStatus));
			}
			resource.setInstances(new Resources<>(instanceStatusResources));
			return resource;
		}
	}

	@RestController
	@RequestMapping("/runtime/apps/{appId}/instances")
	@ExposesResourceFor(AppInstanceStatusResource.class)
	public static class AppInstanceController {

		private final AppDeployer appDeployer;

		public AppInstanceController(AppDeployer appDeployer) {
			this.appDeployer = appDeployer;
		}

		@RequestMapping
		public PagedResources<AppInstanceStatusResource> list(@PathVariable String appId,
				PagedResourcesAssembler<AppInstanceStatus> assembler) {
			AppStatus status = appDeployer.status(appId);
			if (status != null) {
				List<AppInstanceStatus> appInstanceStatuses = new ArrayList<>(status.getInstances().values());
				Collections.sort(appInstanceStatuses, INSTANCE_SORTER);
				return assembler.toResource(new PageImpl<>(appInstanceStatuses), new InstanceAssembler(status));
			}
			throw new ResourceNotFoundException();
		}

		@RequestMapping("/{instanceId}")
		public AppInstanceStatusResource display(@PathVariable String appId, @PathVariable String instanceId) {
			AppStatus status = appDeployer.status(appId);
			if (status != null) {
				AppInstanceStatus appInstanceStatus = status.getInstances().get(instanceId);
				if (appInstanceStatus == null) {
					throw new ResourceNotFoundException();
				}
				return new InstanceAssembler(status).toResource(appInstanceStatus);
			}
			throw new ResourceNotFoundException();
		}

	}

	@SuppressWarnings("serial")
	@ResponseStatus(HttpStatus.NOT_FOUND)
	private static class ResourceNotFoundException extends RuntimeException {
	}

	private static class InstanceAssembler extends ResourceAssemblerSupport<AppInstanceStatus, AppInstanceStatusResource> {

		private final AppStatus owningApp;

		public InstanceAssembler(AppStatus owningApp) {
			super(AppInstanceController.class, AppInstanceStatusResource.class);
			this.owningApp = owningApp;
		}

		@Override
		public AppInstanceStatusResource toResource(AppInstanceStatus entity) {
			return createResourceWithId("/" + entity.getId(), entity, owningApp.getDeploymentId().toString());
		}

		@Override
		protected AppInstanceStatusResource instantiateResource(AppInstanceStatus entity) {
			return new AppInstanceStatusResource(entity.getId(), entity.getState().name(), entity.getAttributes());
		}
	}
}
