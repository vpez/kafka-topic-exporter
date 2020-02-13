package jp.gr.java_conf.ogibayashi.prometheus;

import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.orbitz.consul.Consul;
import com.orbitz.consul.HealthClient;
import com.orbitz.consul.model.health.ServiceHealth;

public class PropertyConfig {
    private static final Logger LOG = LoggerFactory.getLogger(PropertyConfig.class);

    public enum Constants {
        CONSUL_SERVER_URL("consul.server.url"),
        CONSUL_KAFKA_SERVICENAME("consul.kafka.servicename"),
        BOOTSTRAP_SERVERS("bootstrap.servers"),
        EXPORTER_PORT("exporter.port"),
        EXPORTER_TOPIC_PREFIX("exporter.topic.prefix"),
        EXPORTER_METRIC_EXPIRE("exporter.metric.expire.seconds"),
        KAFKA_CONSUMER_TOPICS("kafka.consumer.topics"),
        KAKFA_CONSUMER_REMOVEPREFIX("kafka.consumer.remove.prefix");

        public final String key;

        Constants(String key) {
            this.key = key;
        }
    }

    private final Properties props;

    public PropertyConfig() {
        props = new Properties();
    }
    
    public PropertyConfig(String propFilePath) throws IOException {
        props = loadProperties(propFilePath);
        props.put("enable.auto.commit", "false");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        changeBootstrapServersIfConsulServerAvailable(props);
    }

    void changeBootstrapServersIfConsulServerAvailable(Properties originalProps) {
        String consulServerUrl = originalProps.getProperty(Constants.CONSUL_SERVER_URL.key);
        String consulKafkaServicename = originalProps.getProperty(Constants.CONSUL_KAFKA_SERVICENAME.key);

        if (consulServerUrl != null && !consulServerUrl.equals("") && consulKafkaServicename != null && !consulKafkaServicename.equals("")) {
            LOG.info("CONSUL: Required properties for using Consul service discovery have been detected");
            ArrayList<String> bootstrapServersArray = getBootStrapServersFromConsul(consulServerUrl, consulKafkaServicename);
            if (bootstrapServersArray != null) {
                String bootstrapServers = String.join(",", bootstrapServersArray);
                if (bootstrapServers != null && !bootstrapServers.equals("")) {
                    String originalBootstrapServers = originalProps.getProperty(Constants.BOOTSTRAP_SERVERS.key);
                    LOG.info("CONSUL: \"bootstrap.servers\" property value was: [" + originalBootstrapServers + "]");
                    originalProps.setProperty(Constants.BOOTSTRAP_SERVERS.key, bootstrapServers);
                    LOG.info("CONSUL: \"bootstrap.servers\" property value has been set to: [" + bootstrapServers + "]");
                }
            }
        } else {
            LOG.info("CONSUL: Not found required properties for using Consul service discovery");
        }
    }

    ArrayList<String> getBootStrapServersFromConsul(String consulServer, String consulKafkaServicename) {
        ArrayList<String> bootstrapServers = null;
        LOG.info("CONSUL: Consul server used: [" + consulServer + "]");
        LOG.info("CONSUL: Consul service retrieved: [" + consulKafkaServicename + "]");
        try {
            Consul consul = Consul.builder().withUrl(consulServer).build();
            HealthClient healthClient = consul.healthClient();

            // discover only "passing" nodes
            List<ServiceHealth> nodes = healthClient.getHealthyServiceInstances(consulKafkaServicename).getResponse();
            for (ServiceHealth kafkaNode : nodes) {
                String address = kafkaNode.getService().getAddress();
                String port = kafkaNode.getService().getPort() + "";
                if (bootstrapServers == null) {
                    bootstrapServers = new ArrayList<String>();
                }
                bootstrapServers.add(address + ":" + port);
            }
        } catch (Exception e){
            LOG.error("CONSUL: " + e.toString());
        }
        LOG.info("CONSUL: Kafka services found through Consul server: " + (bootstrapServers==null?"None available":bootstrapServers.toString()));
        return bootstrapServers;
    }

    public Pattern getTopicsPattern() {
        return Pattern.compile(props.getProperty(Constants.KAFKA_CONSUMER_TOPICS.key));
    }
    
    public Properties getProperties() {
        return props;
    }

    public long getMetricExpire() {
        return Long.parseLong(get(Constants.EXPORTER_METRIC_EXPIRE.key, "0"));
    }

    public int getExporterPort() {
        return getInt(Constants.EXPORTER_PORT.key, 9185);
    }

    public boolean getExporterTopicPrefix() {
        return getBoolean(Constants.EXPORTER_TOPIC_PREFIX.key, false);
    }

    public String get(String key) {
        String value = props.getProperty(key);
        if (value == null)
            throw new RuntimeException(key + " parameter not found in the configuration");
        return value;
    }

    public String get(String key, String defaultValue) {
        String value = props.getProperty(key);
        if (value == null)
            return defaultValue;
        return value;
    }

    public void set(String key, String value) {
        props.setProperty(key, value);
    }
    
    public int getInt(String key) {
        return (int)Long.parseLong(get(key));
    }

    public int getInt(String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null)
            return defaultValue;
        return (int)Long.parseLong(get(key));
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = props.getProperty(key);
        if (value == null)
            return defaultValue;
        return Boolean.parseBoolean(value);
    }


    private Properties loadProperties(String propFilePath) throws IOException {
        Properties props = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream(propFilePath);
            if (input != null) {
                props.load(input);
            } else {
                throw new FileNotFoundException(propFilePath + "' not found");
            }
        } finally {
            input.close();
        }

        return props;
    }

}
