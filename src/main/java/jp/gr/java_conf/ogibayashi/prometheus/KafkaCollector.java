package jp.gr.java_conf.ogibayashi.prometheus;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.prometheus.client.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

public class KafkaCollector extends Collector {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaCollector.class);

    private ObjectMapper mapper = new ObjectMapper();
    private List<MetricFamilySamples> mfsList = new ArrayList<MetricFamilySamples>();
    private Map<String, Map<KafkaExporterLogEntry, LocalDateTime>> metricEntries = new ConcurrentHashMap<String, Map<KafkaExporterLogEntry, LocalDateTime>>();
    private PropertyConfig pc;
    private long expire;
    
    public KafkaCollector(PropertyConfig pc) {
        this.pc = pc;
        expire = pc.getMetricExpire();
    }

    public void add(String topic, String recordValue) {
        add(topic, recordValue, LocalDateTime.now());
    }
    
    public void add(String topic, String recordValue, LocalDateTime datetime) {
        LOG.debug("add: {}, {}", topic, recordValue);
        try {
            KafkaExporterLogEntry record = mapper.readValue(recordValue, KafkaExporterLogEntry.class);

            String metricName = record.getName().replaceAll("\\.","_");

            // Add the topic name as prefix only if specified by the configuration
            if (pc.getExporterTopicPrefix()) {
                metricName = topic.replaceAll("\\.","_") + "_" + metricName;
            }

            if (metricName.startsWith("_")) {
              metricName = metricName.substring(1);
            }
            if(! metricEntries.containsKey(metricName)){
                metricEntries.put(metricName, new ConcurrentHashMap<>());
            }

            Map<KafkaExporterLogEntry, LocalDateTime> entry = metricEntries.get(metricName);
            entry.remove(record);
            entry.put(record, datetime);
        }
        catch(JsonMappingException e){
            LOG.warn("Invalid record: " + recordValue, e);
        }
        catch(Exception e){
            LOG.error("Error happened in adding record to the collector", e);
        }
    }

    @Override
    public List<MetricFamilySamples> collect() {
        return collect(LocalDateTime.now());
    }

    
    public List<MetricFamilySamples> collect(LocalDateTime current_timestamp) {
        
        List<MetricFamilySamples> mfsList = new ArrayList<>();
        for(Map.Entry<String, Map<KafkaExporterLogEntry, LocalDateTime>> e: metricEntries.entrySet()){
            List<MetricFamilySamples.Sample> samples = new ArrayList<>();
            for(Map.Entry<KafkaExporterLogEntry, LocalDateTime> le: e.getValue().entrySet()){
                if (expire != 0 && le.getValue().plusSeconds(expire).isBefore(current_timestamp)) {
                    e.getValue().remove(le.getKey());
                    if(e.getValue().isEmpty()) {
                        metricEntries.remove(e.getKey());
                    }
                }
                else {
                    samples.add(generateSample(e.getKey(), le.getKey()));
                }
            }
            mfsList.add(new MetricFamilySamples(e.getKey(), Type.GAUGE, "", samples));
        }

        return mfsList;
    }

    public MetricFamilySamples.Sample generateSample(String metricName, KafkaExporterLogEntry logEntry) {
        ArrayList<String> labelNames = new ArrayList<>();
        ArrayList<String> labelValues = new ArrayList<>();
        if (logEntry.getLabels() != null) {
            for(Map.Entry<String, String> entry: logEntry.getLabels().entrySet()){
                labelNames.add(entry.getKey());
                labelValues.add(entry.getValue());
            }
        }
        MetricFamilySamples.Sample sample = new MetricFamilySamples.Sample(metricName, 
            labelNames, labelValues, 
            Double.valueOf(logEntry.getValue()), 
            getTimestamp(logEntry.getTimestamp()));
        LOG.debug("sample: {}", sample );
        return sample; 
    }

    private long getTimestamp(String timestamp) {
        String ISO_DATE_FORMAT_ZERO_OFFSET = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        String UTC_TIMEZONE_NAME = "UTC";

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ISO_DATE_FORMAT_ZERO_OFFSET);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone(UTC_TIMEZONE_NAME));
        
        long value = 0;

        try {
            value = simpleDateFormat.parse(timestamp).getTime();
        } catch(ParseException e) {
            LOG.error(e.getMessage(), e);
        }

        return value;
    }
}
