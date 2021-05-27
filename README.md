# Demo of the new Foreign Linker API (Java 16)

## Build
mvn clean install

## Run
`java -jar --add-modules jdk.incubator.foreign -Dforeign.restricted=permit -jar target/de.itemis.mosig.flademo-1.0.0-SNAPSHOT-shaded.jar testString`
