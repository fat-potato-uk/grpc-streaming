### Polymorphic Json and Freemarker

In this example we are going to create a streaming gRpc connection. For this, we must include the following
in our `pom.xml`:

```xml
<plugin>
    <groupId>org.xolstice.maven.plugins</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
    <version>0.6.1</version>
    <configuration>
        <protocArtifact>
            com.google.protobuf:protoc:3.3.0:exe:${os.detected.classifier}
        </protocArtifact>
        <pluginId>grpc-java</pluginId>
        <pluginArtifact>
            io.grpc:protoc-gen-grpc-java:1.4.0:exe:${os.detected.classifier}
        </pluginArtifact>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>compile</goal>
                <goal>compile-custom</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

We will then need to define our interface via a `proto` file:

```proto
syntax = "proto3";

option java_multiple_files = true;
package grpc.helloworld;

message Person {
  string first_name = 1;
  string last_name = 2;
}

message Ok {
}

service HelloWorldService {
  rpc sayHello (stream Person) returns (Ok);
}
```

This both defines the objects/entities we are going to use in communication as well as the
API itself. When the project is then built, we have several classes generated for us which
we can use to create a client and server:

```java
@Component
@Slf4j
public class HelloWorldCallbackClient {

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private HelloWorldStreamingServiceGrpc.HelloWorldServiceFutureStub helloWorldServiceFutureStub;

  @PostConstruct
  private void init() {
    var managedChannel = ManagedChannelBuilder.forAddress("localhost", 6565).usePlaintext().build();
    helloWorldServiceFutureStub = HelloWorldStreamingServiceGrpc.newFutureStub(managedChannel);
  }

  public void sayHello(String firstName, String lastName) throws ExecutionException, InterruptedException {
    var person = Person.newBuilder().setFirstName(firstName).setLastName(lastName).build();
    log.info("client sending {}", person);
    ListenableFuture<Greeting> future = helloWorldServiceFutureStub.sayHello(person);
    addCallback(future, getCallback(), directExecutor());
    withTimeout(future, 1, TimeUnit.SECONDS, scheduler);
  }

  private FutureCallback<Greeting> getCallback() {
    return new FutureCallback<>() {
      public void onSuccess(Greeting greeting) {
        log.info("Got a response: {}", greeting.getMessage());
      }
      public void onFailure(@Nullable Throwable t) {
        log.error(requireNonNull(t).getMessage());
      }
    };
  }
}
```

```java
@GRpcService
@Slf4j
public class HelloWorldServiceImpl extends HelloWorldStreamingServiceGrpc.HelloWorldServiceImplBase {

  @Override
  public void sayHello(Person request, StreamObserver<Greeting> responseObserver) {
    log.info("server received {}", request);
    // Do heavy processing...
    try { sleep((long) (Math.random() * 1500)); } catch (InterruptedException e) { e.printStackTrace(); }

    var message  = format("Hello %s %s!", request.getFirstName(),request.getLastName());
    var greeting = Greeting.newBuilder().setMessage(message).build();
    responseObserver.onNext(greeting);
    responseObserver.onCompleted();
  }

}
```

There is a lot of flexibility in the way gRpc can communicate with other services,
this hopefully provides a (very) brief introduction on how you can achieve this!