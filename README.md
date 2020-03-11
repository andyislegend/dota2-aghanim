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
**Dota2-Aghanim** provide two types of the Steam client:
- synchronous, represented by `SteamClient` class.
- asynchronous, represented by `SteamClientAsync` class.

Synchronous client provide two types of methods:
1) Regular methods, e.g. `steamUser.logOn(logOnDetails)`. It will provide registered in queue `CompletableFuture` callback 
and developer can handle it according to the business logic of the application.
2) Methods with timeout argument, e.g. `steamUser.logOn(logOnDetails, timeout)` It will automatically handle 
`CompletableFuture` callback and return Steam server response or throw `CallbackTimeoutException` in case response 
won't be received during specified time.

Developer should consider next Steam Network and Game Coordinator behavior: 
> Sometimes Steam server could ignore request and won't respond on client request. When developer want to use CompletableFuture from regular methods of the synchronous client, better always to keep this in mind.

Before making calls to Steam Network and Game Coordinator services you need to open connection and login to Steam server.
Synchronous client provide **manual** and **automatic** control on connection logic:

##### Automatic connection logic
User can use automated logic provided by Steam client, which will automatically: 
1) Re-established connection in case it was disconnected by Steam server.
2) Get next user credentials registered in `UserCredentialsProvider` and try to login to Steam Network.
3) Execute additional actions described in `onAutoReconnect` callback.

 ```java
public static void main(String[] args) throws CallbackTimeoutException {
    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
        var timeoutInMillis = 15000;
        var steamClient = new SteamClient();

        LogOnDetails logOnDetails = new LogOnDetails();
        logOnDetails.setUsername(args[0]);
        logOnDetails.setPassword(args[1]);
        // We need to provide list of the user credentials, which automatic reconnect logic will use to rotate connection session.
        steamClient.setCredentialsProvider(new UserCredentialsProvider(List.of(logOnDetails)));
        
        // We can register additional actions, which reconnect logic will execute after successful establishing connection. 
        steamClient.setOnAutoReconnect((client) -> {
            var gameServer = client.getHandler(SteamGameServer.class);
            var dotaClient = client.getHandler(SteamGameCoordinator.class).getHandler(DotaClient.class);
            try {
                gameServer.setClientPlayedGame(List.of(SteamGame.Dota2.getApplicationId()), timeoutInMillis);
                dotaClient.sendClientHello(timeoutInMillis);
            } catch (CallbackTimeoutException e) {
                client.setReconnectOnUserInitiated(true);
                client.disconnect();
            }
        });
        
        // We establish connection with Steam Network and login user credentials registered in UserCredentialsProvider.
        steamClient.connectAndLogin();

        // now You can query for data using Steam Network API.
    }
}
 ```

##### Manual connection logic
###### LogIn to Steam server using synchronous Steam client and regular methods:
 ```java
public static void main(String[] args) throws CallbackTimeoutException {
    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
        var timeoutInMillis = 15000;
        var steamClient = new SteamClient();

        steamClient.connectAndGetCallback()
                .thenAccept((packetMessage) -> System.out.println("Successfully connected to Steam Network!"))
                .get(timeoutInMillis, TimeUnit.MILLISECONDS);

        var details = new LogOnDetails();
        details.setUsername(args[0]);
        details.setPassword(args[1]);

        var steamUser = steamClient.getHandler(SteamUser.class);
        steamUser.logOn(details)
                .thenAccept((logOnResponse) -> System.out.println("Result of logOn response: " + logOnResponse.getResult().name()))
                .get(timeoutInMillis, TimeUnit.MILLISECONDS);
        
        // now You can query for data using Steam Network API.
    }
}
 ```

###### LogIn to Steam server using synchronous Steam client and methods with timeout:
 ```java
public static void main(String[] args) throws CallbackTimeoutException {
    var timeoutInMillis = 15000;
    var steamClient = new SteamClient();

    steamClient.connect(timeoutInMillis);
    System.out.println("Successfully connected to Steam Network!");

    var details = new LogOnDetails();
    details.setUsername(args[0]);
    details.setPassword(args[1]);

    var steamUser = steamClient.getHandler(SteamUser.class);
    try {
        var logOnResponse = steamUser.logOn(details, timeoutInMillis);
        System.out.println("Result of logOn response: " + logOnResponse.getResult().name());
    } catch (CallbackTimeoutException e) {
        // You can retry or simply disconnect here and try later on.
        steamClient.disconnect();
    }
    // now You can query for data using Steam Network API.
}
 ```

###### LogIn to Steam server using asynchronous Steam client:
 ```java
public static void main(String[] args) {
    if (args.length < 2) {
        System.out.println("Sample1: No username and password specified!");
        return;
    }

    Logger.getRootLogger().setLevel(Level.DEBUG);
    BasicConfigurator.configure();
    new ReadmeAsync(args[0], args[1]).run();
}

@Override
public void run() {
    // create our steamclient instance
    steamClient = new SteamClientAsync();

    // create the callback manager which will route callbacks to function calls
    manager = new DefaultCallbackManager(steamClient);

    // get the steamuser handler, which is used for logging on after successfully connecting
    steamUser = steamClient.getHandler(SteamUserAsync.class);

    manager.subscribe(ConnectedCallback.class, this::onConnected);
    manager.subscribe(DisconnectedCallback.class, this::onDisconnected);

    manager.subscribe(LoggedOnCallback.class, this::onLoggedOn);
    manager.subscribe(LoggedOffCallback.class, this::onLoggedOff);

    // Here we will need to subscribe on Steam APIs messages which we want to receive from Steam Network.

    isRunning = true;

    System.out.println("Connecting to steam...");

    // initiate the connection
    steamClient.connect();

    // create our callback handling loop
    while (isRunning) {
        // in order for the callbacks to get routed, they need to be handled by the manager
        manager.runWaitCallbacks(1000L);
    }
}

private void onConnected(ConnectedCallback callback) {
    System.out.println("Connected to Steam! Logging in " + username + "...");

    LogOnDetails details = new LogOnDetails();
    details.setUsername(username);
    details.setPassword(password);

    steamUser.logOn(details);
}

private void onDisconnected(DisconnectedCallback callback) {
    System.out.println("Disconnected from Steam");
    isRunning = false;
}

private void onLoggedOn(LoggedOnCallback callback) {
    if (callback.getResult() != EResult.OK) {
        if (callback.getResult() == EResult.AccountLogonDenied) {
            System.out.println("Unable to logon to Steam: This account is SteamGuard protected.");
            isRunning = false;
            return;
        }

        System.out.println("Unable to logon to Steam: " + callback.getResult());
        isRunning = false;
        return;

    }
    System.out.println("Logon to Steam: " + callback.getResult());
    // now You need to make request to nexxt Steam API service or You can logOff:
    steamUser.logOff();
    isRunning = false;
}

private void onLoggedOff(LoggedOffCallback callback) {
    System.out.println("Logged off of Steam: " + callback.getResult());
    isRunning = false;
}
 ```

After successful login, we can init session with Game Coordinator server and request DOTA2 related data:
###### Request DOTA2 Match details using synchronous Dota client and regular methods:
 ```java
public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException, CallbackTimeoutException {
    var timeoutInMillis = 15000;
    var steamClient = new SteamClient();

    //Here we already open connection and logIn to Steam Network
    
    // DotaClient will automatically set DOTA2 played status in Steam and make "Hello" message exchange with Game Coordinator.
    var gameServer = steamClient.getHandler(SteamGameServer.class);
    var dotaClient = steamClient.getHandler(SteamGameCoordinator.class).getHandler(DotaClient.class);
    try {
        gameServer.setClientPlayedGame(List.of(SteamGame.Dota2.getApplicationId()), timeoutInMillis);
        dotaClient.sendClientHello(timeoutInMillis);
    } catch (CallbackTimeoutException e) {
        steamUser.logOff();
    }
    var dotaMatchId = 5239025268L;
    try {
        dotaClient.getMatchDetails(dotaMatchId)
                .thenAccept(matchDetails -> System.out.println("Match duration time: " + matchDetails.getDuration()))
                .get(timeoutInMillis, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
        System.out.println("Timeout message: " + e.getMessage());
    } finally {
        // We need to log off or disconnect from Steam Server to stop application.
        steamUser.logOff();
    }
}
```

###### Request DOTA2 Match details using synchronous Dota client and methods with timeout:
 ```java
public static void main(String[] args) throws CallbackTimeoutException {
    var timeoutInMillis = 15000;
    var steamClient = new SteamClient();

    //Here we already open connection and logIn to Steam Network
    
    // DotaClient will automatically set DOTA2 played status in Steam and make "Hello" message exchange with Game Coordinator.
    var gameServer = steamClient.getHandler(SteamGameServer.class);
    var dotaClient = steamClient.getHandler(SteamGameCoordinator.class).getHandler(DotaClient.class);
    try {
        gameServer.setClientPlayedGame(List.of(SteamGame.Dota2.getApplicationId()), timeoutInMillis);
        dotaClient.sendClientHello(timeoutInMillis);
    } catch (CallbackTimeoutException e) {
        steamUser.logOff();
    }
    var dotaMatchId = 5239025268L;
    try {
        var matchDetails = dotaClient.getMatchDetails(dotaMatchId, timeoutInMillis);
        System.out.println("Match duration time: " + matchDetails.getDuration());
    } catch (CallbackTimeoutException e) {
        System.out.println("Timeout message: " + e.getMessage());
    } finally {
        // We need to log off or disconnect from Steam Server to stop application.
        steamUser.logOff();
    }
}

```

###### Request DOTA2 Match details using asynchronous Dota client:
 ```java
@Override
public void run() {
    //Here we already register callbacks for handling open connection and logIn callbacks

    manager.subscribe(GameConnectTokensCallback.class, this::onGameTokens);
    manager.subscribe(ClientWelcomeCallback.class, this::onClientWelcome);

    manager.subscribe(DotaMatchDetailsCallback.class, this::onMatchDetails);

    //Here we already open connection and launch callback manager
}

private void onLoggedOn(LoggedOnCallback callback) {
    if (callback.getResult() != EResult.OK) {
        if (callback.getResult() == EResult.AccountLogonDenied) {
            System.out.println("Unable to logon to Steam: This account is SteamGuard protected.");
            isRunning = false;
            return;
        }

        System.out.println("Unable to logon to Steam: " + callback.getResult());
        isRunning = false;
        return;

    }
    System.out.println("Logon to Steam: " + callback.getResult());
    gameServerAsync.sendGamePlayed(List.of(SteamGame.Dota2.getApplicationId()));
}

// Callback for handling Game played response
private void onGameTokens(GameConnectTokensCallback callback) {
    try {
        System.out.println("GameConnectTokens:");
        System.out.println(mapper.writeValueAsString(callback));;
    } catch (JsonProcessingException e) {
        System.out.println(e.getMessage());
    }
    dotaClientAsync.sendClientHello();
}

private void onClientWelcome(ClientWelcomeCallback callback) {
    try {
        System.out.println("ClientWelcome:");
        System.out.println(mapper.writeValueAsString(callback));;
    } catch (JsonProcessingException e) {
        System.out.println(e.getMessage());
    }

    dotaClientAsync.requestMatchDetails(5239025268L);
}

private void onMatchDetails(DotaMatchDetailsCallback callback) {
    try {
        System.out.println("MatchDetails:");
        System.out.println(mapper.writeValueAsString(callback));;
    } catch (JsonProcessingException e) {
        System.out.println(e.getMessage());
    }
    dotaClientAsync.requestAccountProfileCard(137935311);
}
 ```

## Examples
Examples can be found on our [Wiki page](https://github.com/andyislegend/dota2-aghanim/wiki).

## Build
To build library from scratch clone this repo and after that execute next command from `root` folder of the project:
```
mvn clean install 
```
Please note, `mvn clean compile` command from `root` of the project won't work. 
Library use internal `steam-language-gen` plugin to build Java classes from `.steamd` files. To execute plugin Maven 
Reactor require `plugin.xml` descriptor to be present in `steam-language-gen/target/META-INF/../plugin.xml` folder, 
which will be created after `steam-language-gen` compile phase.  
