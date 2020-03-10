### Streaming gRPC

In this example we are going to create a streaming gRPC connection. For this, we must include the following
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
public class HelloWorldClient {

    private HelloWorldStreamingServiceGrpc.HelloWorldStreamingServiceStub helloWorldServiceStub;

    private int count;
    private StreamObserver<Person> result;
    private ManagedChannel managedChannel;

    private StreamObserver<Ok> okStreamObserver = new StreamObserver<>() {
        @Override
        public void onNext(Ok ok) {
            log.info("Got response {}", ok);
        }

        @Override
        public void onError(Throwable throwable) {
            // Do nothing
        }

        @Override
        public void onCompleted() {
            // Do nothing
        }
    };


    @PostConstruct
    private void init() {
        createConnection();
    }

    private void createConnection() {
        managedChannel = ManagedChannelBuilder.forAddress("localhost", 6565).usePlaintext().build();
        helloWorldServiceStub = HelloWorldStreamingServiceGrpc.newStub(managedChannel);
        result = null;
    }

    public void sayHello(String firstName, String lastName) throws InterruptedException {
        // Setup the response for handling the connection to the server
        result = (result != null) ? result : helloWorldServiceStub.sayHello(okStreamObserver);

        // Send a batch of data as a continuous stream
        range(0, 10).forEach(i -> {
            log.info(format("\"Sending %d", i));
            result.onNext(Person.newBuilder().setFirstName(firstName).setLastName(format("%s:%d", lastName, i)).build());
        });
        
        // Arbitrary sleep
        sleep(1000);
        log.info("Finished sending");
        count++;

        // Close and recreate the connection on every 3rd "batch"
        if(count % 3 == 0) {
            log.info("Closing channel");
            managedChannel.shutdownNow();
            createConnection();
        }
    }
}
```

```java
@GRpcService
@Slf4j
public class HelloWorldServiceImpl extends HelloWorldStreamingServiceGrpc.HelloWorldStreamingServiceImplBase {
    @Override
    public StreamObserver<Person> sayHello(StreamObserver<Ok> responseObserver) {
        // Send the acknowledgement response
        responseObserver.onNext(Ok.newBuilder().build());

        // Return the handler for processing the received Persons
        return new StreamObserver<>() {
            @Override
            public void onNext(Person person) {
                log.info("Got person {}", person);
            }

            @Override
            public void onError(Throwable throwable) {
                // Do nothing
            }

            @Override
            public void onCompleted() {
                // Do nothing
            }
        };
    }
}
```

The first bean is the streaming client that sends over a stream of `Person` objects and initialises the objects used
for handling the response. The second bean is the server (`@GRpcService`) that sends an acknowledgement message then
returns a handler for processing the received `Person` objects. As we are re-using objects here, we will only get a response
for the first message(s), afterwards we will continue to invoke the call-back function directly.

The client shows how we can manually terminate the connection and re-establish if required. This can also be done via the server
side through the `GRpcServerBuilderConfigurer`.