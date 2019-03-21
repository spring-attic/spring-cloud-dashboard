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

package org.springframework.cloud.deployer.admin.rest.resource;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.hateoas.ResourceSupport;

/**
 * Represents a list of completion proposals for the DSL when using <i>e.g.</i> TAB completion in the Shell.
 *
 * @author Eric Bottard
 */
public class CompletionProposalsResource extends ResourceSupport {

	private List<Proposal> proposals = new ArrayList<>();

	public void addProposal(String text, String explanation) {
		proposals.add(new Proposal(text, explanation));
	}

	public List<Proposal> getProposals() {
		return proposals;
	}

	/**
	 * Represents a completion proposal for the DSL when using <i>e.g.</i> TAB completion in the Shell.
	 *
	 * @author Eric Bottard
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Proposal {

		private String text;

		private String explanation;

		private Proposal() {
			// No-arg constructor for Json serialization purposes
		}

		public Proposal(String text, String explanation) {
			this.text = text;
			this.explanation = explanation;
		}

		public String getText() {
			return text;
		}

		public String getExplanation() {
			return explanation;
		}
	}
}
