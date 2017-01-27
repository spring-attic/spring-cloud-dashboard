/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.deployer.admin.rest.client;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of DataFlowOperations delegating to sub-templates, discovered
 * via REST relations.
 *
 *  @author Ilayaperumal Gopinathan
 *  @author Mark Fisher
 *  @author Glenn Renfro
 *  @author Patrick Peralta
 *  @author Gary Russell
 *  @author Eric Bottard
 *  @author Gunnar Hillert
 */
public class DataFlowTemplate implements DataFlowOperations {

	/**
	 * A template used for http interaction.
	 */
	protected final RestTemplate restTemplate;

	/**
	 * Holds discovered URLs of the API.
	 */
	protected final Map<String, UriTemplate> resources = new HashMap<String, UriTemplate>();

	/**
	 * REST client for app registry operations.
	 */
	private final AppRegistryOperations appRegistryOperations;

	/**
	 * REST client for completion operations.
	 */
	private final CompletionOperations completionOperations;

	/**
	 * REST Client for runtime operations.
	 */
	private final RuntimeOperations runtimeOperations;

	/**
	 * Rest client for application operations.
	 */
	private final ApplicationOperations applicationOperations;

	/**
	 * Setup a {@link DataFlowTemplate} using the provided baseURI. Will create a {@link RestTemplate} implicitly with
	 * the required set of Jackson MixIns. For more information, please see {@link #prepareRestTemplate(RestTemplate)}.
	 *
	 * Please be aware that the created RestTemplate will use the JDK's default timeout values. Consider passing in
	 * a custom {@link RestTemplate} or, depending on your JDK implementation, set System properties such as:
	 *
	 * <p><ul>
	 * <li>sun.net.client.defaultConnectTimeout
	 * <li>sun.net.client.defaultReadTimeout
	 * </ul><p>
	 *
	 * For more information see
	 * <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/net/properties.html">this link</a>
	 *
	 * @param baseURI Must not be null
	 */
	public DataFlowTemplate(URI baseURI) {
		this(baseURI, getDefaultDataflowRestTemplate());
	}

	/**
	 * Setup a {@link DataFlowTemplate} using the provide {@link RestTemplate}. Any missing Mixins for Jackson will be
	 * added implicitly. For more information, please see {@link #prepareRestTemplate(RestTemplate)}.
	 *
	 * @param baseURI Must not be null
	 * @param restTemplate Must not be null
	 */
	public DataFlowTemplate(URI baseURI, RestTemplate restTemplate) {

		Assert.notNull(baseURI, "The provided baseURI must not be null.");
		Assert.notNull(restTemplate, "The provided restTemplate must not be null.");

		this.restTemplate = prepareRestTemplate(restTemplate);
		final ResourceSupport resourceSupport = restTemplate.getForObject(baseURI, ResourceSupport.class);
		this.runtimeOperations = new RuntimeTemplate(restTemplate, resourceSupport);
		this.appRegistryOperations = new AppRegistryTemplate(restTemplate, resourceSupport);
		this.completionOperations = new CompletionTemplate(restTemplate,
			resourceSupport.getLink("completions/stream"),
			resourceSupport.getLink("completions/task"));
		if (resourceSupport.hasLink(ApplicationTemplate.DEFINITIONS_REL)) {
			this.applicationOperations = new ApplicationTemplate(restTemplate, resourceSupport);
		}
		else {
			this.applicationOperations = null;
		}
	}

	public Link getLink(ResourceSupport resourceSupport, String rel) {
		Link link = resourceSupport.getLink(rel);
		if (link == null) {
			throw new DataFlowServerException("Server did not return a link for '" + rel + "', links: '"
					+ resourceSupport + "'");
		}
		return link;
	}

	@Override
	public AppRegistryOperations appRegistryOperations() {
		return appRegistryOperations;
	}

	@Override
	public CompletionOperations completionOperations() {
		return completionOperations;
	}

	@Override
	public RuntimeOperations runtimeOperations() {
		return runtimeOperations;
	}

	@Override
	public ApplicationOperations applicationOperations() {
		return applicationOperations;
	}

	/**
	 * Will augment the provided {@link RestTemplate} with the Jackson
	 *
	 * Furthermore, this method will also register the {@link Jackson2HalModule}
	 *
	 * @param restTemplate Can be null. Instantiates a new {@link RestTemplate} if null
	 * @return RestTemplate with the required Jackson Mixins
	 */
	public static RestTemplate prepareRestTemplate(RestTemplate restTemplate) {
		if (restTemplate == null) {
			restTemplate = new RestTemplate();
		}

		restTemplate.setErrorHandler(new VndErrorResponseErrorHandler(restTemplate.getMessageConverters()));

		boolean containsMappingJackson2HttpMessageConverter = false;

		for(HttpMessageConverter<?> converter : restTemplate.getMessageConverters()) {
			if (converter instanceof MappingJackson2HttpMessageConverter) {
				containsMappingJackson2HttpMessageConverter = true;
				final MappingJackson2HttpMessageConverter jacksonConverter = (MappingJackson2HttpMessageConverter) converter;
				jacksonConverter.getObjectMapper()
					.registerModule(new Jackson2HalModule());
			}
		}

		if (!containsMappingJackson2HttpMessageConverter) {
			throw new IllegalArgumentException("The RestTemplate does not contain a required MappingJackson2HttpMessageConverter.");
		}
		return restTemplate;
	}

	/**
	 * Invokes {@link #prepareRestTemplate(RestTemplate)}.
	 *
	 * @return RestTemplate with the required Jackson MixIns applied
	 */
	public static RestTemplate getDefaultDataflowRestTemplate() {
		return prepareRestTemplate(null);
	}

	/**
	 * @return The underlying RestTemplate, will never return null
	 */
	public RestTemplate getRestTemplate() {
		return restTemplate;
	}

}
