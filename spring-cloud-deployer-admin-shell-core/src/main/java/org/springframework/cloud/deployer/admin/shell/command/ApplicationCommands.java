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

package org.springframework.cloud.deployer.admin.shell.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.cloud.deployer.admin.rest.client.ApplicationOperations;
import org.springframework.cloud.deployer.admin.rest.client.DataFlowOperations;
import org.springframework.cloud.deployer.admin.rest.resource.ApplicationDefinitionResource;
import org.springframework.cloud.deployer.admin.rest.util.DeploymentPropertiesUtils;
import org.springframework.cloud.deployer.admin.shell.config.DataFlowShell;
import org.springframework.core.io.FileSystemResource;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.stereotype.Component;

/**
 * Application commands sharing same space for registry commands
 * but handling individual application deployments.
 *
 * @author Janne Valkealahti
 */
@Component
public class ApplicationCommands implements CommandMarker {

	private static final String LIST_APPLICATION = "app instances";
	private static final String CREATE_APPLICATION = "app create";
	private static final String DEPLOY_APPLICATION = "app deploy";
	private static final String UNDEPLOY_APPLICATION = "app undeploy";
	private static final String DESTROY_APPLICATION = "app destroy";
	private static final String UNDEPLOY_APPLICATION_ALL = "app all undeploy";
	private static final String DESTROY_APPLICATION_ALL = "app all destroy";
	private static final String PROPERTIES_OPTION = "properties";
	private static final String PROPERTIES_FILE_OPTION = "propertiesFile";

	@Autowired
	private DataFlowShell dataFlowShell;

	@Autowired
	private UserInput userInput;

	@CliAvailabilityIndicator({ LIST_APPLICATION, CREATE_APPLICATION, DEPLOY_APPLICATION, UNDEPLOY_APPLICATION,
			DESTROY_APPLICATION, UNDEPLOY_APPLICATION_ALL, DESTROY_APPLICATION_ALL })
	public boolean available() {
		DataFlowOperations dataFlowOperations = dataFlowShell.getDataFlowOperations();
		return dataFlowOperations != null && dataFlowOperations.applicationOperations() != null;
	}

	@CliCommand(value = LIST_APPLICATION, help = "List created applications")
	public Table listApplications() {
		final PagedResources<ApplicationDefinitionResource> applications = applicationOperations().list();
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("name", "Application Name");
		headers.put("dslText", "Application Definition");
		headers.put("status", "Status");
		BeanListTableModel<ApplicationDefinitionResource> model = new BeanListTableModel<>(applications, headers);
		return DataFlowTables.applyStyle(new TableBuilder(model))
				.build();
	}

	@CliCommand(value = CREATE_APPLICATION, help = "Create a new application definition")
	public String createApplication(
			@CliOption(mandatory = true, key = { "", "name" }, help = "the name to give to the application") String name,
			@CliOption(mandatory = true, key = { "definition" }, help = "a application definition, using the DSL") String dsl,
			@CliOption(key = "deploy", help = "whether to deploy the application immediately", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean deploy) {
		applicationOperations().createApplication(name, dsl, deploy);
		String message = String.format("Created new application '%s'", name);
		if (deploy) {
			message += "\nDeployment request has been sent";
		}
		return message;
	}

	@CliCommand(value = DEPLOY_APPLICATION, help = "Deploy a previously created application")
	public String deployStream(
			@CliOption(key = { "", "name" }, help = "the name of the application to deploy", mandatory = true) String name,
			@CliOption(key = { PROPERTIES_OPTION }, help = "the properties for this deployment", mandatory = false) String properties,
			@CliOption(key = { PROPERTIES_FILE_OPTION }, help = "the properties for this deployment (as a File)", mandatory = false) File propertiesFile
			) throws IOException {
		int which = Assertions.atMostOneOf(PROPERTIES_OPTION, properties, PROPERTIES_FILE_OPTION, propertiesFile);
		Map<String, String> propertiesToUse;
		switch (which) {
			case 0:
				propertiesToUse = DeploymentPropertiesUtils.parse(properties);
				break;
			case 1:
				String extension = FilenameUtils.getExtension(propertiesFile.getName());
				Properties props = null;
				if (extension.equals("yaml") || extension.equals("yml")) {
					YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
					yamlPropertiesFactoryBean.setResources(new FileSystemResource(propertiesFile));
					yamlPropertiesFactoryBean.afterPropertiesSet();
					props = yamlPropertiesFactoryBean.getObject();
				}
				else {
					props = new Properties();
					try (FileInputStream fis = new FileInputStream(propertiesFile)) {
						props.load(fis);
					}
				}
				propertiesToUse = DeploymentPropertiesUtils.convert(props);
				break;
			case -1: // Neither option specified
				propertiesToUse = Collections.<String, String> emptyMap();
				break;
			default:
				throw new AssertionError();
		}
		applicationOperations().deploy(name, propertiesToUse);
		return String.format("Deployment request has been sent for application '%s'", name);
	}

	@CliCommand(value = UNDEPLOY_APPLICATION, help = "Un-deploy a previously deployed application")
	public String undeployStream(
			@CliOption(key = { "", "name" }, help = "the name of the application to un-deploy", mandatory = true) String name
			) {
		applicationOperations().undeploy(name);
		return String.format("Un-deployed application '%s'", name);
	}

	@CliCommand(value = DESTROY_APPLICATION, help = "Destroy an existing application")
	public String destroyStream(
			@CliOption(key = { "", "name" }, help = "the name of the application to destroy", mandatory = true) String name) {
		applicationOperations().destroy(name);
		return String.format("Destroyed application '%s'", name);
	}

	@CliCommand(value = UNDEPLOY_APPLICATION_ALL, help = "Un-deploy all previously deployed applications")
	public String undeployAllApplications(
			@CliOption(key = "force", help = "bypass confirmation prompt", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean force
			) {
		if (force || "y".equalsIgnoreCase(userInput.promptWithOptions("Really undeploy all applications?", "n", "y", "n"))) {
			applicationOperations().undeployAll();
			return String.format("Un-deployed all the applications");
		}
		else {
			return "";
		}
	}

	@CliCommand(value = DESTROY_APPLICATION_ALL, help = "Destroy all existing applications")
	public String destroyAllStreams(
			@CliOption(key = "force", help = "bypass confirmation prompt", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean force) {
		if (force || "y".equalsIgnoreCase(userInput.promptWithOptions("Really destroy all applications?", "n", "y", "n"))) {
			applicationOperations().destroyAll();
			return "Destroyed all applications";
		}
		else {
			return "";
		}
	}

	ApplicationOperations applicationOperations() {
		return dataFlowShell.getDataFlowOperations().applicationOperations();
	}
}
