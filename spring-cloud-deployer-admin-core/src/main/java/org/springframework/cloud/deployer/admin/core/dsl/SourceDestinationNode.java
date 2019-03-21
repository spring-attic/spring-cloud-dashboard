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

package org.springframework.cloud.deployer.admin.core.dsl;

/**
 * @author Andy Clement
 */
public class SourceDestinationNode extends AstNode {

	private final DestinationNode destinationNode;

	public SourceDestinationNode(DestinationNode destinationNode, int endPos) {
		super(destinationNode.startPos, endPos);
		this.destinationNode = destinationNode;
	}

	/** @inheritDoc */
	@Override
	public String stringify(boolean includePositionalInfo) {
		return destinationNode.stringify(includePositionalInfo) + ">";
	}

	@Override
	public String toString() {
		return destinationNode.toString() + " > ";
	}

	public DestinationNode getDestinationNode() {
		return destinationNode;
	}

	public SourceDestinationNode copyOf() {
		return new SourceDestinationNode(destinationNode.copyOf(), super.endPos);
	}

	public String getDestinationName() {
		return destinationNode.getDestinationName();
	}

	public ArgumentNode[] getArguments() {
		return this.getDestinationNode().getArguments();
	}

}
