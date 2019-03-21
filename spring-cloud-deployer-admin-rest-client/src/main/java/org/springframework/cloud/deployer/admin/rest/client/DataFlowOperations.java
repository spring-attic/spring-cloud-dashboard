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

package org.springframework.cloud.deployer.admin.rest.client;

/**
 * Interface the REST clients implement to interact with spring-cloud-dataflow REST API.
 *
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Janne Valkealahti
 */
public interface DataFlowOperations {

	/**
	 * Application registry related operations.
	 */
	AppRegistryOperations appRegistryOperations();

	/**
	 * DSL Completion related operations.
	 */
	CompletionOperations completionOperations();

	/**
	 * Runtime related opertations.
	 */
	RuntimeOperations runtimeOperations();

	/**
	 * Application related operations.
	 */
	ApplicationOperations applicationOperations();
}
