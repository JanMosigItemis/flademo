# Demo of the new Foreign Linker API (Java 16)

## Build
`mvn clean install`

## Run
* Default: `mvn exec:exec`
* Custom test string: `mvn exec:exec -Dtest.string=foobar`
* Plain: `java --add-modules jdk.incubator.foreign -Dforeign.restricted=permit -jar target/de.itemis.mosig.flademo-1.0.0-SNAPSHOT.jar testString`

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