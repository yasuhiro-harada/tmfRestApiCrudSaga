package com.example.tmf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

//=========================================================
// Lock用 ConnectionFactory クラス
//=========================================================
@Configuration
public class LockConnectionFactory{

    // application.propertiesの値を設定
    @Value("${spring.redis.lock.host}") String host;
    @Value("${spring.redis.lock.port}") int port;
    @Value("${spring.redis.lock.user}") String user;
    @Value("${spring.redis.lock.password}") String password;
    @Value("${spring.redis.lock.database}") int database;

	//=========================================================
	// lock用 ConnectionFactory を返す
	//=========================================================
	@Bean
	public LettuceConnectionFactory lockLettuceConnectionFactory(){

 		// lettuce(native Redis Client) Instance化(hostName, portNo設定)
		RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(host, port);
		// userName設定
		redisStandaloneConfiguration.setUsername(user);
		// password設定
		redisStandaloneConfiguration.setPassword(password);
		// database設定
		redisStandaloneConfiguration.setDatabase(database);

        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }
}