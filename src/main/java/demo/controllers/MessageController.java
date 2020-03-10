package demo.controllers;

import demo.grpc.client.HelloWorldClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE, value = "/sendMessage")
public class MessageController {

    @Autowired
    private HelloWorldClient helloWorldClient;

    @GetMapping("/greeting")
    public void greeting(@RequestParam(value="firstname", defaultValue="Billy") String firstName,
                         @RequestParam(value="lastname", defaultValue="Bob") String lastName) throws InterruptedException {
        helloWorldClient.sayHello(firstName, lastName);
    }


}
