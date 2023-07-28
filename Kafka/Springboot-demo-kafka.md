# 1. 环境配置

## 1.1 添加项目依赖

```java
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
```



## 1.2 添加配置文件application.yml

```xml
spring:
  kafka:
    bootstrap-servers: 192.168.10.102:9092,192.168.10.102:9093,192.168.10.102:9094  #bootstrap-servers：连接kafka的地址，多个地址用逗号分隔
    consumer:
      group-id: myGroup
      enable-auto-commit: true
      auto-commit-interval: 100ms # 提交offset延时(接收到消息后多久提交offset)
      auto-offset-reset: latest
      max-poll-records: 50 # 批量消费每次最多消费多少条消息
      properties:
        session.timeout.ms: 15000
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer


    producer:
      retries: 0 #若设置大于0的值，客户端会将发送失败的记录重新发送
      batch-size: 16384 #当将多个记录被发送到同一个分区时， Producer 将尝试将记录组合到更少的请求中。这有助于提升客户端和服务器端的性能。这个配置控制一个批次的默认大小（以字节为单位）。16384是缺省的配置
      buffer-memory: 33554432 #Producer 用来缓冲等待被发送到服务器的记录的总字节数，33554432是缺省配置
      key-serializer: org.apache.kafka.common.serialization.StringSerializer #关键字的序列化类
      value-serializer: org.apache.kafka.common.serialization.StringSerializer #值的序列化类
      acks: 1 # 应答级别:多少个分区副本备份完成时向生产者发送ack确认(可选0、1、all/-1)

#      transaction-id-prefix: tx- # 开启kafka事务 再使用@Transactional注解在方法上即可

#      设置分区
#      properties:
#        partitioner:
#          class: com.lzy.springbootdemo.utils.CustomizePartitioner

#    listener:
#      type: batch # 设置批量消费
#      missing-topics-fatal: false # 消费端监听的topic不存在时，项目启动会报错(关掉)

```

## 1.3 添加配置文件（可选）

```java
package com.lzy.springbootdemo.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConsumerAwareListenerErrorHandler;

@Configuration
public class KafkaConfig {
    @Bean
    public NewTopic initTopic(){
        return new NewTopic("topic-test-lzy",3,(short)2);
    }

    @Bean
    public NewTopic updataTopic(){
        return new NewTopic("topic-test-lzy-1",4,(short) 2);
    }

    // 新建一个异常处理器，用@Bean注入
    @Bean
    public ConsumerAwareListenerErrorHandler consumerAwareErrorHandler() {
        return (message, exception, consumer) -> {
            System.out.println("消费异常："+message.getPayload());
            return null;
        };
    }

}
```

# 2. 生产者与消费者

## 2.1 kafka的生产者,不带回调函数

```java
    /**
     * kafka的生产者,不带回调函数
     * @param message
     * @param session
     * @return
     */
    @GetMapping("/kafka/normal/{message}")
    public String sendMessage1(@PathVariable("message") String message, HttpSession session){
        kafkaTemplate.send("topic-test-lzy",message);
        return "ok";
    }
```

访问路径：localhost:8080/kafka/normal/aaa



## 2.2 带回调函数的生产者

```java
    /**
     * 带有回调函数的kafka生产者
     * @param callbackMessage
     */
    @GetMapping("/kafka/callback1/{message}")
    public void sendMessage2(@PathVariable("message") String callbackMessage) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send("my-topic", callbackMessage);
        future.thenAccept(result->{
            // 消息发送到的topic
            String topic = result.getRecordMetadata().topic();
            // 消息发送到的分区
            int partition = result.getRecordMetadata().partition();
            // 消息在分区内的offset
            long offset = result.getRecordMetadata().offset();
            System.out.println("发送消息成功:" + topic + "-" + partition + "-" + offset);
        });
        future.exceptionally(e->{
            e.printStackTrace();
            System.out.println("发送消息失败:" + e.getMessage());
            return null;
        });
    }
```

## 2.3 自定义分区器

kafka中每个topic被划分为多个分区，那么生产者将消息发送到topic时，具体要追加到哪个分区？这就是分区策略，Kafka 为我们提供了默认的分区策略，同时它也支持自定义分区策略。其路由机制为：

① 若发送消息时指定了分区（即自定义分区策略），则直接将消息append到指定分区；

② 若发送消息时未指定 patition，但指定了 key（kafka允许为每条消息设置一个key），则对key值进行hash计算，根据计算结果路由到指定分区，这种情况下可以保证同一个 Key 的所有消息都进入到相同的分区；

③  patition 和 key 都未指定，则使用kafka默认的分区策略，轮询选出一个 patition；

我们自定义一个分区策略，将消息发送到我们指定的partition，首先新建一个分区器类实现Partitioner接口，重写方法，其中partition方法的返回值就表示将消息发送到几号分区，

```java
public class CustomizePartitioner implements Partitioner {
    @Override
    public int partition(String s, Object o, byte[] bytes, Object o1, byte[] bytes1, Cluster cluster) {
        //自定义分区规则（这里假设全部发到0号分区）

        return 0;
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> map) {

    }
}
```

在application.propertise中配置自定义分区器，配置的值就是分区器类的全路径名，

```yaml
spring.kafka.producer.properties.partitioner.class=com.felix.kafka.producer.CustomizePartitioner
```

## 2.4 kafka事务提交



**第一种**

```java
    // 第一种 配置事务
    @GetMapping("/kafka/transaction")
    public void sendMessage7(){
        //使用executeInTransaction 需要在yml中配置事务参数，配置成功才能使用executeInTransaction方法，且运行报错后回滚
        kafkaTemplate.executeInTransaction(operations->{
            operations.send("topic1","test executeInTransaction");
            throw new RuntimeException("fail");
        });

        //没有在yml配置事务，这里就会出现消息发送成功，异常也出现了。如果配置事务，则改用executeInTransaction 替代send方法
        kafkaTemplate.send("topic1","test executeInTransaction");
        throw new RuntimeException("fail");
    }
```

**第二种**

```java
//第二种 配置事务 （注解方式）
    // [1] 需要在yml 配置 transaction-id-prefix: kafka_tx.
    // [2] 在方法上添加@Transactional(rollbackFor = RuntimeException.class)  做为开启事务并回滚
    // [3] 在注解方式中 任然可以使用.send方法，不需要使用executeInTransaction方法
    @GetMapping("/send2/{input}")
    @Transactional(rollbackFor = RuntimeException.class)
    public String sendToKafka2(@PathVariable String input){
//        this.template.send(topic,input);
        //事务的支持

        kafkaTemplate.send("topic1",input);
        if("error".equals(input))
        {
            throw new RuntimeException("input is error");
        }
        kafkaTemplate.send("topic1",input+"anthor");

        return "send success!"+input;

    }
```

需要在yml文件里配置

```yaml
      transaction-id-prefix: tx- # 开启kafka事务 再使用@Transactional注解在方法上即可
```

## 2.5 kafka的消费者

```java
 //监听器用@KafkaListener注解，topics表示监听的topic，支持同时监听多个，用英文逗号分隔。启动项目，postman调接口触发生产者发送消息，
    @KafkaListener(topics = {"topic-test-lzy"})
    public void onMessage1(ConsumerRecord<?, ?> record) {
        // 消费的哪个topic、partition的消息,打印出消息内容
        System.out.println("简单消费Topic：" + record.topic() + "**分区" + record.partition() + "**值内容" + record.value());
    }
```

指定topic、partition、offset消费

前面我们在监听消费topic1的时候，监听的是topic1上所有的消息，如果我们想指定topic、指定partition、指定offset来消费呢？也很简单，@KafkaListener注解已全部为我们提供。

```java
/**
     * @param record
     * @Title 指定topic、partition、offset消费
     * @Description 同时监听topic1和topic2，监听topic1的0号分区、
     * topic2的 "0号和1号" 分区，指向1号分区的offset初始值为8
     */
    @KafkaListener(id = "consumer1", groupId = "lzy-group", topicPartitions = {
            @TopicPartition(topic = "topic-test-lzy", partitions = {"0"}),
            @TopicPartition(topic = "topic2", partitions = "0"
                    //,partitionOffsets = @PartitionOffset(partition = "1", initialOffset = "8")
            )
    })
    public void onMessage2(ConsumerRecord<?, ?> record) {
        System.out.println("-------------------------");
        System.out.println("topic:" + record.topic() + ",partition:" + record.partition() + ",offset:" + record.offset() + "value:" + record.value());
    }
```

属性解释：

① id：消费者ID；

② groupId：消费组ID；

③ topics：监听的topic，可监听多个；

④ topicPartitions：可配置更加详细的监听信息，可指定topic、parition、offset监听。

上面onMessage2监听的含义：监听topic1的0号分区，同时监听topic2的0号分区和topic2的1号分区里面offset从8开始的消息。

注意：topics和topicPartitions不能同时使用；



## 2.6 批量消费



设置application.yml开启批量消费即可，

```yaml
# 设置批量消费 spring.kafka.listener.type=batch 
# 批量消费每次最多消费多少条消息 spring.kafka.consumer.max-poll-records=50
```

接收消息时用List来接收，监听代码如下，

```java
    /**
     * 批量消费 （和普通消费只能2选1）
     * @param records
     */
    @KafkaListener(id="consumer2",groupId = "felix-group",topics = "topic1" )
    public void onMesssage(List<ConsumerRecord<?,?>> records){
        System.out.println(">>>批量消费一次，records.size()="+records.size());
        for(ConsumerRecord<?,?> record:records){
            System.out.println(record.value());
        }
    }
```

## 2.7 ConsumerAwareListenerErrorHandler异常处理器

通过异常处理器，我们可以处理consumer在消费时发生的异常。

新建一个 ConsumerAwareListenerErrorHandler 类型的异常处理方法，用@Bean注入，BeanName默认就是方法名，然后我们将这个异常处理器的BeanName放到@KafkaListener注解的errorHandler属性里面，当监听抛出异常的时候，则会自动调用异常处理器，

```java
    // 新建一个异常处理器，用@Bean注入
    @Bean
    public ConsumerAwareListenerErrorHandler consumerAwareErrorHandler() {
        return (message, exception, consumer) -> {
            System.out.println("消费异常："+message.getPayload());
            return null;
        };
    }
```

```java
// 将这个异常处理器的BeanName放到@KafkaListener注解的errorHandler属性里面
    @KafkaListener(topics = {"topic-test-lzy"},errorHandler = "consumerAwareErrorHandler")
    public void onMessage4(ConsumerRecord<?, ?> record) throws Exception {
        throw new Exception("简单消费-模拟异常");
    }

    // 批量消费也一样，异常处理器的message.getPayload()也可以拿到各条消息的信息
    @KafkaListener(topics = "topic1",errorHandler="consumerAwareErrorHandler")
    public void onMessage5(List<ConsumerRecord<?, ?>> records) throws Exception {
        System.out.println("批量消费一次...");
        throw new Exception("批量消费-模拟异常");
    }
```

## 2.8 消息过滤器

消息过滤器可以在消息抵达consumer之前被拦截，在实际应用中，我们可以根据自己的业务逻辑，筛选出需要的信息再交由KafkaListener处理，不需要的消息则过滤掉。

配置消息过滤只需要为 监听器工厂 配置一个RecordFilterStrategy（消息过滤策略），返回true的时候消息将会被抛弃，返回false时，消息能正常抵达监听容器。 @Component public class KafkaConsumer { @Autowired ConsumerFactory consumerFactory;

```java
@Component
public class KafkaConsumer {

    @Autowired
    ConsumerFactory consumerFactory;

    //消息过滤器
    @Bean
    public ConcurrentKafkaListenerContainerFactory filterContainerFactory(){
        ConcurrentKafkaListenerContainerFactory factory=new ConcurrentKafkaListenerContainerFactory();
        factory.setConsumerFactory(consumerFactory);
        //被过滤器的消息将被丢弃
        factory.setAckDiscarded(true);
        //消息过滤策略
        factory.setRecordFilterStrategy(consumerRecord -> {
            if(Integer.parseInt(consumerRecord.value().toString())%2==0){
                return false;
            }
            //返回true消息则被过滤
            return true;
        });
        return factory;
    }

    //消息过滤监听
    @KafkaListener(topics = {"topic1"},containerFactory = "filterContainerFactory")
    public void onMessage6(ConsumerRecord<?,?> record){
        System.out.println(record.value());
    }
}

```

上面实现了一个"过滤奇数、接收偶数"的过滤策略，我们向topic1发送0-99总共100条消息，看一下监听器的消费情况，可以看到监听器只消费了偶数，



## 2.9 消息转发

在实际开发中，我们可能有这样的需求，应用A从TopicA获取到消息，经过处理后转发到TopicB，再由应用B监听处理消息，即一个应用处理完成后将该消息转发至其他应用，完成消息的转发。

在SpringBoot集成Kafka实现消息的转发也很简单，只需要通过一个@SendTo注解，被注解方法的return值即转发的消息内容，如下

```java
    /**
     * @Title 消息转发
     * @Description 从topic1接收到的消息经过处理后转发到topic2
     * @param record
     * @return
     */
    @KafkaListener(topics = {"topic"})
    @SendTo("topic2")
    public String onMessage7(ConsumerRecord<?,?> record){
        return record.value()+"-forward message";
    }

```

## 2.10 定时启动，停止监听器

默认情况下，当消费者项目启动的时候，监听器就开始工作，监听消费发送到指定topic的消息，那如果我们不想让监听器立即工作，想让它在我们指定的时间点开始工作，或者在我们指定的时间点停止工作，该怎么处理呢——使用KafkaListenerEndpointRegistry，下面我们就来实现：

① 禁止监听器自启动；

② 创建两个定时任务，一个用来在指定时间点启动定时器，另一个在指定时间点停止定时器；

新建一个定时任务类，用注解@EnableScheduling声明，KafkaListenerEndpointRegistry 在SpringIO中已经被注册为Bean，直接注入，设置禁止KafkaListener自启动，

```java
package com.lzy.springbootdemo.consumers;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling
@Component
public class CronTimer {
    /**
     * @KafkaListener注解所标注的方法并不会在IOC容器中被注册为Bean，
     * 而是会被注册在KafkaListenerEndpointRegistry中，
     * 而KafkaListenerEndpointRegistry在SpringIOC中已经被注册为Bean
     **/
    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Autowired
    private ConsumerFactory consumerFactory;

    // 监听器容器工厂(设置禁止KafkaListener自启动)
    @Bean
    public ConcurrentKafkaListenerContainerFactory delayContainerFactory() {
        ConcurrentKafkaListenerContainerFactory container = new ConcurrentKafkaListenerContainerFactory();
        container.setConsumerFactory(consumerFactory);
        //禁止KafkaListener自启动
        container.setAutoStartup(false);
        return container;
    }

    // 监听器
    @KafkaListener(id="timingConsumer",topics = "topic1",containerFactory = "delayContainerFactory")
    public void onMessage1(ConsumerRecord<?, ?> record){
        System.out.println("消费成功："+record.topic()+"-"+record.partition()+"-"+record.value());
    }

    // 定时启动监听器
    @Scheduled(cron = "0 42 11 * * ? ")
    public void startListener() {
        System.out.println("启动监听器...");
        // "timingConsumer"是@KafkaListener注解后面设置的监听器ID,标识这个监听器
        if (!registry.getListenerContainer("timingConsumer").isRunning()) {
            registry.getListenerContainer("timingConsumer").start();
        }
        //registry.getListenerContainer("timingConsumer").resume();
    }

    // 定时停止监听器
    @Scheduled(cron = "0 45 11 * * ? ")
    public void shutDownListener() {
        System.out.println("关闭监听器...");
        registry.getListenerContainer("timingConsumer").pause();
    }
}

```

启动项目，触发生产者向topic1发送消息，可以看到consumer没有消费，因为这时监听器还没有开始工作，
