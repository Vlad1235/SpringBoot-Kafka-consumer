package com.learnkafka.config;


import com.learnkafka.service.LibraryEventsService;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
@EnableKafka // чтобы consumer смог прочитать данные из application.yml
@Slf4j
public class LibraryEventsConsumerConfig {


  @Autowired
  LibraryEventsService libraryEventsService;

  @Bean
  ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
      ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
      ConsumerFactory<Object, Object> kafkaConsumerFactory) {
    ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
    configurer.configure(factory, kafkaConsumerFactory);
    /*
    Group-id one but many listeners
     */
    factory.setConcurrency(3); // configure multiple consumer listeners of same application( they are all in one group-id)
    // factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL); // если хотим manual offset management
    factory.setErrorHandler(((thrownException, data) -> {
      log.info("Exception in consumerConfig is {} and the record is {}", thrownException.getMessage(), data);
      //persist
    }));

    /*
    Retry logic
    Если не удасться прочитать сообщения, данная логика позволит не выпдать в ошибку, а провобовать
     еще запрашивать указанное количество раз
     */
    factory.setRetryTemplate(retryTemplate());

    /*
    Recovery logic
     */
    factory.setRecoveryCallback((context -> {
      if (context.getLastThrowable().getCause() instanceof RecoverableDataAccessException) {
        //invoke recovery logic
        log.info("Inside the recoverable logic");
        ConsumerRecord<Integer, String> consumerRecord = (ConsumerRecord<Integer, String>) context.getAttribute("record");
        libraryEventsService.handleRecovery(consumerRecord);
      } else {
        log.info("Inside the non recoverable logic");
        throw new RuntimeException(context.getLastThrowable().getMessage());
      }
      return null;
    }));
    return factory;
  }

  private RetryTemplate retryTemplate() {
    FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
    fixedBackOffPolicy.setBackOffPeriod(1000);
    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setRetryPolicy(simpleRetryPolicy());
    retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
    return retryTemplate;
  }

  private RetryPolicy simpleRetryPolicy() {
    Map<Class<? extends Throwable>, Boolean> exceptionsMap = new HashMap<>();
    exceptionsMap.put(IllegalArgumentException.class, false); // dont want to retry
    exceptionsMap.put(RecoverableDataAccessException.class, true); // want to retry this type of exception
    SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(3, exceptionsMap, true);
    return simpleRetryPolicy;
  }
}