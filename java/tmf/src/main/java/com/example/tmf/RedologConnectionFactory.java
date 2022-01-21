package com.example.tmf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

//=========================================================
// Redolog用 ConnectionFactory クラス
//=========================================================
@Configuration
public class RedologConnectionFactory{

    // application.propertiesの値を設定
    @Value("${spring.redis.redolog.host}") String host;
    @Value("${spring.redis.redolog.port}") int port;
    @Value("${spring.redis.redolog.user}") String user;
    @Value("${spring.redis.redolog.password}") String password;
    @Value("${spring.redis.redolog.database}") int database;

	//=========================================================
	// Redolog用 ConnectionFactory を返す
	//=========================================================
    @Bean
    @Primary
	public LettuceConnectionFactory redologLettuceConnectionFactory(){

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