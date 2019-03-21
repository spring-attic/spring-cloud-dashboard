/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.deployer.admin.server.repository.support;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.cloud.deployer.admin.server.repository.support.SearchPageable;
import org.springframework.data.domain.PageRequest;

/**
 * @author Gunnar Hillert
 */
public class SearchPageableTests {

	@Test
	public void initializeSearchPageableWithNullPageable() throws Exception{
		try {
			new SearchPageable(null, null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("pageable must not be null", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void initializeSearchPageableWithNullSearchQuery() throws Exception{
		final PageRequest pageable = new PageRequest(1, 5);
		try {
			new SearchPageable(pageable, null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("searchQuery must not be empty", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void initializeSearchPageableWithEmptySearchQuery() throws Exception{
		final PageRequest pageable = new PageRequest(1, 5);
		try {
			new SearchPageable(pageable, "  ");
		}
		catch (IllegalArgumentException e) {
			assertEquals("searchQuery must not be empty", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void addNullCollumn() throws Exception{
		final PageRequest pageable = new PageRequest(1, 5);
		final SearchPageable searchPageable = new SearchPageable(pageable, "search query");

		try {
			searchPageable.addColumns(new String[]{});
		}
		catch (IllegalArgumentException e) {
			assertEquals("You must specify at least 1 column.", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void addNullCollumn2() throws Exception{
		final PageRequest pageable = new PageRequest(1, 5);
		final SearchPageable searchPageable = new SearchPageable(pageable, "search query");

		try {
			searchPageable.addColumns("c1", null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("Column names cannot be null or empty.", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void addWhitespaceCollumn() throws Exception{
		final PageRequest pageable = new PageRequest(1, 5);
		final SearchPageable searchPageable = new SearchPageable(pageable, "search query");

		try {
			searchPageable.addColumns("     ");
		}
		catch (IllegalArgumentException e) {
			assertEquals("Column names cannot be null or empty.", e.getMessage());
			return;
		}

		fail("Expected an IllegalArgumentException to be thrown.");
	}

	@Test
	public void testSearchPageableGetters() throws Exception{
		final PageRequest pageable = new PageRequest(1, 5);
		final SearchPageable searchPageable = new SearchPageable(pageable, "search query");

		assertThat(searchPageable.getColumns(), is(empty()));
		assertNotNull(searchPageable.getPageable());
		assertEquals(searchPageable.getSearchQuery(), "search query");

		searchPageable.addColumns("c1", "c2");

		assertThat(searchPageable.getColumns(), hasSize(2));
		assertThat(searchPageable.getColumns(), contains("c1", "c2"));

	}
}
