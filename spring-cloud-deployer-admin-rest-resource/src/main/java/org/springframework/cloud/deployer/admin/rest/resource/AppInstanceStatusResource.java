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

package org.springframework.cloud.deployer.admin.rest.resource;

import java.util.Map;

import org.springframework.hateoas.ResourceSupport;

/**
 * REST representation for an AppInstanceStatus.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
public class AppInstanceStatusResource extends ResourceSupport {

	private String instanceId;

	private String state;

	private Map<String, String> attributes;

	private AppInstanceStatusResource() {
		// noarg constructor for serialization
	}

	public AppInstanceStatusResource(String instanceId, String state, Map<String, String> attributes) {
		this.instanceId = instanceId;
		this.state = state;
		this.attributes = attributes;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public String getState() {
		return state;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public void setState(String state) {
		this.state = state;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}
}
