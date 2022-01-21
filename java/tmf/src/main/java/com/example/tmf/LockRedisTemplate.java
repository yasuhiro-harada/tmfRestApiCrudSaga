package com.example.tmf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

//=========================================================
// Lock用 RedisTemplate クラス
// @Configuration必須。
//=========================================================
@Configuration
public class LockRedisTemplate{

    @Autowired
    @Qualifier("lockLettuceConnectionFactory")
    private LettuceConnectionFactory lettuceConnectionFactory;

    //====================================================================================
    // Lock用 RedisTemplate。@Bean必須。
    //====================================================================================
    @Bean
	public StringRedisTemplate getLockRedisTemplate(){
		
		StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory);
		return redisTemplate;
    }
}