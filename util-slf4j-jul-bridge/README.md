# Twitter Util SLF4J API Bridging Support

### Rerouting `java.util.logging` when using [`slf4j`](https://www.slf4j.org/)

Additional configuration is necessary to reroute the `java.util.logging` system when using the 
[`slf4j-api`](https://www.slf4j.org/). The reason is that the `jul-to-slf4j` bridge cannot replace 
classes in the `java.util.logging` package to do the redirection statically as it does for the other 
bridge implementations. Instead, it has to register a handler on the root logger and listen for 
logging statements like any other handler. It will then redirect those logging statements 
appropriately. This redirection is accomplished via the `slf4j-api` 
[`SLF4JBridgeHandler`](https://www.slf4j.org/api/org/slf4j/bridge/SLF4JBridgeHandler.html).

The `util-slf4j-jul-bridge` module is a wrapper for the [`slf4j`](https://www.slf4j.org/) 
`jul-to-slf4j` dependency and provides a utility to safely install the 
[`SLF4JBridgeHandler`](https://www.slf4j.org/apidocs/org/slf4j/bridge/SLF4JBridgeHandler.html) 
for [bridging legacy logging APIs](https://www.slf4j.org/legacy.html) including `java.util.logging`.

The utility attempts to protect the user from installing the [`SLF4JBridgeHandler`](https://www.slf4j.org/apidocs/org/slf4j/bridge/SLF4JBridgeHandler.html) 
if the `slf4j-jdk14` logging implementation is detected on the classpath to prevent 
[an infinite loop](https://www.slf4j.org/legacy.html#julRecursion).
