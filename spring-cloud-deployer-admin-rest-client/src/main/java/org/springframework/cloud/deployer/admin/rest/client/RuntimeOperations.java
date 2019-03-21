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

package org.springframework.cloud.deployer.admin.rest.client;

import org.springframework.cloud.deployer.admin.rest.resource.AppStatusResource;
import org.springframework.hateoas.PagedResources;

/**
 * Defines operations available for obtaining information about deployed apps.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
public interface RuntimeOperations {

	/**
	 * Return runtime information about all deployed apps.
	 */
	PagedResources<AppStatusResource> status();

	/**
	 * Return runtime information about a single app deployment.
	 */
	AppStatusResource status(String deploymentId);
}
