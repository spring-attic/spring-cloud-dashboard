/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.deployer.admin.rest.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.StringUtils;

/**
 * Provides utility methods for formatting and parsing deployment properties.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
public final class DeploymentPropertiesUtils {

	private static final Logger logger = LoggerFactory.getLogger(DeploymentPropertiesUtils.class);

	private DeploymentPropertiesUtils() {
		// prevent instantiation
	}

	/**
	 * Pattern used for parsing a String of comma-delimited key=value pairs.
	 */
	private static final Pattern DEPLOYMENT_PROPERTIES_PATTERN = Pattern.compile(",\\s*(app|deployer)\\.[^\\.]+\\.[^=]+=");

	/**
	 * Pattern used for parsing a String of command-line arguments.
	 */
	private static final Pattern DEPLOYMENT_PARAMS_PATTERN = Pattern.compile("(\\s(?=([^\\\"']*[\\\"'][^\\\"']*[\\\"'])*[^\\\"']*$))");

	/**
	 * Parses a String comprised of 0 or more comma-delimited key=value pairs where each key has the format:
	 * {@code app.[appname].[key]} or {@code deployer.[appname].[key]}.
	 * Values may themselves contain commas, since the split points will be based upon the key pattern.
	 *
	 * @param s the string to parse
	 * @return the Map of parsed key value pairs
	 */
	public static Map<String, String> parse(String s) {
		Map<String, String> deploymentProperties = new HashMap<String, String>();
		if (!StringUtils.isEmpty(s)) {
			Matcher matcher = DEPLOYMENT_PROPERTIES_PATTERN.matcher(s);
			int start = 0;
			while (matcher.find()) {
				addKeyValuePairAsProperty(s.substring(start, matcher.start()), deploymentProperties);
				start = matcher.start() + 1;
			}
			addKeyValuePairAsProperty(s.substring(start), deploymentProperties);
		}
		return deploymentProperties;
	}

	/**
	 * Retain only properties that are meant for the <em>deployer</em> of a given app
	 * (those that start with {@code deployer.[appname]} or {@code deployer.*})
	 * and qualify all property values with the {@code spring.cloud.deployer.} prefix.
	 */
	public static Map<String, String> extractAndQualifyDeployerProperties(Map<String, String> input, String appName) {
		final String wildcardPrefix = "deployer.*.";
		final int wildcardLength = wildcardPrefix.length();
		final String appPrefix = String.format("deployer.%s.", appName);
		final int appLength = appPrefix.length();

		// Using a TreeMap makes sure wildcard entries appear before app specific ones
		Map<String, String> result = new TreeMap<>(input).entrySet().stream()
			.filter(kv -> kv.getKey().startsWith(wildcardPrefix) || kv.getKey().startsWith(appPrefix))
			.collect(Collectors.toMap(
				kv -> kv.getKey().startsWith(wildcardPrefix)
					? "spring.cloud.deployer." + kv.getKey().substring(wildcardLength)
					: "spring.cloud.deployer." + kv.getKey().substring(appLength),
				kv -> kv.getValue(),
				(fromWildcard, fromApp) -> fromApp
				)
			);

		Map<String, String> deprecated = extractDeprecatedDeployerProperties(input, appName);
		// Also, 'count' used to be treated as a special case. Handle here
		String deprecatedWildcardCound = input.get("app.*.count");
		String deprecatedCount = input.getOrDefault("app." + appName + ".count", deprecatedWildcardCound);
		if (deprecatedCount != null && deprecated.get("spring.cloud.deployer.count") == null) {
			deprecated.put("spring.cloud.deployer.count", deprecatedCount);
			logger.warn("Usage of application property 'app.{}.count' to specify number of instances has been deprecated and will be removed in a future release\n" +
				"Instead, please use 'deployer.{}.count = {}'",
				appName, appName, deprecatedCount);
		}


		if (deprecated.isEmpty()) {
			return result;
		} else {
			deprecated.entrySet().forEach(kv -> {
				logger.warn("Usage of application property prefix 'spring.cloud.deployer' to pass properties to the deployer has been deprecated and will be removed in a future release\n" +
					"Instead of 'app.{}.{} = {}', please use\n" +
					"           'deployer.{}.{} = {}'",
					appName, kv.getKey(), kv.getValue(),
					appName, kv.getKey().substring("spring.cloud.deployer.".length()), kv.getValue());
			});
			if (result.isEmpty()) {
				return deprecated;
			} else {
				return result;
			}
		}

	}

	private static Map<String, String> extractDeprecatedDeployerProperties(Map<String, String> input, String appName) {
		final String wildcardPrefix = "app.*.spring.cloud.deployer.";
		final int wildcardLength = "app.*.".length();
		final String appPrefix = String.format("app.%s.spring.cloud.deployer.", appName);
		final int appLength = String.format("app.%s.", appName).length();

		return new TreeMap<>(input).entrySet().stream()
			.filter(kv -> kv.getKey().startsWith(wildcardPrefix) || kv.getKey().startsWith(appPrefix))
			.collect(Collectors.toMap(
				kv -> kv.getKey().startsWith(wildcardPrefix)
					? kv.getKey().substring(wildcardLength)
					: kv.getKey().substring(appLength),
				kv -> kv.getValue(),
				(fromWildcard, fromApp) -> fromApp
				)
			);
	}

	/**
	 * Returns a String representation of deployment properties as a comma separated list of key=value pairs.
	 *
	 * @param properties the properties to format
	 * @return the properties formatted as a String
	 */
	public static String format(Map<String, String> properties) {
		StringBuilder sb = new StringBuilder(15 * properties.size());
		for (Map.Entry<String, String> pair : properties.entrySet()) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			sb.append(pair.getKey()).append("=").append(pair.getValue());
		}
		return sb.toString();
	}

	/**
	 * Convert Properties to a Map with String keys and values. Entries whose key or value is not a String are omitted.
	 */
	public static Map<String, String> convert(Properties properties) {
		Map<String, String> result  = new HashMap<>(properties.size());
		for (String key : properties.stringPropertyNames()) {
			result.put(key, properties.getProperty(key));
		}
		return result;
	}

	/**
	 * Adds a String of format key=value to the provided Map as a key/value pair.
	 *
	 * @param pair the String representation
	 * @param properties the Map to which the key/value pair should be added
	 */
	private static void addKeyValuePairAsProperty(String pair, Map<String, String> properties) {
		int firstEquals = pair.indexOf('=');
		if (firstEquals != -1) {
			// todo: should key only be a "flag" as in: put(key, true)?
			properties.put(pair.substring(0, firstEquals).trim(), pair.substring(firstEquals + 1).trim());
		}
	}

	/**
	 * Parses a list of command line parameters and returns a list of parameters
	 * which doesn't contain any special quoting either for values or whole parameter.
	 *
	 * @param params the params
	 * @return the list
	 */
	public static List<String> parseParams(List<String> params) {
		List<String> paramsToUse = new ArrayList<>();
		if (params != null) {
			for (String param : params) {
				Matcher regexMatcher = DEPLOYMENT_PARAMS_PATTERN.matcher(param);
				int start = 0;
				while (regexMatcher.find()) {
					String p = removeQuoting(param.substring(start, regexMatcher.start()).trim());
					if (StringUtils.hasText(p)) {
						paramsToUse.add(p);
					}
					start = regexMatcher.start();
				}
				if (param != null && param.length() > 0) {
					String p = removeQuoting(param.substring(start, param.length()).trim());
					if (StringUtils.hasText(p)) {
						paramsToUse.add(p);
					}
				}
			}
		}
		return paramsToUse;
	}

	private static String removeQuoting(String param) {
		param = removeQuote(param, '\'');
		param = removeQuote(param, '"');
		if (StringUtils.hasText(param)) {
			String[] split = param.split("=", 2);
			if (split.length == 2) {
				String value = removeQuote(split[1], '\'');
				value = removeQuote(value, '"');
				param = split[0] + "=" + value;
			}
		}
		return param;
	}

	private static String removeQuote(String param, char c) {
		if (param != null && param.length() > 1) {
			if (param.charAt(0) == c && param.charAt(param.length()-1) == c) {
				param = param.substring(1, param.length()-1);
			}
		}
		return param;
	}
}
