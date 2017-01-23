/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.repository;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;

/**
 * @author Janne Valkealahti
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public class InMemoryDeploymentIdRepositoryTests {

	@Test
	public void testSimpleSaveFind() {
		StreamDefinition streamDefinition1 = new StreamDefinition("myStream1", "time | log");
		StreamAppDefinition[] appDefinitions1 = streamDefinition1.getAppDefinitions().toArray(new StreamAppDefinition[0]);
		StreamDefinition streamDefinition2 = new StreamDefinition("myStream1", "time | log");
		StreamAppDefinition[] appDefinitions2 = streamDefinition2.getAppDefinitions().toArray(new StreamAppDefinition[0]);
		String appDeploymentKey1 = DeploymentKey.forStreamAppDefinition(appDefinitions1[0]);
		String appDeploymentKey2 = DeploymentKey.forStreamAppDefinition(appDefinitions1[1]);
		String appDeploymentKey3 = DeploymentKey.forStreamAppDefinition(appDefinitions2[0]);
		String appDeploymentKey4 = DeploymentKey.forStreamAppDefinition(appDefinitions2[1]);

		DeploymentIdRepository repository = new InMemoryDeploymentIdRepository();
		repository.save(appDeploymentKey1, "id1");
		repository.save(appDeploymentKey2, "id2");

		String findOne1 = repository.findOne(appDeploymentKey1);
		assertThat(findOne1, notNullValue());
		assertThat(findOne1, is("id1"));
		String findOne2 = repository.findOne(appDeploymentKey3);
		assertThat(findOne2, notNullValue());
		assertThat(findOne2, is("id1"));

		String findOne3 = repository.findOne(appDeploymentKey2);
		assertThat(findOne3, notNullValue());
		assertThat(findOne3, is("id2"));
		String findOne4 = repository.findOne(appDeploymentKey4);
		assertThat(findOne4, notNullValue());
		assertThat(findOne4, is("id2"));
	}

	@Test
	public void testDeleteKey() {
		StreamDefinition streamDefinition1 = new StreamDefinition("myStream1", "time | log");
		StreamAppDefinition[] appDefinitions1 = streamDefinition1.getAppDefinitions().toArray(new StreamAppDefinition[0]);
		String appDeploymentKey1 = DeploymentKey.forStreamAppDefinition(appDefinitions1[0]);
		String appDeploymentKey2 = DeploymentKey.forStreamAppDefinition(appDefinitions1[1]);

		DeploymentIdRepository repository = new InMemoryDeploymentIdRepository();
		repository.save(appDeploymentKey1, "id1");
		repository.save(appDeploymentKey2, "id2");
		repository.delete(appDeploymentKey1);
		assertThat(repository.findOne(appDeploymentKey1), nullValue());
		assertThat(repository.findOne(appDeploymentKey2), notNullValue());
	}
}
