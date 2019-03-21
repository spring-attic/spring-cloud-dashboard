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

package org.springframework.cloud.deployer.admin.completion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.deployer.admin.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.deployer.admin.configuration.metadata.ApplicationConfigurationMetadataResolverAutoConfiguration;
import org.springframework.cloud.deployer.admin.registry.AppRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Include this Configuration class to expose fully configured {@link StreamCompletionProvider} and
 * {@link TaskCompletionProvider}.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
@Configuration
@Import({ApplicationConfigurationMetadataResolverAutoConfiguration.class})
public class CompletionConfiguration {

	@Autowired
	private AppRegistry appRegistry;

	@Autowired
	private ApplicationConfigurationMetadataResolver metadataResolver;

	@Bean
	public ValueHintProvider defaultValueHintProvider() {
		return new DefaultValueHintProvider();
	}

	@Bean
	public ValueHintProvider enumValueHintProvider() {
		return new EnumValueHintProvider();
	}

	@Bean
	public ValueHintProvider booleanValueHintProvider() {
		return new BooleanValueHintProvider();
	}
}
