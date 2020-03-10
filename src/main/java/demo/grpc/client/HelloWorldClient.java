package demo.grpc.client;

import grpc.helloworld.HelloWorldStreamingServiceGrpc;
import grpc.helloworld.Ok;
import grpc.helloworld.Person;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static java.util.stream.IntStream.range;

@Component
@Slf4j
public class HelloWorldClient {

    private HelloWorldStreamingServiceGrpc.HelloWorldStreamingServiceStub helloWorldServiceStub;

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
        var managedChannel = ManagedChannelBuilder.forAddress("localhost", 6565).usePlaintext().build();
        helloWorldServiceStub = HelloWorldStreamingServiceGrpc.newStub(managedChannel);
    }

    public void sayHello(String firstName, String lastName) throws InterruptedException {
        // Setup the response handling for this batch
        var result = helloWorldServiceStub.sayHello(okStreamObserver);

        // Send a batch of data as a continuous stream
        range(0, 10).forEach(i -> {
            log.info(format("\"Sending %d", i));
            result.onNext(Person.newBuilder().setFirstName(firstName).setLastName(format("%s:%d", lastName, i)).build());
        });

        // Arbitrary sleep
        sleep(1000);
        log.info("Finished sending");
    }
}
