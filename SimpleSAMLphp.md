# Introduction #

This document describes how to integrate eID authentication/identification using the eID Identity Provider wihin your PHP web application. [SimpleSAMLphp](http://simpleSAMLphp.org) will be used for the SAML integration. We assume that you use version 1.12.0.


# Install SimpleSAMLphp #

Please refer to the SimpleSAMLphp documentation for installation instructions. In the following sections we use `$SIMPLE_SAML_HOME` to indicate the home directory of your SimplSAMLphp installation. Note that you should replace every occurrence of `$SIMPLE_SAML_HOME` with the actual location on your platform.

First we need to configure the Apache HTTPD service.
Create a file named `/etc/httpd/conf.d/simplesamlphp.conf` that contains the following:
```
Alias /simplesaml $SIMPLE_SAML_HOME/www
```
Note that you have to change `$SIMPLE_SAML_HOME` according to your installation.

# SimpleSAMLphp configuration #

You certainly should change the SimpleSAMLphp administrator password.
Edit `$SIMPLE_SAML_HOME/config/config.php` and change the value of `auth.adminpassword`.

Add the following entry to `$SIMPLE_SAML_HOME/metadata/saml20-idp-remote.php`:
```
$metadata['www.e-contract.be'] = array (
  'SingleSignOnService' => 'https://www.e-contract.be/eid-idp/protocol/saml2/post/auth-ident',
  'certFingerprint' => '5981a2be47ca66203c9165edeb697d833df1b77d',
);
```

The values for `SingleSignOnService` and `certFingerprint` can be found on the [eID Identity Provider home page](https://www.e-contract.be/eid-idp/).

If you want to use the eID Identity Provider as your default identity provider, also configure the following.
Edit `$SIMPLE_SAML_HOME/config/authsources.php`: set the entity ID of the eID IDP you just configured in `saml20-idp-remote.php` as default IDP.

```
<?php

$config = array(

        // An authentication source which can authenticate against both SAML 2.0
        // and Shibboleth 1.3 IdPs.
        'default-sp' => array(
                'saml:SP',

                // The entity ID of the IdP this should SP should contact.
                // Can be NULL/unset, in which case the user will be shown a list of available IdPs.
                'idp' => 'www.e-contract.be',

        ),

);
```

# Restart HTTPD #

Depending on your system, you can restart the Apache HTTPD service via
```
/etc/init.d/httpd restart
```
or
```
systemctl restart httpd.service
```

# Test the configuration #

You can easily test out the configuration by navigating to
```
https://localhost/simplesaml
```
Next click on the "Authentication" tab. Click "Test configured authentication sources".  Click "default-sp". Select the "www.e-contract.be" identity provider.

After performing the authentication, you should see a page displaying all eID IdP provided attributes.


# Sample Service Provider script #

Put these files somewhere in the same folder in your httpd document tree

## index.php ##

```
<?php
require_once('/var/simplesamlphp/lib/_autoload.php');
$as = new SimpleSAML_Auth_Simple('default-sp');
$as->requireAuth();
?>
<html>
<head>
<title>eID IDP Test SP</title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/> 
</head>
<body>
<?php
session_start();
$attributes = $as->getAttributes();
print ("Hello, " . $attributes["http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name"][0] . "<br />");

print ("<table>");
foreach ($attributes as $key => $value) {
  if ( $key == "be:fedict:eid:idp:photo") {
    $_SESSION["imagedata"] = $value[0];
    print ("<tr><td><strong>". $key . "</td><td><img src='image.php' /></td></tr>");
  }
  else
    print ("<tr><td><strong>" . $key . "</strong></td><td>" . $value[0] . "</td></tr>");
}
print ("</table>");
?>
</body>
</html>
```

## image.php ##

```
<?php
# 
# image.php: simple jpeg generator with image payload in session variable
#   session variables:
#         imagedata : base64 encoded jpeg payload
#
# set jpeg mime type
header('Content-Type: image/jpeg');

session_start();
# echo decoded image payload
echo base64_decode($_SESSION["imagedata"]);
?>
```


# Security Analysis #

We performed a pragmatic security analysis of SimpleSAMLphp using WebScarab.

## Replay attack ##

In this attack we replay a previous captured SAML authentication response message.
The SimpleSAMLphp library refuses to accept the replayed SAML message.

## Remove signature ##

In this attack we remove the XML signature on the SAML assertion within the authentication response message.
The SimpleSAMLphp library refuses to accept an unsigned SAML assertion.

## Resign assertion ##

In this attack we resign the SAML assertion received within the authentication response message using our own service provider key.
The SimpleSAMLphp library refuses to accept the resigned SAML assertion.

## Assertion decryption ##

In this attack we decrypt the SAML assertion received within the authentication response message using our own service provider key.
Per default the SimpleSAMLphp library does not detect a man-in-the-middle decryption of the incoming SAML assertion.
This attack can be mitigated by adding the following configuration option to `metadata/saml20-idp-remote.php` under the metadata entry of the identity provider:
```
'assertion.encryption' => true,
```

Note that this option only makes sense for identity providers that support encryption. The eID Identity Provider does not support encryption per default.

## Attribute injection ##

In this attack we inject a new value for an attribute within the SAML assertion received within the authentication response message.
The SimpleSAMLphp library refuses to accept an altered SAML assertion. It detects this integrity attack by checking the XML signature creates by the identity provider.

## Signature wrapping ##

In this setup we scanned the signature wrapping attack vector.
We could successfully duplicate the SAML assertion using both the `ds:Object` and `samlp:Extensions` free XML extension points.
However, SimpleSAMLphp successfully detected various attempts in altering the SAML assertion ID attribute.
Hence we could not inject altered attribute values without SimpleSAMLphp detecting our attacks.