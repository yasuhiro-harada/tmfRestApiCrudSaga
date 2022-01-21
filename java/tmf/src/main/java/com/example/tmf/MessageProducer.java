package com.example.tmf;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageProducer{

    @Value("${spring.kafka.producer.bootstrap-servers}") String producerBootstrapServers;
    @Value("${spring.kafka.producer.topic}") String producerTopic;
	
	public void produce(MessageKey messageKey, ResData resData) throws TmfException
	{
		KafkaProducer<byte[], byte[]> producer = null;
		try{
			Properties properties = new Properties();
			properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producerBootstrapServers);

			producer = new KafkaProducer<>(properties, new ByteArraySerializer(), new  ByteArraySerializer());
			ProducerRecord<byte[], byte[]> producerRecord = new ProducerRecord<>(producerTopic, KafkaUtil.serializeMessageKey(messageKey), KafkaUtil.serializeResData(resData));
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