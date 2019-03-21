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

package org.springframework.cloud.deployer.admin.shell.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.deployer.admin.rest.client.AppRegistryOperations;
import org.springframework.cloud.deployer.admin.rest.client.DataFlowOperations;
import org.springframework.cloud.deployer.admin.rest.resource.AppRegistrationResource;
import org.springframework.cloud.deployer.admin.shell.command.AppRegistryCommands;
import org.springframework.cloud.deployer.admin.shell.config.DataFlowShell;
import org.springframework.hateoas.PagedResources;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableModel;

/**
 * Unit tests for {@link AppRegistryCommands}.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
public class AppRegistryCommandsTests {

	AppRegistryCommands appRegistryCommands = new AppRegistryCommands();

	@Mock
	private DataFlowOperations dataFlowOperations;

	@Mock
	private AppRegistryOperations appRegistryOperations;

	private DataFlowShell dataFlowShell = new DataFlowShell();

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		when(dataFlowOperations.appRegistryOperations()).thenReturn(appRegistryOperations);
		dataFlowShell.setDataFlowOperations(dataFlowOperations);
		appRegistryCommands.setDataFlowShell(dataFlowShell);
	}

	@Test
	public void testHintOnEmptyList() {
		Collection<AppRegistrationResource> data = new ArrayList<>();
		PagedResources.PageMetadata metadata = new PagedResources.PageMetadata(data.size(), 1, data.size(), 1);
		PagedResources<AppRegistrationResource> result = new PagedResources<>(data, metadata);
		when(appRegistryOperations.list()).thenReturn(result);

		Object commandResult = appRegistryCommands.list();
		assertThat((String) commandResult, CoreMatchers.containsString("app register"));
		assertThat((String) commandResult, CoreMatchers.containsString("app import"));
	}

	@Test
	public void testList() {

		String[][] apps = new String[][] {
				{"http", "source"},
				{"filter", "processor"},
				{"transform", "processor"},
				{"file", "source"},
				{"log", "sink"},
				{"moving-average", "processor"}
		};

		Collection<AppRegistrationResource> data = new ArrayList<>();
		for (String[] app : apps) {
			data.add(new AppRegistrationResource(app[0], app[1], null));
		}
		PagedResources.PageMetadata metadata = new PagedResources.PageMetadata(data.size(), 1, data.size(), 1);
		PagedResources<AppRegistrationResource> result = new PagedResources<>(data, metadata);
		when(appRegistryOperations.list()).thenReturn(result);

		Object[][] expected = new String[][] {
				{ "source", "processor", "sink" },
				{ "http", "filter", "log" },
				{ "file", "transform", null },
				{ null, "moving-average", null }
		};
		TableModel model = ((Table) appRegistryCommands.list()).getModel();
		for (int row = 0; row < expected.length; row++) {
			for (int col = 0; col < expected[row].length; col++) {
				assertThat(model.getValue(row, col), Matchers.is(expected[row][col]));
			}
		}
	}

	@Test
	public void testUnknownModule() {
		List<Object> result = appRegistryCommands.info(new AppRegistryCommands.QualifiedApplicationName("unknown", "processor"));
		assertEquals((String) result.get(0), "Application info is not available for processor:unknown");
	}

	@Test
	public void register() {
		String name = "foo";
		String type = "sink";
		String uri = "file:///foo";
		boolean force = false;
		AppRegistrationResource resource = new AppRegistrationResource(name, type, uri);
		when(appRegistryOperations.register(name, type, uri, force)).thenReturn(resource);
		String result = appRegistryCommands.register(name, type, uri, force);
		assertEquals("Successfully registered application 'sink:foo'", result);
	}

	@Test
	public void importFromLocalResource() {
		String name1 = "foo";
		String type1 = "source";
		String uri1 = "file:///foo";
		String name2 = "bar";
		String type2 = "sink";
		String uri2 = "file:///bar";
		Properties apps = new Properties();
		apps.setProperty(type1 + "." + name1, uri1);
		apps.setProperty(type2 + "." + name2, uri2);
		List<AppRegistrationResource> resources = new ArrayList<>();
		resources.add(new AppRegistrationResource(name1, type1, uri1));
		resources.add(new AppRegistrationResource(name2, type2, uri2));
		PagedResources<AppRegistrationResource> pagedResources = new PagedResources<>(resources,
				new PagedResources.PageMetadata(resources.size(), 1, resources.size(), 1));
		when(appRegistryOperations.registerAll(apps, true)).thenReturn(pagedResources);
		String appsFileUri = "classpath:appRegistryCommandsTests-apps.properties";
		String result = appRegistryCommands.importFromResource(appsFileUri, true, true);
		assertEquals("Successfully registered applications: [source.foo, sink.bar]", result);
	}

	@Test
	public void importFromResource() {
		List<AppRegistrationResource> resources = new ArrayList<>();
		resources.add(new AppRegistrationResource("foo", "source", null));
		resources.add(new AppRegistrationResource("bar", "sink", null));
		PagedResources<AppRegistrationResource> pagedResources = new PagedResources<>(resources,
				new PagedResources.PageMetadata(resources.size(), 1, resources.size(), 1));
		String uri = "test://example";
		when(appRegistryOperations.importFromResource(uri, true)).thenReturn(pagedResources);
		String result = appRegistryCommands.importFromResource(uri, false, true);
		assertEquals("Successfully registered 2 applications from 'test://example'", result);
	}
}
