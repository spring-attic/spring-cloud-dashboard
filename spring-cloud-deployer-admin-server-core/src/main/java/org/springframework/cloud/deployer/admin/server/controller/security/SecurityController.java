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

package org.springframework.cloud.deployer.admin.server.controller.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.admin.rest.resource.security.SecurityInfoResource;
import org.springframework.core.env.Environment;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides security-related meta information. Provides one REST endpoint at present
 * time {@code /security/info} that provides information such as whether security
 * is enabled and if so what is the username of the currently logged in user etc.
 *
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @since 1.0
 */
@RestController
@RequestMapping("/security/info")
@ExposesResourceFor(SecurityInfoResource.class)
public class SecurityController {

	private final SecurityProperties securityProperties;

	public SecurityController(SecurityProperties securityProperties) {
		this.securityProperties = securityProperties;
	}

	/**
	 * Return security information. E.g. is security enabled? Which user do you represent?
	 */
	@ResponseBody
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public SecurityInfoResource getSecurityInfo() {

		final boolean authenticationEnabled = securityProperties.getBasic().isEnabled();

		final SecurityInfoResource securityInfo = new SecurityInfoResource();
		securityInfo.setAuthenticationEnabled(authenticationEnabled);
		securityInfo.add(ControllerLinkBuilder.linkTo(SecurityController.class).withSelfRel());

		if (authenticationEnabled && SecurityContextHolder.getContext() != null) {
			final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (!(authentication instanceof AnonymousAuthenticationToken)) {
				securityInfo.setAuthenticated(authentication.isAuthenticated());
				securityInfo.setUsername(authentication.getName());
			}
		}

		return securityInfo;
	}

}