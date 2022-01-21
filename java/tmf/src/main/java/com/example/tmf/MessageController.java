package com.example.tmf;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class MessageController {

	@Autowired private MessageConsumer messageConsumer;
	@Autowired private JobBuilderFactory jobBuilderFactory;
	@Autowired private StepBuilderFactory stepBuilderFactory;
	
	@Bean
	public Step step() {
		return stepBuilderFactory.get("step")
				.tasklet(messageConsumer)
				.build();
	}
	@Bean
	public Job job(Step step1) throws Exception {
		return jobBuilderFactory.get("job")
				.incrementer(new RunIdIncrementer())
				.start(step1)
				.build();
	}
}