package demo.grpc.server;

import com.google.protobuf.Empty;
import grpc.helloworld.HelloWorldStreamingServiceGrpc;
import grpc.helloworld.Ok;
import grpc.helloworld.Person;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.GRpcService;

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
