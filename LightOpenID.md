# Introduction #

The LightOpenID library can be used to integrate the eID IdP within your PHP application.

http://code.google.com/p/lightopenid/

# Example #

The following code sample demonstrates a simple eID based authentication with readout of all available eID attributes.

```
<?php
include "openid.php";
$openid = new LightOpenID('localhost');
if ($openid->mode) {
    	echo $openid->validate() ? 'Logged in.' : 'Failed';
	echo '<pre>';
	echo print_r($openid->getAttributes(), true);
	echo '</pre>';
} else {
	$openid->identity = 'https://www.e-contract.be/eid-idp/endpoints/openid/auth-ident';
	$openid->required = array('namePerson/first', 'namePerson/last',
		'namePerson', 'person/gender', 'contact/postalCode/home',
		'contact/postalAddress/home', 'contact/city/home', 'eid/nationality',
		'eid/pob', 'birthDate', 'eid/card-number', 'eid/card-validity/begin',
		'eid/card-validity/end');
	header('Location: ' . $openid->authUrl());
}
?>
```

The available eID IdP OpenID endpoints (i.e., value of `$openid->identity`) at e-contract.be are:
| **eID IdP OpenID endpoint URL** |
|:--------------------------------|
| <pre>https://www.e-contract.be/eid-idp/endpoints/openid/ident</pre> |
| <pre>https://www.e-contract.be/eid-idp/endpoints/openid/auth</pre> |
| <pre>https://www.e-contract.be/eid-idp/endpoints/openid/auth-ident</pre> |

It is always advised to have deep integration within the security layer of your used PHP framework if possible.

# Example with eID photo #

The following example builds on the previous example, and adds the visualization of the eID photo attribute.

```
<?php

session_start();

include "openid.php";

function base64url_decode($base64url) {
	$base64 = strtr($base64url, '-_', '+/');
	$plainText = base64_decode($base64);
	return ($plainText);
}

$openid = new LightOpenID('localhost');
if ($openid->mode) {
    	echo $openid->validate() ? 'Logged in.' : 'Failed';
	echo ($openid->__get("identity"));
	echo '<pre>';
	echo print_r($openid->getAttributes(), true);
	echo '</pre>';
	$attributes = $openid->getAttributes();
	$encodedPhoto = $attributes['eid/photo'];
	$photo = base64url_decode($encodedPhoto);
	$_SESSION['photo'] = $photo;
	echo '<img src="photo.php"/>';
} else {
/* for $openid->identity = ... you can choose one of those (eid use with or without pincode) : */
/* https://www.e-contract.be/eid-idp/endpoints/openid/auth-ident */
/* https://www.e-contract.be/eid-idp/endpoints/openid/auth */
/* https://www.e-contract.be/eid-idp/endpoints/openid/ident */

	$openid->identity = 'https://www.e-contract.be/eid-idp/endpoints/openid/ident';
	$openid->required = array('namePerson/first', 'namePerson/last',
		'namePerson', 'person/gender', 'contact/postalCode/home',
		'contact/postalAddress/home', 'contact/city/home', 'eid/nationality',
		'eid/pob', 'birthDate', 'eid/card-number', 'eid/card-validity/begin',
		'eid/card-validity/end', 'eid/photo');
	header('Location: ' . $openid->authUrl());
}
?>
```

With photo.php containing:

```
<?php

session_start();

$photo = $_SESSION['photo'];

header('Content-Type: image/jpeg');

echo($photo);

?>
```

So we first push the decoded eID photo into the HTTP session. Next we output the eID photo as a JPEG.

The eID photo OpenID attribute is safe-URL-Base64 encoded by the eID IdP.