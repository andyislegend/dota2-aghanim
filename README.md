# DOTA2 -Aghanim

**DOTA2 -Aghanim** is a Java library to interpolate with Valve's [Steam network](http://store.steampowered.com/about). 
It aims to provide interface to perform various actions on the network and fetch DOTA related data.

## Requirements
**DOTA2 -Aghanim** is written in java 11 and can be used with any jdk11 and higher.

## Dependencies
The only dependencies that will be added to your project is:
```xml
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>${slf4j-api.version}</version>
            <scope>provided</scope>
        </dependency>
```
There is a high chance you will use some logging library so [Slf4j](https://github.com/qos-ch/slf4j) 
shouldn't be an unexpected resident in your build file.

## How to get DOTA2 -Aghanim 
 **Maven**
 ```xml
<dependecies>
    <dependency>
        <groupId>com.avenga</groupId>
        <artifactId>steam-client</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```
 **Gradle**
```groovy
compile group: 'com.avenga', name: 'steam-client', version: '1.0.0'
```

## Build
To build library execute next command from `root` folder of the project:
```
mvn clean install 
```
Please note, `mvn clean compile` command from `root` of the project won't work. 
Library use internal `steam-language-gen` plugin to build Java classes from `.steamd` files. To execute plugin Maven 
Reactor require `plugin.xml` descriptor to be present in `steam-language-gen/target/META-INF/../plugin.xml` folder, 
which will be created after `steam-language-gen` compile phase.  