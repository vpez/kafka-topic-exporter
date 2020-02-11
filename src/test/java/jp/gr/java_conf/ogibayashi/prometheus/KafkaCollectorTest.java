package jp.gr.java_conf.ogibayashi.prometheus;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;

public class KafkaCollectorTest extends TestCase
{
    private ObjectMapper mapper = new ObjectMapper();
    private PropertyConfig emptyConfig = new PropertyConfig();
    
    public void testRecordParsing() {
        final String logRecord = "{\"labels\":{\"__name__\":\"container_tasks_state\",\"container_name\":\"istio-proxy\",\"endpoint\":\"https-metrics\",\"id\":\"/kubepods/burstable/pod4b9ec602-e450-11e9-b9ff-000c29595c38/9060278e694dd0787af2ce3391f049736924aca3fc1005b92a4838a4cb47086a\",\"image\":\"sha256:01594d7a3746e05ae47449ef3b91a6588f43c646c61b482c0c3e0ee1a14c44e0\",\"instance\":\"10.10.10.12:10250\",\"job\":\"kubelet\",\"name\":\"k8s_istio-proxy_istio-ingressgateway-57f7cfdfdd-x9krd_istio-system_4b9ec602-e450-11e9-b9ff-000c29595c38_6\",\"namespace\":\"istio-system\",\"node\":\"k8s-worker-12.dmp\",\"pod_name\":\"istio-ingressgateway-57f7cfdfdd-x9krd\",\"prometheus\":\"prometheus-monitoring/prometheus-operator-prometheus\",\"prometheus_replica\":\"prometheus-prometheus-operator-prometheus-0\",\"service\":\"prometheus-operator-kubelet\",\"state\":\"stopped\"},\"name\":\"container_tasks_state\",\"timestamp\":\"2020-02-11T13:16:17Z\",\"value\":\"0\"}";

        try {
            KafkaExporterLogEntry record = mapper.readValue(logRecord, KafkaExporterLogEntry.class);
            assertEquals("0", record.getValue());
        } catch(Exception e) {
            fail(e.getMessage());
        }
    }

    // public void testAddSimpleMetric() {
    //     KafkaCollector collector = new KafkaCollector(emptyConfig);
    //     final String logRecord = "{\"name\":\"foo\", \"value\": \"9\"}";
    //     final String topic = "test.hoge";

    //     collector.add(topic, logRecord);
    //     List<MetricFamilySamples> mfsList = collector.collect();
    //     MetricFamilySamples mfs = mfsList.get(0);
        
    //     assertEquals("test_hoge_foo", mfs.name);
    //     assertEquals(Collector.Type.GAUGE, mfs.type);
    //     assertEquals("", mfs.help);
    //     assertEquals(9.0, mfs.samples.get(0).value);
    // }
    
    // public void testAddMetricWithLabel() throws IOException {
    //     KafkaCollector collector = new KafkaCollector(emptyConfig);
    //     final String logRecord = "{\"name\":\"foo\", \"labels\": { \"label1\": \"v1\", \"lable2\": \"v2\" }, \"value\": 9}";
    //     final String topic = "test.hoge";
    //     KafkaExporterLogEntry jsonRecord = mapper.readValue(logRecord, KafkaExporterLogEntry.class);
        
    //     collector.add(topic, logRecord);
    //     List<MetricFamilySamples> mfsList = collector.collect();
    //     MetricFamilySamples mfs = mfsList.get(0);
    //     Map<String, String> labelMap = MetricUtil.getLabelMapFromSample(mfs.samples.get(0));
        
    //     assertEquals("test_hoge_foo", mfs.name);
    //     assertEquals(Collector.Type.GAUGE, mfs.type);
    //     assertEquals("", mfs.help);
    //     assertEquals(jsonRecord.getLabels(), labelMap);
    //     assertEquals(9.0, mfs.samples.get(0).value);
    // }
    
    // public void testAddMetricWithLabelAndTimestamp() throws IOException {
    //     KafkaCollector collector = new KafkaCollector(emptyConfig);
    //     final String logRecord = "{\"name\":\"test.foo\", \"labels\": { \"label1\": \"v1\", \"lable2\": \"v2\" }, \"value\": 9, \"timestamp\": 1517330227}";
    //     final String topic = "test.hoge";
    //     KafkaExporterLogEntry jsonRecord = mapper.readValue(logRecord, KafkaExporterLogEntry.class);
        
    //     collector.add(topic, logRecord);
    //     List<MetricFamilySamples> mfsList = collector.collect();
    //     MetricFamilySamples mfs = mfsList.get(0);
    //     Map<String, String> labelMap = MetricUtil.getLabelMapFromSample(mfs.samples.get(0));
       
    //     assertEquals("test_hoge_test_foo", mfs.name);
    //     assertEquals(Collector.Type.GAUGE, mfs.type);
    //     assertEquals("", mfs.help);
    //     assertEquals(jsonRecord.getLabels(), labelMap);
    //     assertEquals(9.0, mfs.samples.get(0).value);
    //     assertEquals(1517330227, mfs.samples.get(0).timestampMs.longValue());
    // }

    // public void testReplaceValueWithSameLabel() throws IOException {
    //     KafkaCollector collector = new KafkaCollector(emptyConfig);

    //     final String logRecord1 = "{\"name\":\"foo\", \"labels\": { \"label1\": \"v1\", \"label2\": \"v2\" }, \"value\": 9}";
    //     final String logRecord2 = "{\"name\":\"foo\", \"labels\": { \"label1\": \"aa1\", \"label2\": \"bb2\" }, \"value\": 10}";
    //     final String logRecord3 = "{\"name\":\"foo\", \"labels\": { \"label1\": \"v1\", \"label2\": \"v2\" }, \"value\": 18}";

    //     final String topic = "test.hoge";
    //     KafkaExporterLogEntry jsonRecord = mapper.readValue(logRecord3, KafkaExporterLogEntry.class);
        
    //     collector.add(topic, logRecord1);
    //     collector.add(topic, logRecord2);
    //     collector.add(topic, logRecord3);
    //     List<MetricFamilySamples> mfsList = collector.collect();
    //     MetricFamilySamples mfs = mfsList.get(0);
    //     List<MetricFamilySamples.Sample> samples = mfs.samples;

    //     assertEquals(2, samples.size());
    //     assertEquals(jsonRecord.getLabels(), MetricUtil.getLabelMapFromSample(samples.get(1)));
    //     assertEquals(18.0, samples.get(1).value);
            
    // }

    // public void testMetricExpire() throws IOException {
    //     PropertyConfig config = new PropertyConfig();
    //     config.set("exporter.metric.expire.seconds", "120");

    //     KafkaCollector collector = new KafkaCollector(config);
    //     LocalDateTime setDate1 = LocalDateTime.of(2016, 9, 20, 10, 0);
    //     LocalDateTime setDate2 = LocalDateTime.of(2016, 9, 20, 10, 9);
    //     LocalDateTime getDate = LocalDateTime.of(2016, 9, 20, 10, 10);

    //     final String logRecord1 = "{\"name\":\"foo\", \"labels\": { \"label1\": \"v1\", \"lable2\": \"v2\" }, \"value\": 9}";
    //     final String logRecord2 = "{\"name\":\"foo\", \"labels\": { \"label1\": \"aa1\", \"lable2\": \"bb2\" }, \"value\": 10}";
    //     final String topic = "test.hoge";
    //     KafkaExporterLogEntry jsonRecord = mapper.readValue(logRecord2, KafkaExporterLogEntry.class);

    //     collector.add(topic, logRecord1, setDate1);
    //     collector.add(topic, logRecord2, setDate2);

    //     List<MetricFamilySamples> mfsList = collector.collect(getDate);
    //     MetricFamilySamples mfs = mfsList.get(0);
    //     List<MetricFamilySamples.Sample> samples = mfs.samples;

    //     assertEquals(1, samples.size());
    //     assertEquals(jsonRecord.getLabels(), MetricUtil.getLabelMapFromSample(samples.get(0)));
    //     assertEquals(10.0, samples.get(0).value);
    // }
    
}
