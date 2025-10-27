package cn.sztu.questioncloud.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

@Configuration
public class RabbitMQConfig {
    public static final String USER_EXCHANGE_NAME = "user.topic";
    public static final String USER_QUEUE_NAME = "user.registered.topic";
    public static final String USER_REGISTERED_KEY = "user.registered";

    /**
     * 配置JacksonConverter
     */
    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory cf, MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setMessageConverter(converter);
        return factory;
    }


    /**
     * 声明交换机
     * durable:持久化，默认false
     * autoDelete:没有生产者/消费者使用此交换机，会自动删除
     */
    @Bean
    public TopicExchange userEventExchange() {
        return new TopicExchange(USER_EXCHANGE_NAME, true, false);
    }

    /**
     * 声明注册事件队列
     * durable:持久化，默认false
     * exclusive:只有能当前创建的连接使用，连接关闭后队列即被删除，默认false
     * autoDelete:没有生产者/消费者使用此交换机，会自动删除
     */
    @Bean
    public Queue userRegisteredQueue() {
        return new Queue(USER_QUEUE_NAME, true, false, false);
    }

    /**
     * 声明注册用户队列绑定关系
     * @param userEventExchange 交换机
     * @param userRegisteredQueue 队列
     * @return 绑定关系
     */
    @Bean
    public Binding userRegisteredBinding(TopicExchange userEventExchange,
                                         Queue userRegisteredQueue) {
        return BindingBuilder.bind(userRegisteredQueue)
                .to(userEventExchange)
                .with(USER_REGISTERED_KEY);
    }
}
