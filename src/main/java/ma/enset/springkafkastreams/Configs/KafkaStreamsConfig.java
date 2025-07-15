package ma.enset.springkafkastreams.Configs;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KafkaStreamsConfig {

    // Noms des state stores pour pouvoir les interroger plus tard
    public static final String TOTAL_CLICKS_STORE = "total-clicks-store";
    public static final String CLICKS_PER_USER_STORE = "clicks-per-user-store";

    @Bean
    public KafkaStreams kafkaStreams(
            @Value("${app.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${app.kafka.streams.application-id}") String applicationId,
            @Value("${app.kafka.streams.state-dir}") String stateDir,
            @Value("${app.kafka.topic.clicks}") String clicksTopic,
            @Value("${app.kafka.topic.counts}") String countsTopic) {

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.STATE_DIR_CONFIG, stateDir);

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> clicksStream = builder.stream(clicksTopic);

        // Topologie 1: Calculer le nombre total de clics
        KTable<String, Long> totalClicksTable = clicksStream
                .selectKey((key, value) -> "total-clicks") // Regrouper tous les messages sous une seule clé
                .groupByKey()
                .count(Materialized.as(TOTAL_CLICKS_STORE));

        // Envoyer le résultat du compte total au topic de sortie
        totalClicksTable.toStream().to(countsTopic, Produced.with(Serdes.String(), Serdes.Long()));

        // Topologie 2: Calculer les clics par utilisateur (la clé est le userId)
        clicksStream
                .groupByKey() // Groupement par la clé existante (userId)
                .count(Materialized.as(CLICKS_PER_USER_STORE)); // Stocker dans un autre state store

        // Construire et retourner l'objet KafkaStreams
        return new KafkaStreams(builder.build(), props);
    }
}
