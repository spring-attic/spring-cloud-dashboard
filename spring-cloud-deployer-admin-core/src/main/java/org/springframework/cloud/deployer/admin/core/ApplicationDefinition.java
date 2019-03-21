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

package org.springframework.cloud.deployer.admin.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.deployer.admin.core.dsl.AppNode;
import org.springframework.cloud.deployer.admin.core.dsl.ApplicationParser;
import org.springframework.cloud.deployer.admin.core.dsl.ArgumentNode;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.core.style.ToStringCreator;

public class ApplicationDefinition extends DataFlowAppDefinition {

	/**
	 * Name of application.
	 */
	private final String name;

	/**
	 * DSL definition for application.
	 */
	private final String dslText;

	public ApplicationDefinition(String name, String dslText) {
		super();
		this.name = name;
		this.dslText = dslText;
		// ApplicationParser
		AppNode applicationNode = new ApplicationParser(name, dslText).parse();
		Map<String, String> properties = new HashMap<>();
		if (applicationNode.hasArguments()) {
			for (ArgumentNode argumentNode : applicationNode.getArguments()) {
				properties.put(argumentNode.getName(), argumentNode.getValue());
			}
		}
		setRegisteredAppName(applicationNode.getName());
		this.appDefinition = new AppDefinition(name, properties);
	}

	public String getDslText() {
		return dslText;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dslText == null) ? 0 : dslText.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ApplicationDefinition other = (ApplicationDefinition) obj;
		if (dslText == null) {
			if (other.dslText != null)
				return false;
		} else if (!dslText.equals(other.dslText))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this)
				.append("name", this.name)
				.append("definition", this.dslText)
				.toString();
	}
}
