/*
 * eID Identity Provider Project.
 * Copyright (C) 2010 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see
 * http://www.gnu.org/licenses/.
 */

package be.fedict.eid.idp.sp;

import be.fedict.eid.idp.common.AttributeConstants;
import be.fedict.eid.idp.sp.protocol.openid.OpenIDAuthenticationResponse;
import be.fedict.eid.idp.sp.protocol.saml2.spi.AuthenticationResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Map;

public class ResponseBean {

        private static final Log LOG = LogFactory.getLog(ResponseBean.class);

        private HttpSession session;
        private String identifier;
        private Map<String, Object> attributeMap;
        private String policy;

        public HttpSession getSession() {
                return this.session;
        }

        @SuppressWarnings("unchecked")
        public void setSession(HttpSession session) {

                this.session = session;

                if (null != session.getAttribute("Response")) {

                        Object responseObject = session.getAttribute("Response");
                        if (responseObject instanceof AuthenticationResponse) {
                                // saml2
                                AuthenticationResponse response =
                                        (AuthenticationResponse) responseObject;
                                this.identifier = response.getIdentifier();
                                this.attributeMap = response.getAttributeMap();
                                this.policy = response.getAuthenticationPolicy().getUri();
                                LOG.debug("SAML2 assertion: " + response.getAssertion());
                        } else {
                                // openid
                                OpenIDAuthenticationResponse response =
                                        (OpenIDAuthenticationResponse) responseObject;
                                this.identifier = response.getIdentifier();
                                this.attributeMap = response.getAttributeMap();
                                this.policy = Arrays.toString(response.getAuthenticationPolicies().toArray());
                        }

                        for (Map.Entry<String, Object> entry : this.attributeMap.entrySet()) {
                                LOG.debug("attribute: " + entry.getKey() + " value=" + entry.getValue());
                        }

                        // get photo
                        if (this.attributeMap.containsKey(AttributeConstants.PHOTO_CLAIM_TYPE_URI)) {
                                byte[] photoData = (byte[]) this.attributeMap
                                        .get(AttributeConstants.PHOTO_CLAIM_TYPE_URI);
                                this.session.setAttribute(PhotoServlet.PHOTO_SESSION_ATTRIBUTE,
                                        photoData);
                        } else {
                                this.session.removeAttribute(PhotoServlet.PHOTO_SESSION_ATTRIBUTE);
                        }
                }
                cleanupSession();


        }

        public Map getAttributeMap() {

                return this.attributeMap;
        }

        public void setAttributeMap(Map value) {
                // empty
        }

        public String getIdentifier() {
                return this.identifier;
        }

        public void setIdentifier(String identifier) {
                // empty
        }

        private void cleanupSession() {
                this.session.removeAttribute("Identifier");
                this.session.removeAttribute("AttributeMap");
                this.session.removeAttribute("Response");
        }

        public String getPolicy() {
                return this.policy;
        }

        public void setPolicy(String policy) {
                // empty
        }
}