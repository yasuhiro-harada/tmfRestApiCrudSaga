package com.example.tmf;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.beans.factory.annotation.Value;

public class StubMessageProducer{

	@Value("${spring.kafka.producer.bootstrap-servers}") String producerBootstrapServers = "localhost:9092";
    @Value("${spring.kafka.producer.topic}") String producerTopic = "consumer";
	
	public void produce(MessageKey messageKey, ReqData reqData) throws TmfException
	{
		KafkaProducer<byte[], byte[]> producer = null;
		try{
			Properties properties = new Properties();
			properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producerBootstrapServers);

			producer = new KafkaProducer<>(properties, new ByteArraySerializer(), new  ByteArraySerializer());
			ProducerRecord<byte[], byte[]> producerRecord = new ProducerRecord<>(producerTopic, KafkaUtil.serializeMessageKey(messageKey), KafkaUtil.serializeReqData(reqData));
			producer.send(producerRecord).get();

		}catch(Exception ex){
			throw new TmfException("Kafkaのproduceに失敗");
		}finally{
			if(producer != null){
				producer.close();
			}
		}
	}
}