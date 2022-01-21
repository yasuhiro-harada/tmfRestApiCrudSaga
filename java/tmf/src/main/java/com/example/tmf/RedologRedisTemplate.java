package com.example.tmf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

//=========================================================
// Redolog用 RedisTemplate クラス
// @Configuration必須。
//=========================================================
@Configuration
public class RedologRedisTemplate{

    @Autowired
    @Qualifier("redologLettuceConnectionFactory")
    private LettuceConnectionFactory lettuceConnectionFactory;

    //====================================================================================
    // Redolog用 RedisTemplate。@Bean必須。
    //====================================================================================
    @Bean
	public RedisTemplate<String, Redolog> getRedologRedisTemplate(){
		
		RedisTemplate<String, Redolog> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.setHashKeySerializer(redisTemplate.getKeySerializer());
        redisTemplate.setHashValueSerializer(redisTemplate.getValueSerializer());
		return redisTemplate;
    }
}