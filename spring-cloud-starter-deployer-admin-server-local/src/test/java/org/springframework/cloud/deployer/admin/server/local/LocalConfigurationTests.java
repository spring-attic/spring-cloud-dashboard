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

package org.springframework.cloud.deployer.admin.server.local;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.deployer.admin.registry.AppRegistry;
import org.springframework.cloud.deployer.admin.server.config.features.FeaturesProperties;
import org.springframework.cloud.deployer.admin.server.local.dataflowapp.LocalTestDataFlowServer;
import org.springframework.cloud.deployer.admin.server.local.nodataflowapp.LocalTestNoDataFlowServer;
import org.springframework.cloud.deployer.admin.server.repository.DeploymentIdRepository;
import org.springframework.cloud.deployer.spi.local.LocalAppDeployer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.SocketUtils;

/**
 * Tests for {@link LocalTestDataFlowServer}.
 *
 * @author Janne Valkealahti
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public class LocalConfigurationTests {

	private static final String APP_DEPLOYER_BEAN_NAME = "appDeployer";

	private ConfigurableApplicationContext context;

	@After
	public void tearDown() {
		context.close();
	}

	@Test
	public void testConfig() {
		SpringApplication app = new SpringApplication(LocalTestDataFlowServer.class);
		int randomPort = SocketUtils.findAvailableTcpPort();
		String dataSourceUrl = String.format("jdbc:h2:tcp://localhost:%s/mem:dataflow", randomPort);
		context = app.run(new String[] { "--server.port=0",
				"--spring.datasource.url=" + dataSourceUrl});
		assertThat(context.containsBean(APP_DEPLOYER_BEAN_NAME), is(true));
		assertThat(context.getBean(APP_DEPLOYER_BEAN_NAME), instanceOf(LocalAppDeployer.class));
		assertNotNull(context.getBean(AppRegistry.class));
	}

	@Test
	public void testNoDataflowConfig() {
		SpringApplication app = new SpringApplication(LocalTestNoDataFlowServer.class);
		context = app.run(new String[] { "--server.port=0" });
		// we still have deployer beans
		assertThat(context.containsBean(APP_DEPLOYER_BEAN_NAME), is(true));
		assertThat(context.containsBean("appRegistry"), is(false));
	}
}
