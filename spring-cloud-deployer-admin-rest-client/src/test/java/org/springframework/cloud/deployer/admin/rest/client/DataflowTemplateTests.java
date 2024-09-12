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
package org.springframework.cloud.deployer.admin.rest.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.deployer.admin.rest.client.DataFlowTemplate;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Gunnar Hillert
 *
 */
public class DataflowTemplateTests {

	@Before
	public void setup() {
		System.setProperty("sun.net.client.defaultConnectTimeout", String.valueOf(100));
	}

	@After
	public void shutdown() {
		System.clearProperty("sun.net.client.defaultConnectTimeout");
	}

	@Test
	public void testDataFlowTemplateContructorWithNullUri() throws URISyntaxException {

		try {
			new DataFlowTemplate(null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("The provided baseURI must not be null.", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test(expected = ResourceAccessException.class)
	public void testDataFlowTemplateContructorWithNonExistingUri() throws URISyntaxException {
			new DataFlowTemplate(new URI("https://doesnotexist:1234"));
	}

	@Test
	public void testPrepareRestTemplateWithRestTemplateThatHasNoMessageConverters() {
		final RestTemplate providedRestTemplate = new RestTemplate();
		providedRestTemplate.getMessageConverters().clear();

		try {
			DataFlowTemplate.prepareRestTemplate(providedRestTemplate);
		}
		catch(IllegalArgumentException e) {
			assertEquals("'messageConverters' must not be empty", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testPrepareRestTemplateWithRestTemplateThatMissesJacksonConverter() {
		final RestTemplate providedRestTemplate = new RestTemplate();
		final Iterator<HttpMessageConverter<?>> iterator = providedRestTemplate.getMessageConverters().iterator();

		while(iterator.hasNext()){
			if(iterator.next() instanceof MappingJackson2HttpMessageConverter) {
				iterator.remove();
			}
		}

		try {
			DataFlowTemplate.prepareRestTemplate(providedRestTemplate);
		}
		catch(IllegalArgumentException e) {
			assertEquals("The RestTemplate does not contain a required MappingJackson2HttpMessageConverter.", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}
}
