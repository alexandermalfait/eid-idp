# DotNetOpenAuth #

The DotNetOpenAuth library provides an interesting alternative to Windows Identity Foundation for integrating the eID IdP within your ASP.NET web applications.
The big advantage over WIF is that you can easily have programmatic control over the eID IdP page flow using DotNetOpenAuth.

This library is open source and available via:
[DotNetOpenAuth home page](http://dotnetopenauth.net/)

This library is using the [OpenID 2.0 authentication](http://openid.net/specs/openid-authentication-2_0.html) protocol to communicate with the eID IdP service.

# Sample ASP.NET page #

Let's start with a simple example ASP.NET page.

```xml

<%@ Page Language="C#" AutoEventWireup="true" CodeFile="Default.aspx.cs" Inherits="_Default" %>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">

<head runat="server">

<title>Test Page

Unknown end tag for &lt;/title&gt;





Unknown end tag for &lt;/head&gt;



<body>

<h1>Test Page

Unknown end tag for &lt;/h1&gt;



<form id="form1" runat="server">

<asp:Button runat="server" ID="loginButton" Text="Login" OnClick="loginButton_Click" />

<asp:Button runat="server" ID="logoutButton" Text="Logout" Visible="false" OnClick="logoutButton_Click"/>

<br />

<asp:Label ID="fullName" runat="server" />



Unknown end tag for &lt;/form&gt;





Unknown end tag for &lt;/body&gt;





Unknown end tag for &lt;/html&gt;


```

# The Code #

The code behind this ASP.NET page looks as follows:
```C#

using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Web.UI;
using System.Web.UI.WebControls;

using DotNetOpenAuth.OpenId.RelyingParty;
using DotNetOpenAuth.OpenId;
using DotNetOpenAuth.OpenId.Extensions.AttributeExchange;

public partial class _Default : System.Web.UI.Page {

protected void Page_Load(object sender, EventArgs e) {
OpenIdRelyingParty openid = new OpenIdRelyingParty();
openid.SecuritySettings.AllowDualPurposeIdentifiers = true;
IAuthenticationResponse response = openid.GetResponse();
if (null != response) {
if (response.Status == AuthenticationStatus.Authenticated) {
FetchResponse fetchResponse = response.GetExtension<FetchResponse>();
this.fullName.Text = fetchResponse.Attributes["http://axschema.org/namePerson"].Values[0];
this.loginButton.Visible = false;
this.logoutButton.Visible = true;
} else {
this.fullName.Text = "Autentication failed";
}
} else {
this.fullName.Text = "Not logged in yet.";
}
}

protected void loginButton_Click(object sender, EventArgs e) {
OpenIdRelyingParty openid = new OpenIdRelyingParty();
IAuthenticationRequest request =
openid.CreateRequest("https://www.e-contract.be/eid-idp/endpoints/openid/ident");

// attribute query
FetchRequest fetchRequest = new FetchRequest();
fetchRequest.Attributes.AddRequired("http://axschema.org/namePerson");
request.AddExtension(fetchRequest);

request.RedirectToProvider();
}

protected void logoutButton_Click(object sender, EventArgs e) {
OpenIdRelyingPartyControlBase.LogOff();
this.logoutButton.Visible = false;
this.loginButton.Visible = true;
}
}
```

The OpenID based eID IdP login is initiated by clicking the login button, which triggers the execution of the `loginButton_Click` function. The available eID IdP OpenID endpoints at e-contract.be are:
| **eID IdP OpenID endpoint URL** |
|:--------------------------------|
| <pre>https://www.e-contract.be/eid-idp/endpoints/openid/ident</pre> |
| <pre>https://www.e-contract.be/eid-idp/endpoints/openid/auth</pre> |
| <pre>https://www.e-contract.be/eid-idp/endpoints/openid/auth-ident</pre> |

After authenticating the end-user the eID IdP redirects to the same page. Via the `Page_Load` function we process the incoming OpenID response message.

The only exotic configuration required to make DotNetOpenAuth to work with the eID IdP is,
```C#

openid.SecuritySettings.AllowDualPurposeIdentifiers = true;
```
This allows the usage of OP selected user identifiers.

# Attributes #

Via the [OpenID Attribute Exchange](http://openid.net/specs/openid-attribute-exchange-1_0.html) extension you can retrieve user attributes.
In DotNetOpenAuth you add such an extension to the `request` object via:
```C#

FetchRequest fetchRequest = new FetchRequest();
fetchRequest.Attributes.AddRequired("http://axschema.org/namePerson");
request.AddExtension(fetchRequest);
```

The list of supported attribute URIs is given below.

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

# Internationalization #

Another interesting OpenID extension is given in the following example:
```C#

using DotNetOpenAuth.OpenId.Extensions.UI;
..
UIRequest uiRequest = new UIRequest();
uiRequest.LanguagePreference = new System.Globalization.CultureInfo[] { new System.Globalization.CultureInfo("nl") };
request.AddExtension(uiRequest);
```

This extension allows you to control the language used by the eID IdP towards the end-user.

# National Registration Number #

The national registration number can be retrieved from the claimed identifier as follows:
```C#

string claimedIdentifier = response.ClaimedIdentifier.ToString();
string nationalRegistrationNumber =
claimedIdentifier.Substring(claimedIdentifier.IndexOf("?") + 1);
```

Note that you need an authorization from the privacy commission to store/process the national registration number. Hence it might be useful to transform the national registration number to an application specific unique user identifier as follows:
```C#

byte[] applicationKey = System.Text.Encoding.ASCII.GetBytes("application-specific-secret");
HMACSHA1 hmac = new HMACSHA1(applicationKey);
byte[] hash = hmac.ComputeHash(System.Text.Encoding.ASCII.GetBytes(nationalRegistrationNumber));
string userIdentifier = BitConverter.ToString(hash).Replace("-", "");
```

# Base 64 URL Safe encoded attributes #

The eID photo and eID authentication certificate attributes are Base 64 URL Safe encoded.

To request the eID authentication certificate attribute, add the following to the request:
```C#

FetchRequest fetchRequest = new FetchRequest();
fetchRequest.Attributes.AddRequired("http://axschema.org/eid/cert/auth");
request.AddExtension(fetchRequest);
```

Read out the eID authentication certificate attribute as follows:
```C#

FetchResponse fetchResponse = response.GetExtension<FetchResponse>();
string base64UrlSafeAuthCert = fetchResponse.Attributes["http://axschema.org/eid/cert/auth"].Values[0];
string base64AuthCert = base64UrlSafeAuthCert.Replace('-', '+').Replace('_', '/');
if (base64AuthCert.Length % 3 != 0) base64AuthCert += '=';
if (base64AuthCert.Length % 3 != 0) base64AuthCert += '=';
byte[] encodedAuthCert = Convert.FromBase64String(base64AuthCert);
X509Certificate2 authCert = new X509Certificate2(encodedAuthCert);
```