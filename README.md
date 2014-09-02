# XTREMEPUSH TITANIUM MODULE

This module provide access to [XtremePush](http://xtremepush.com) service from Titanium applications.

## INTEGRATION DOCUMENTATION

You can find integration documentation [here](documentation/index.md).

## USAGE EXAMPLE

[Usage example](example/app.js) of module.



REGISTER YOUR MODULE
--------------------

Register your module with your application by editing `tiapp.xml` and adding your module.
Example:

<modules>
	<module version="0.1">com.krilab.xtremepush</module>
</modules>

When you run your project, the compiler will combine your module along with its dependencies
and assets into the application.


USING YOUR MODULE IN CODE
-------------------------

To use your module in code, you will need to require it.

For example,

	var my_module = require('com.krilab.xtremepush');
	my_module.foo();
