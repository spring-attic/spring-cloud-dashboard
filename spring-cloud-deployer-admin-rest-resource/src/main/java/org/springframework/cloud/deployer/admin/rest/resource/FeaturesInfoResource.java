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

package org.springframework.cloud.deployer.admin.rest.resource;

import org.springframework.hateoas.ResourceSupport;

/**
 * Provides features information.
 *
 * @author Ilayaperumal Gopinathan
 */
public class FeaturesInfoResource extends ResourceSupport {
	/**
	 * Default constructor for serialization frameworks.
	 */
	public FeaturesInfoResource() {
	}

	private boolean analyticsEnabled = true;

	private boolean streamsEnabled = true;

	public boolean isAnalyticsEnabled() {
		return this.analyticsEnabled;
	}

	public void setAnalyticsEnabled(boolean analyticsEnabled) {
		this.analyticsEnabled = analyticsEnabled;
	}

	public boolean isStreamsEnabled() {
		return this.streamsEnabled;
	}

	public void setStreamsEnabled(boolean streamsEnabled) {
		this.streamsEnabled = streamsEnabled;
	}

}
