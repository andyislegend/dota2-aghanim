![Logo](https://i.ibb.co/yyVmp7h/logo-aghs.png)

# DOTA2-Aghanim

[![GNU License](https://img.shields.io/badge/license-GNU%20GPL%20v3-green.svg)](https://github.com/andyislegend/dota2-aghanim/blob/master/LICENSE)
![Publish library to GitHub Packages](https://github.com/andyislegend/dota2-aghanim/workflows/Publish%20library%20to%20GitHub%20Packages/badge.svg)

**DOTA2-Aghanim** is a Java library to interpolate with Valve's [Steam network](http://store.steampowered.com/about).
In the first place this library was created to get data related to Dota2, but nothing prevents from using it for other Valve's games. 
It aims to provide an interface to perform various actions on the network and fetch DOTA related data.

## Requirements
**DOTA2-Aghanim** is written in java 11 and can be used with any jdk11 and higher.

## Dependencies
Next base dependencies should be provided in your project:
```xml
        <!--(1)-->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>${slf4j-api.version}</version>
        </dependency>
        <!--(2)-->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>${commons-lang3.version}</version>
        </dependency>
        <!--(3)-->
        <dependency>
            <groupId>com.google.protobuf</groupId>
            <artifactId>protobuf-java</artifactId>
            <version>${protobuf-java.version}</version>
        </dependency>
        <!--(4)-->
        <dependency>
            <groupId>org.java-websocket</groupId>
            <artifactId>Java-WebSocket</artifactId>
            <version>${Java-WebSocket.version}</version>
        </dependency>
        <!--(5)-->
        <dependency>
            <groupId>com.github.corese4rch</groupId>
            <artifactId>cvurl-io</artifactId>
            <version>${cvurl-io.version}</version>
        </dependency>
        <!--(6)-->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-core</artifactId>
            <version>${jackson.version}</version>
        </dependency>
        <!--(7)-->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>
```
Depends on platform add the appropriate cryptography dependency to your project:
```xml
        <!-- NON-ANDROID ONLY -->
        <dependency>
            <groupId>org.bouncycastle</groupId>
            <artifactId>bcprov-jdk15on</artifactId>
            <version>${bcprov-jdk15on.version}</version>
        </dependency>
        <!-- ANDROID ONLY -->
        <dependency>
            <groupId>com.madgag.spongycastle</groupId>
            <artifactId>prov</artifactId>
            <version>${prov.version}</version>
        </dependency>
```

## How to get DOTA2-Aghanim 
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

## How to use DOTA2-Aghanim
```java

public static void main(String[] args) {  
    var steamClient = new SteamClient();
    steamClient.connect();

    var details = new LogOnDetails();
    steamUser = new SteamUser(steamClient);
    details.setUsername(args[0]);
    details.setPassword(args[1]);
    steamUser.logOn(details)

    var client = new DotaClient(new GameCoordinator(steamClient));
    // now You can query for data using DotaClient

    steamUser.logOff();
}
```

## Build
To build library from scratch clone this repo and after that execute next command from `root` folder of the project:
```
mvn clean install 
```
Please note, `mvn clean compile` command from `root` of the project won't work. 
Library use internal `steam-language-gen` plugin to build Java classes from `.steamd` files. To execute plugin Maven 
Reactor require `plugin.xml` descriptor to be present in `steam-language-gen/target/META-INF/../plugin.xml` folder, 
which will be created after `steam-language-gen` compile phase.  
