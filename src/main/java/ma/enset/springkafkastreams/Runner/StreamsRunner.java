package ma.enset.springkafkastreams.Runner;

import org.apache.kafka.streams.KafkaStreams;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StreamsRunner implements ApplicationRunner {

    private final KafkaStreams kafkaStreams;

    public StreamsRunner(KafkaStreams kafkaStreams) {
        this.kafkaStreams = kafkaStreams;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Nettoyer l'état local avant de démarrer.
        // Utile en développement pour repartir de zéro, à ne pas faire en production.
        kafkaStreams.cleanUp();

        // Ajouter un "shutdown hook" pour garantir une fermeture propre. C'est crucial.
        Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));

        // Démarrer le traitement des flux.
        kafkaStreams.start();
        System.out.println("Kafka Streams a démarré !");
    }
}