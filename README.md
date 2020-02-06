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
Library use `steam-language-gen` plugin to generate Java classes from `.steamd` files. In case you don't have installed plugin
in the local m2 repository, you will need to install it before launch build of the library. 
To build and install plugin from `steam-language-gen` folder execute next command:
```
mvn clean install 
```
If plugin was installed to local repository, you can build library by execution command from `root` folder of the project:
```
mvn clean install 
```
