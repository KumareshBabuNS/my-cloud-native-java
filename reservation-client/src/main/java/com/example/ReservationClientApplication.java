package com.example;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import yanglifan.reservation.service.Reservation;

import java.util.Collection;
import java.util.Collections;

import static java.util.stream.Collectors.toList;

interface ReservationChannels {
    @Output
    MessageChannel output();
}

@EnableBinding(ReservationChannels.class)
@EnableZuulProxy
@EnableDiscoveryClient
@SpringBootApplication
public class ReservationClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReservationClientApplication.class, args);
    }

    @LoadBalanced
    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@RequestMapping("/reservations")
@RestController
class ReservationApiGateway {
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ReservationChannels reservationChannels;

    @PostMapping
    public void write(@RequestBody Reservation reservation) {
        MessageChannel output = this.reservationChannels.output();
        Message<String> message = MessageBuilder.withPayload(reservation.getReservationName()).build();
        output.send(message);
    }

    @HystrixCommand(fallbackMethod = "fallback")
    @GetMapping("/names")
    public Collection<String> read() {
        return this.restTemplate
                .exchange(
                        "http://reservation-service/reservations",
                        HttpMethod.GET, null,
                        new ParameterizedTypeReference<Resources<Reservation>>() {
                        })
                .getBody()
                .getContent()
                .stream()
                .map(Reservation::getReservationName)
                .collect(toList());
    }

    Collection<String> fallback() {
        return Collections.emptyList();
    }
}