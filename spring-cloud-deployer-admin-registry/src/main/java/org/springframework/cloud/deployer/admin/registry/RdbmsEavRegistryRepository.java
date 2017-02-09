/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.deployer.admin.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

public class RdbmsEavRegistryRepository implements EavRegistryRepository {

	private static final String FIND_ALL_QUERY =
			"SELECT era.NAME, erav.VALUE FROM EAV_REGISTRY_ATTRIBUTE_VALUES erav " +
			"INNER JOIN EAV_REGISTRY_ATTRIBUTES era " +
			"ON erav.ATTRIBUTE_ID=era.ID " +
			"AND erav.NAMESPACE=?";

	private static final String FIND_ONE_QUERY =
			"SELECT erav.VALUE FROM EAV_REGISTRY_ATTRIBUTE_VALUES erav " +
			"INNER JOIN EAV_REGISTRY_ATTRIBUTES era " +
			"ON erav.ATTRIBUTE_ID=era.ID " +
			"AND erav.NAMESPACE=? " +
			"AND era.NAME=?";

	private static final String ADD_ATTRIBUTES_INSERT =
			"INSERT INTO EAV_REGISTRY_ATTRIBUTES (NAME) VALUES (?)";

	private static final String ADD_ATTRIBUTE_INSERT =
			"INSERT INTO EAV_REGISTRY_ATTRIBUTE_VALUES (ATTRIBUTE_ID, NAMESPACE, VALUE) VALUES (?, ?, ?)";

	private static final String GET_ATTRIBUTE_ID_SELECT =
			"SELECT ID FROM EAV_REGISTRY_ATTRIBUTES where NAME=?";

	private final Map<String, Integer> attributeNameCache = new HashMap<>();

	private final JdbcTemplate jdbcTemplate;

	public RdbmsEavRegistryRepository(DataSource dataSource) {
		Assert.notNull(dataSource, "DataSource must not be null");
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public void save(String namespace, String attribute, String value) {
		int attributeId = getAttributeId(attribute);
		jdbcTemplate.update(ADD_ATTRIBUTE_INSERT, new Object[] { attributeId, namespace, value });
	}

	@Override
	public String findOne(String namespace, String attribute) {
		return jdbcTemplate.queryForObject(FIND_ONE_QUERY, new Object[] { namespace, attribute }, String.class);
	}

	@Override
	public Map<String, String> findAll(String namespace) {
		Map<String, String> map = new HashMap<>();
		Map<String, Object> queryForMap = jdbcTemplate.queryForMap(FIND_ALL_QUERY,  new Object[] {namespace});
		for (Entry<String, Object> e : queryForMap.entrySet()) {
			map.put(e.getKey(), e.getValue().toString());
		}
		return map;
	}

	private int getAttributeId(String attribute) {
		Integer id = attributeNameCache.get(attribute);
		if (id == null) {
			try {
				id = jdbcTemplate.queryForObject(GET_ATTRIBUTE_ID_SELECT, new Object[] { attribute }, int.class);
			} catch (EmptyResultDataAccessException e) {
			}
			if (id == null) {
				jdbcTemplate.update(ADD_ATTRIBUTES_INSERT, new Object[] { attribute });
				id = jdbcTemplate.queryForObject(GET_ATTRIBUTE_ID_SELECT, new Object[] { attribute }, int.class);
				attributeNameCache.put(attribute, id);
			}
		}
		return id;
	}

}
