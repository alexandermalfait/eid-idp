# eID IdP as OpenID identity provider #

The eID IdP product can act as an OpenID identity provider.
The eID IdP supports the [OpenID Authentication 2.0](http://openid.net/specs/openid-authentication-2_0.html) protocol. Together with the following extensions:
  * [OpenID Attribute Exchange 1.0](http://openid.net/specs/openid-attribute-exchange-1_0.html)
  * [OpenID Provider Authentication Policy Extension 1.0](http://openid.net/specs/openid-provider-authentication-policy-extension-1_0.html)
  * [OpenID User Interface Extension 1.0](http://svn.openid.net/repos/specifications/user_interface/1.0/trunk/openid-user-interface-extension-1_0.html)

The eID IdP uses OP selected identifiers according to the OpenID 2.0 specification.

For the eID IdP instance running on the e-contract.be server, your OpenID identifier can be any of:
| **eID IdP OpenID endpoint URL** |
|:--------------------------------|
| <pre>https://www.e-contract.be/eid-idp/endpoints/openid/ident</pre> |
| <pre>https://www.e-contract.be/eid-idp/endpoints/openid/auth</pre> |
| <pre>https://www.e-contract.be/eid-idp/endpoints/openid/auth-ident</pre> |

Copy and paste this identifier when web applications ask for your OpenID identifier.

# OpenID Attribute Exchange #

The following table lists the supported OpenID attribute URIs.

| **Attribute URI** |
|:------------------|
| <pre>http://axschema.org/eid/card-validity/end</pre> |
| <pre>http://axschema.org/person/gender</pre> |
| <pre>http://axschema.org/contact/postalAddress/home</pre> |
| <pre>http://axschema.org/namePerson/first</pre> |
| <pre>http://axschema.org/eid/photo</pre> |
| <pre>http://axschema.org/eid/card-validity/begin</pre> |
| <pre>http://axschema.org/contact/city/home</pre> |
| <pre>http://axschema.org/contact/postalCode/home</pre> |
| <pre>http://axschema.org/birthDate</pre> |
| <pre>http://openid.net/schema/birthDate/birthYear</pre> |
| <pre>http://openid.net/schema/birthDate/birthMonth</pre> |
| <pre>http://openid.net/schema/birthDate/birthday</pre> |
| <pre>http://axschema.org/eid/pob</pre> |
| <pre>http://axschema.org/eid/card-number</pre> |
| <pre>http://axschema.org/eid/nationality</pre> |
| <pre>http://axschema.org/namePerson/last</pre> |
| <pre>http://axschema.org/namePerson</pre> |
| <pre>http://axschema.org/eid/rrn</pre> |
| <pre>http://axschema.org/eid/cert/auth</pre> |
| <pre>http://axschema.org/eid/age</pre> |

# OpenID Applications #

The following OpenID enabled applications accept the eID IdP OpenID identity:
  * https://www.e-contract.be/eid-idp-sp/
  * http://www.mychores.co.uk
  * https://www.hampr.com/
  * http://redbirdapps.com/
  * http://demand.openid.net/
  * http://www.wikispaces.com/
  * http://jyte.com
  * http://trac.openidenabled.com/trac/
  * http://getsatisfaction.com/
  * http://www.pokersource.info/
  * http://identi.ca/