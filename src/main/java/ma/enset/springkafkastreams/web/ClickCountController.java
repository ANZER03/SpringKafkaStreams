package ma.enset.springkafkastreams.web;

import ma.enset.springkafkastreams.Configs.KafkaStreamsConfig;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class ClickCountController {

    private final KafkaStreams kafkaStreams;

    public ClickCountController(KafkaStreams kafkaStreams) {
        this.kafkaStreams = kafkaStreams;
    }

    @GetMapping("/clicks/count")
    public ResponseEntity<Long> getTotalClicks() {
        if (!kafkaStreams.state().isRunningOrRebalancing()) {
            return ResponseEntity.status(503).body(-1L); // Service Unavailable
        }
        try {
            ReadOnlyKeyValueStore<String, Long> countStore = kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType(
                            KafkaStreamsConfig.TOTAL_CLICKS_STORE,
                            QueryableStoreTypes.keyValueStore()
                    )
            );
            Long totalClicks = Optional.ofNullable(countStore.get("total-clicks")).orElse(0L);
            return ResponseEntity.ok(totalClicks);
        } catch (Exception e) {
            return ResponseEntity.status(503).body(-2L);
        }
    }

    @GetMapping("/clicks/count/by-user")
    public ResponseEntity<Map<String, Long>> getClicksByUser() {
        if (!kafkaStreams.state().isRunningOrRebalancing()) {
            return ResponseEntity.status(503).build();
        }
        Map<String, Long> userCounts = new HashMap<>();
        try {
            ReadOnlyKeyValueStore<String, Long> userStore = kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType(
                            KafkaStreamsConfig.CLICKS_PER_USER_STORE,
                            QueryableStoreTypes.keyValueStore()
                    )
            );
            try (KeyValueIterator<String, Long> all = userStore.all()) {
                while (all.hasNext()) {
                    KeyValue<String, Long> record = all.next();
                    userCounts.put(record.key, record.value);
                }
            }
            return ResponseEntity.ok(userCounts);
        } catch (Exception e) {
            return ResponseEntity.status(503).body(new HashMap<>());
        }
    }
}