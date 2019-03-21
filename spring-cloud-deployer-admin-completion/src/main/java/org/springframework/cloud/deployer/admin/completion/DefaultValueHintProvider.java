/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.deployer.admin.completion;

import java.util.List;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ValueHint;
import org.springframework.boot.configurationmetadata.ValueProvider;

/**
 * A default {@link ValueHintProvider} that returns hints explicitly
 * defined by a property.
 *
 * @author Eric Bottard
 */
public class DefaultValueHintProvider implements ValueHintProvider {

	@Override
	public List<ValueHint> generateValueHints(ConfigurationMetadataProperty property, ClassLoader classLoader) {
		return property.getValueHints();
	}

	@Override
	public boolean isExclusive(ConfigurationMetadataProperty property) {
		for (ValueProvider valueProvider : property.getValueProviders()) {
			if ("any".equals(valueProvider.getName())) {
				return false;
			}
		}
		return true;
	}
}
