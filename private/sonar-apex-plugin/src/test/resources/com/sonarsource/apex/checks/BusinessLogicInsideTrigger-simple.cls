trigger HelloWorldTrigger on Account (before insert) { // Compliant
	if (true) {
	  System.debug('Hello World!');
	}
}
