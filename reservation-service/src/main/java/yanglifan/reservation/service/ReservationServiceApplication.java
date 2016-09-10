package yanglifan.reservation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Stream;

@RepositoryRestResource
interface ReservationRepository extends JpaRepository<Reservation, Long> {

}

//@EnableBinding(Sink.class)
@IntegrationComponentScan
@EnableDiscoveryClient
@SpringBootApplication
public class ReservationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReservationServiceApplication.class, args);
    }
}

@Component
class SampleGenerator implements CommandLineRunner {
    private ReservationRepository reservationRepository;

    @Autowired
    public SampleGenerator(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    public void run(String... strings) throws Exception {
        Stream.of("Lifan", "Jinlong", "Hongliang", "Tannan")
                .forEach(name -> reservationRepository.save(new Reservation(name)));
        reservationRepository.findAll().forEach(System.out::println);
    }
}

@RefreshScope
@RestController
class MessageController {
    private String message;

    public MessageController(@Value("${message}") String message) {
        this.message = message;
    }

    //    @RequestMapping(value = "/message", method = RequestMethod.GET)
    @GetMapping("/message")
    public String getMessage() {
        return message;
    }
}

@MessageEndpoint
class ReservationProcessor {
    @Autowired
    private ReservationRepository reservationRepository;

    @ServiceActivator(inputChannel = "input")
    public void write(String reservationName) {
        reservationRepository.save(new Reservation(reservationName));
    }
}