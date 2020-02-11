package jp.gr.java_conf.ogibayashi.prometheus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown=true)
@EqualsAndHashCode(exclude={"value", "timestamp"})
@NoArgsConstructor
public class KafkaExporterLogEntry {
    @NonNull private String name;
    @NonNull private String value;
    private String timestamp;
    private Map<String,String> labels;
}
