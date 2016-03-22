The eID Identity Provider is a simple IdP using the eID as authentication token. The eID IdP supports different authentication protocols:
  * SAML v2 browser POST profile
  * OpenID 2.0 with AX, PAPE, UI extension support
  * WS-Federation

The scope of this project is not to create an IDM product. The eID IdP limits itself to the eID card and the attribute set that this authentication token offers. The eID IdP has no local attribute store, nor provides support for other tokens but the eID card.

The eID IdP is implemented as a Java EE application. The eID IdP is using the eID Applet to perform the entity authentication. The eID Trust Service is used for validation of the authentication certificate chain of the citizen.

The eID IdP comes with an eID IdP SDK to ease the task of developers on integrating the eID IdP functionality in web application.

Make yourself member of the [eID Applet group](http://groups.google.com/group/eid-applet) for free support and staying up to date with the eID products. Given the constant security threats in the world of web applications, the security features of the eID products are ever evolving. Every day we invest effort in keeping the eID products as safe as possible by applying innovative security concepts. Via the eID Applet group we also keep you informed about eID product security updates.