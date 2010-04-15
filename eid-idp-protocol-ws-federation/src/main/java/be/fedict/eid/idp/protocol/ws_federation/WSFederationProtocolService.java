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

package be.fedict.eid.idp.protocol.ws_federation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignContext;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import oasis.names.tc.saml._2_0.assertion.AssertionType;
import oasis.names.tc.saml._2_0.assertion.AttributeStatementType;
import oasis.names.tc.saml._2_0.assertion.AttributeType;
import oasis.names.tc.saml._2_0.assertion.AudienceRestrictionType;
import oasis.names.tc.saml._2_0.assertion.ConditionAbstractType;
import oasis.names.tc.saml._2_0.assertion.ConditionsType;
import oasis.names.tc.saml._2_0.assertion.NameIDType;
import oasis.names.tc.saml._2_0.assertion.SubjectConfirmationType;
import oasis.names.tc.saml._2_0.assertion.SubjectType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.utils.Constants;
import org.apache.xpath.XPathAPI;
import org.joda.time.DateTime;
import org.oasis_open.docs.ws_sx.ws_trust._200512.ObjectFactory;
import org.oasis_open.docs.ws_sx.ws_trust._200512.RequestSecurityTokenResponseCollectionType;
import org.oasis_open.docs.ws_sx.ws_trust._200512.RequestSecurityTokenResponseType;
import org.oasis_open.docs.ws_sx.ws_trust._200512.RequestedSecurityTokenType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import be.fedict.eid.applet.service.Address;
import be.fedict.eid.applet.service.Identity;
import be.fedict.eid.idp.spi.IdentityProviderConfiguration;
import be.fedict.eid.idp.spi.IdentityProviderFlow;
import be.fedict.eid.idp.spi.IdentityProviderProtocolService;
import be.fedict.eid.idp.spi.ReturnResponse;

/**
 * WS-Federation Web (Passive) Requestors. We could use OpenAM (OpenSS0), but
 * then again they're also just doing a wrapping around the JAXB classes.
 * 
 * @author Frank Cornelis
 * 
 */
public class WSFederationProtocolService implements
		IdentityProviderProtocolService {

	private static final Log LOG = LogFactory
			.getLog(WSFederationProtocolService.class);

	public static final String WCTX_SESSION_ATTRIBUTE = WSFederationProtocolService.class
			.getName()
			+ ".wctx";

	public static final String WTREALM_SESSION_ATTRIBUTE = WSFederationProtocolService.class
			.getName()
			+ ".wtrealm";

	private IdentityProviderConfiguration configuration;

	private void storeWCtx(String wctx, HttpServletRequest request) {
		HttpSession httpSession = request.getSession();
		httpSession.setAttribute(WCTX_SESSION_ATTRIBUTE, wctx);
	}

	private String retrieveWctx(HttpSession httpSession) {
		String wctx = (String) httpSession.getAttribute(WCTX_SESSION_ATTRIBUTE);
		return wctx;
	}

	private void storeWtrealm(String wtrealm, HttpServletRequest request) {
		HttpSession httpSession = request.getSession();
		httpSession.setAttribute(WTREALM_SESSION_ATTRIBUTE, wtrealm);
	}

	private String retrieveWtrealm(HttpSession httpSession) {
		String wtrealm = (String) httpSession
				.getAttribute(WTREALM_SESSION_ATTRIBUTE);
		return wtrealm;
	}

	@Override
	public IdentityProviderFlow handleIncomingRequest(
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		LOG.debug("handleIncomingRequest");
		String wa = request.getParameter("wa");
		if (null == wa) {
			throw new ServletException("wa parameter missing");
		}
		if (false == "wsignin1.0".equals(wa)) {
			throw new ServletException("wa action not \"wsignin1.0\"");
		}
		String wtrealm = request.getParameter("wtrealm");
		if (null == wtrealm) {
			throw new ServletException("missing wtrealm parameter");
		}
		LOG.debug("wtrealm: " + wtrealm);
		storeWtrealm(wtrealm, request);
		String wctx = request.getParameter("wctx");
		LOG.debug("wctx: " + wctx);
		storeWCtx(wctx, request);
		return IdentityProviderFlow.AUTHENTICATION_WITH_IDENTIFICATION;
	}

	@Override
	public ReturnResponse handleReturnResponse(HttpSession httpSession,
			Identity identity, Address address, String authenticatedIdentifier,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		LOG.debug("handleReturnResponse");
		String wtrealm = retrieveWtrealm(httpSession);
		ReturnResponse returnResponse = new ReturnResponse(wtrealm);
		returnResponse.addAttribute("wa", "wsignin1.0");
		String wctx = retrieveWctx(httpSession);
		returnResponse.addAttribute("wctx", wctx);
		String wresult = getWResult(wctx, wtrealm, identity,
				authenticatedIdentifier);
		returnResponse.addAttribute("wresult", wresult);
		return returnResponse;
	}

	private String getWResult(String wctx, String wtrealm, Identity identity,
			String authenticatedIdentifier) throws JAXBException,
			DatatypeConfigurationException, ParserConfigurationException,
			TransformerException, NoSuchAlgorithmException,
			InvalidAlgorithmParameterException, MarshalException,
			XMLSignatureException, TransformerFactoryConfigurationError,
			IOException {
		ObjectFactory trustObjectFactory = new ObjectFactory();
		RequestSecurityTokenResponseCollectionType requestSecurityTokenResponseCollection = trustObjectFactory
				.createRequestSecurityTokenResponseCollectionType();

		List<RequestSecurityTokenResponseType> requestSecurityTokenResponses = requestSecurityTokenResponseCollection
				.getRequestSecurityTokenResponse();
		RequestSecurityTokenResponseType requestSecurityTokenResponse = trustObjectFactory
				.createRequestSecurityTokenResponseType();
		requestSecurityTokenResponses.add(requestSecurityTokenResponse);

		if (null != wctx) {
			requestSecurityTokenResponse.setContext(wctx);
		}

		List<Object> requestSecurityTokenResponseContent = requestSecurityTokenResponse
				.getAny();

		requestSecurityTokenResponseContent.add(trustObjectFactory
				.createTokenType("urn:oasis:names:tc:SAML:2.0:assertion"));
		requestSecurityTokenResponseContent
				.add(trustObjectFactory
						.createRequestType("http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue"));
		requestSecurityTokenResponseContent
				.add(trustObjectFactory
						.createKeyType("http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer"));

		RequestedSecurityTokenType requestedSecurityToken = trustObjectFactory
				.createRequestedSecurityTokenType();
		requestSecurityTokenResponseContent.add(trustObjectFactory
				.createRequestedSecurityToken(requestedSecurityToken));

		oasis.names.tc.saml._2_0.assertion.ObjectFactory samlObjectFactory = new oasis.names.tc.saml._2_0.assertion.ObjectFactory();
		AssertionType assertion = samlObjectFactory.createAssertionType();
		requestedSecurityToken.setAny(samlObjectFactory
				.createAssertion(assertion));

		AttributeStatementType attributeStatement = samlObjectFactory
				.createAttributeStatementType();
		assertion.getStatementOrAuthnStatementOrAuthzDecisionStatement().add(
				attributeStatement);
		/*
		 * Maybe we should be using OpenSAML2 here instead of the JAXB binding?
		 */
		assertion.setVersion("2.0");
		String assertionId = "saml-" + UUID.randomUUID().toString();
		assertion.setID(assertionId);
		DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
		DateTime issueInstantDateTime = new DateTime();
		GregorianCalendar issueInstantCalendar = issueInstantDateTime
				.toGregorianCalendar();
		assertion.setIssueInstant(datatypeFactory
				.newXMLGregorianCalendar(issueInstantCalendar));
		NameIDType issuer = samlObjectFactory.createNameIDType();
		X509Certificate certificate = this.configuration.getIdentity();
		issuer.setValue(certificate.getSubjectX500Principal().toString());
		assertion.setIssuer(issuer);

		SubjectType subject = samlObjectFactory.createSubjectType();
		assertion.setSubject(subject);
		NameIDType nameId = samlObjectFactory.createNameIDType();
		nameId.setValue(authenticatedIdentifier);
		subject.getContent().add(samlObjectFactory.createNameID(nameId));

		SubjectConfirmationType subjectConfirmation = samlObjectFactory
				.createSubjectConfirmationType();
		subject.getContent().add(
				samlObjectFactory
						.createSubjectConfirmation(subjectConfirmation));
		subjectConfirmation.setMethod("urn:oasis:names:tc:SAML:2.0:cm:bearer");

		List<Object> attributes = attributeStatement
				.getAttributeOrEncryptedAttribute();
		AttributeType nameAttribute = samlObjectFactory.createAttributeType();
		attributes.add(nameAttribute);
		nameAttribute
				.setName("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name");
		nameAttribute.getAttributeValue().add(identity.getName());

		ConditionsType conditions = samlObjectFactory.createConditionsType();
		assertion.setConditions(conditions);
		DateTime notBeforeDateTime = issueInstantDateTime;
		DateTime notAfterDateTime = notBeforeDateTime.plusHours(1);
		conditions.setNotBefore(datatypeFactory
				.newXMLGregorianCalendar(notBeforeDateTime
						.toGregorianCalendar()));
		conditions
				.setNotOnOrAfter(datatypeFactory
						.newXMLGregorianCalendar(notAfterDateTime
								.toGregorianCalendar()));
		List<ConditionAbstractType> conditionList = conditions
				.getConditionOrAudienceRestrictionOrOneTimeUse();
		AudienceRestrictionType audienceRestriction = samlObjectFactory
				.createAudienceRestrictionType();
		audienceRestriction.getAudience().add(wtrealm);
		conditionList.add(audienceRestriction);

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		Document document = documentBuilder.newDocument();

		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class,
				oasis.names.tc.saml._2_0.assertion.ObjectFactory.class);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper",
				new WSFederationNamespacePrefixMapper());
		marshaller
				.marshal(
						trustObjectFactory
								.createRequestSecurityTokenResponseCollection(requestSecurityTokenResponseCollection),
						document);

		signDocument(document, assertionId);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		writeDocument(document, outputStream);
		return new String(outputStream.toByteArray());
	}

	protected void writeDocument(Document document,
			OutputStream documentOutputStream)
			throws TransformerConfigurationException,
			TransformerFactoryConfigurationError, TransformerException,
			IOException {
		Result result = new StreamResult(documentOutputStream);
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		Source source = new DOMSource(document);
		xformer.transform(source, result);
	}

	private void signDocument(Document document, String assertionId)
			throws TransformerException, NoSuchAlgorithmException,
			InvalidAlgorithmParameterException, MarshalException,
			XMLSignatureException {
		Element nsElement = document.createElement("ns");
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:saml",
				"urn:oasis:names:tc:SAML:2.0:assertion");
		Node samlAssertionNode = XPathAPI.selectSingleNode(document,
				"//saml:Assertion", nsElement);
		if (null == samlAssertionNode) {
			throw new IllegalStateException(
					"saml:Assertion element not present");
		}
		Node samlSubjectNode = XPathAPI.selectSingleNode(samlAssertionNode,
				"saml:Subject", nsElement);
		if (null == samlSubjectNode) {
			throw new IllegalStateException("saml:Subject element not present");
		}

		PrivateKey privateKey = this.configuration.getPrivateIdentityKey();
		X509Certificate certificate = this.configuration.getIdentity();

		XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance(
				"DOM", new org.jcp.xml.dsig.internal.dom.XMLDSigRI());

		XMLSignContext signContext = new DOMSignContext(privateKey,
				samlAssertionNode, samlSubjectNode);
		signContext.putNamespacePrefix(
				javax.xml.crypto.dsig.XMLSignature.XMLNS, "ds");
		DigestMethod digestMethod = signatureFactory.newDigestMethod(
				DigestMethod.SHA1, null);

		List<Transform> transforms = new LinkedList<Transform>();
		transforms.add(signatureFactory.newTransform(Transform.ENVELOPED,
				(TransformParameterSpec) null));
		Transform exclusiveTransform = signatureFactory
				.newTransform(CanonicalizationMethod.EXCLUSIVE,
						(TransformParameterSpec) null);
		transforms.add(exclusiveTransform);

		Reference reference = signatureFactory.newReference("#" + assertionId,
				digestMethod, transforms, null, null);

		SignatureMethod signatureMethod = signatureFactory.newSignatureMethod(
				SignatureMethod.RSA_SHA1, null);
		CanonicalizationMethod canonicalizationMethod = signatureFactory
				.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE,
						(C14NMethodParameterSpec) null);
		SignedInfo signedInfo = signatureFactory.newSignedInfo(
				canonicalizationMethod, signatureMethod, Collections
						.singletonList(reference));

		List<Object> keyInfoContent = new LinkedList<Object>();
		KeyInfoFactory keyInfoFactory = KeyInfoFactory.getInstance();
		List<Object> x509DataObjects = new LinkedList<Object>();
		x509DataObjects.add(certificate);
		X509Data x509Data = keyInfoFactory.newX509Data(x509DataObjects);
		keyInfoContent.add(x509Data);
		KeyInfo keyInfo = keyInfoFactory.newKeyInfo(keyInfoContent);

		javax.xml.crypto.dsig.XMLSignature xmlSignature = signatureFactory
				.newXMLSignature(signedInfo, keyInfo);
		xmlSignature.sign(signContext);
	}

	@Override
	public void init(ServletContext servletContext,
			IdentityProviderConfiguration configuration) {
		LOG.debug("init");
		this.configuration = configuration;
	}
}
