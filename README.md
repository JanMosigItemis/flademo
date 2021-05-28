# Demo of the new Foreign Linker API (Java 16)

## Build
`mvn clean install`

## Run
* Default: `mvn exec:exec`
* Custom test string: `mvn exec:exec -Dtest.string=foobar`
* Plain: `java --add-modules jdk.incubator.foreign -Dforeign.restricted=permit -jar target/de.itemis.mosig.flademo-1.0.0-SNAPSHOT.jar testString`

## What does it do?
This demo computes the length of an arbitrary string in characters, e. g. the length of `foobar` would be 6.  
It does so by using `size_t strlen (const char *s)` of the [C Standard Library](https://www.gnu.org/software/libc/manual/html_node/String-Length.html) instead of the Java based `java.lang.String.length()`.
  
In order to call `strlen` directly from Java, it uses the new [Foreign Linker API (FLA)](https://openjdk.java.net/jeps/389).

## Acknowledgements
This little demo was inspired by the work of [Markus Karg](https://gitlab.com/mkarg/foreignlinkerapi). You may also want to checkout his [Youtube channel](https://www.youtube.com/channel/UCOPEUog206SxNI-LM6Y8Wwg)  
  
Also [https://cr.openjdk.java.net/~mcimadamore/panama/ffi.html](https://cr.openjdk.java.net/~mcimadamore/panama/ffi.html) was a very helpful source of knowledge.

## Note on -Dforeign.restricted=permit
In order to use code from `jdk.incubator.foreign`, it is necessary to set the system property `foreign.restricted` to one of `permit`, `warn` or `debug` (see [https://openjdk.java.net/jeps/393](https://openjdk.java.net/jeps/393)).  
  
This must be specified at the command line of the JVM and cannot be changed at runtime. For details, see below.  
  
### IDE Launch Configurations
Because of this, running the app or the accompanying tests straight after project import will most likely fail. This is, why the project comes with IDE specific launch configurations for both. See subdir `ide_setup`. Currently only Eclipse is supported.  
  
### Details
Usually it is sufficient to set this property programmatically at runtime via `System.setProperty`. However, in case of `jdk.incubator.foreign` access to incubated code is secured with a check to `jdk.internal.foreign.Utils.foreignRestrictedAccess`.  
  
This value is computed at JVM startup like this:  
`private static final String foreignRestrictedAccess = Optional.ofNullable(VM.getSavedProperty("foreign.restricted")).orElse("deny");`  
This means, it is not possible to change behavior via `System.setProperty`, because at the time this call gets executed, it is already too late.  
  
This means, it is required to set this property at JVM startup, which is only possible via command line args.