/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.cloud.deployer.admin.server.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.admin.service.JobService;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.admin.completion.CompletionConfiguration;
import org.springframework.cloud.deployer.admin.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.deployer.admin.registry.AppRegistry;
import org.springframework.cloud.deployer.admin.registry.EavRegistryRepository;
import org.springframework.cloud.deployer.admin.registry.RdbmsEavRegistryRepository;
import org.springframework.cloud.deployer.admin.registry.RdbmsUriRegistry;
import org.springframework.cloud.deployer.admin.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.deployer.admin.server.config.features.FeaturesProperties;
import org.springframework.cloud.deployer.admin.server.controller.AppRegistryController;
import org.springframework.cloud.deployer.admin.server.controller.ApplicationDefinitionController;
import org.springframework.cloud.deployer.admin.server.controller.ApplicationDeploymentController;
import org.springframework.cloud.deployer.admin.server.controller.CompletionController;
import org.springframework.cloud.deployer.admin.server.controller.FeaturesController;
import org.springframework.cloud.deployer.admin.server.controller.RestControllerAdvice;
import org.springframework.cloud.deployer.admin.server.controller.RootController;
import org.springframework.cloud.deployer.admin.server.controller.RuntimeAppsController;
import org.springframework.cloud.deployer.admin.server.controller.UiController;
import org.springframework.cloud.deployer.admin.server.controller.RuntimeAppsController.AppInstanceController;
import org.springframework.cloud.deployer.admin.server.controller.security.LoginController;
import org.springframework.cloud.deployer.admin.server.controller.security.SecurityController;
import org.springframework.cloud.deployer.admin.server.repository.ApplicationDefinitionRepository;
import org.springframework.cloud.deployer.admin.server.repository.DeploymentIdRepository;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResourceLoader;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.hateoas.EntityLinks;

/**
 * Configuration for the Data Flow Server Controllers.
 *
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Andy Clement
 */
@SuppressWarnings("all")
@Configuration
@Import(CompletionConfiguration.class)
@ConditionalOnBean({EnableDataFlowServerConfiguration.Marker.class, AppDeployer.class, TaskLauncher.class})
@EnableConfigurationProperties({FeaturesProperties.class})
@ConditionalOnProperty(prefix = "dataflow.server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataFlowControllerAutoConfiguration {

	@Bean
	public EavRegistryRepository eavRegistryRepository(DataSource dataSource) {
		return new RdbmsEavRegistryRepository(dataSource);
	}

	@Bean
	public UriRegistry uriRegistry(DataSource dataSource) {
		return new RdbmsUriRegistry(dataSource);
	}

	@Bean
	public AppRegistry appRegistry(UriRegistry uriRegistry, DelegatingResourceLoader resourceLoader,
			EavRegistryRepository eavRegistryRepository) {
		return new AppRegistry(uriRegistry, resourceLoader, eavRegistryRepository);
	}

	@Bean
	public RootController rootController(EntityLinks entityLinks) {
		return new RootController(entityLinks);
	}

	@Bean
	public AppInstanceController appInstanceController(AppDeployer appDeployer) {
		return new AppInstanceController(appDeployer);
	}

	@Bean
	@ConditionalOnBean(ApplicationDefinitionRepository.class)
	public RuntimeAppsController runtimeAppsController(ApplicationDefinitionRepository repository,
			DeploymentIdRepository deploymentIdRepository, AppDeployer appDeployer) {
		return new RuntimeAppsController(repository, deploymentIdRepository, appDeployer);
	}

	@Bean
	public MavenResourceLoader mavenResourceLoader(MavenProperties properties) {
		return new MavenResourceLoader(properties);
	}

	@Bean
	@ConditionalOnMissingBean(DelegatingResourceLoader.class)
	public DelegatingResourceLoader delegatingResourceLoader(MavenResourceLoader mavenResourceLoader) {
		Map<String, ResourceLoader> loaders = new HashMap<>();
		loaders.put("maven", mavenResourceLoader);
		return new DelegatingResourceLoader(loaders);
	}

	@Bean
	public CompletionController completionController() {
		return new CompletionController();
	}

	@Bean
	public AppRegistryController appRegistryController(AppRegistry appRegistry, ApplicationConfigurationMetadataResolver metadataResolver) {
		return new AppRegistryController(appRegistry, metadataResolver);
	}

	@Bean
	@ConditionalOnBean(ApplicationDefinitionRepository.class)
	public ApplicationDefinitionController applicationDefinitionController(ApplicationDefinitionRepository repository,
			DeploymentIdRepository deploymentIdRepository, ApplicationDeploymentController deploymentController,
			AppDeployer deployer, AppRegistry appRegistry) {
		return new ApplicationDefinitionController(repository, deploymentIdRepository, deploymentController, deployer, appRegistry);
	}

	@Bean
	@ConditionalOnBean(ApplicationDefinitionRepository.class)
	public ApplicationDeploymentController applicationDeploymentController(ApplicationDefinitionRepository repository,
			DeploymentIdRepository deploymentIdRepository, EavRegistryRepository eavRegistryRepository,
			AppDeployer deployer, AppRegistry appRegistry, ApplicationConfigurationMetadataResolver metadataResolver,
			CommonApplicationProperties appsProperties) {
		return new ApplicationDeploymentController(repository, deploymentIdRepository, eavRegistryRepository, deployer,
				appRegistry, metadataResolver, appsProperties);
	}

	@Bean
	public SecurityController securityController(SecurityProperties securityProperties) {
		return new SecurityController(securityProperties);
	}

	@Bean
	@ConditionalOnProperty("security.basic.enabled")
	public LoginController loginController() {
		return new LoginController();
	}

	@Bean
	public FeaturesController featuresController(FeaturesProperties featuresProperties) {
		return new FeaturesController(featuresProperties);
	}

	@Bean
	public UiController uiController() {
		return new UiController();
	}

	@Bean
	public RestControllerAdvice restControllerAdvice() {
		return new RestControllerAdvice();
	}

	@Bean
	public MavenProperties mavenProperties() {
		return new MavenConfigurationProperties();
	}

	@ConfigurationProperties(prefix = "maven")
	static class MavenConfigurationProperties extends MavenProperties {
	}
}
