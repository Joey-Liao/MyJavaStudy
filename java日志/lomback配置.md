# 一 SpringBoot整合lombok+Slf4j+logback日志框架

Slf4j有四个级别的log level可供选择，级别从上到下由低到高，**优先级高的将被打印出来。**

- **Debug**
  简单来说，对程序调试有利的信息都可以debug输出
- **info**
  对用户有用的信息
- **warn**
  可能会导致错误的信息
- **error**
  顾名思义，发生错误的地方

## 1、配置依赖

若使用springboot开发，需先将springboot-starter中自带的log排除掉

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

```xml
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.2.3</version>
</dependency>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>
```

## 2、配置logback

logback在启动时，根据以下步骤寻找配置文件：

在classpath中寻找logback-test.xml文件
如果找不到logback-test.xml，则在 classpath中寻找logback.groovy文件
如果找不到 logback.groovy，则在classpath中寻找logback.xml文件
如果上述的文件都找不到，则logback会使用JDK的SPI机制查找 META-INF/services/ch.qos.logback.classic.spi.Configurator中的 logback 配置实现类，这个实现类必须实现Configuration接口，使用它的实现来进行配置
如果上述操作都不成功，logback 就会使用它自带的 BasicConfigurator 来配置，并将日志输出到console。

在 src\main\resources 路径下创建logback.xml配置文件（仅供参考）。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--scan:
            当此属性设置为true时，配置文件如果发生改变，将会被重新加载，默认值为true。
scanPeriod:
            设置监测配置文件是否有修改的时间间隔，如果没有给出时间单位，默认单位是毫秒。当scan为true时，此属性生效。默认的时间间隔为1分钟。
debug:
            当此属性设置为true时，将打印出logback内部日志信息，实时查看logback运行状态。默认值为false。

configuration 子节点为 appender、logger、root

            -->
<configuration scan="true" scanPeriod="60 seconds" debug="false">
    <!--用于区分不同应用程序的记录-->
    <contextName>edu-cloud</contextName>
    <!--日志文件所在目录，如果是tomcat，如下写法日志文件会在则为${TOMCAT_HOME}/bin/logs/目录下-->
    <property name="LOG_HOME" value="logs"/>

    <!--控制台-->
    <appender name="stdout" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <!--格式化输出：%d表示日期，%thread表示线程名，%-5level：级别从左显示5个字符宽度 %logger输出日志的logger名 %msg：日志消息，%n是换行符 -->
            <pattern>[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%thread] %-5level %logger{36} : %msg%n</pattern>
            <!--解决乱码问题-->
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!--滚动文件-->
    <appender name="infoFile" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <!-- ThresholdFilter:临界值过滤器，过滤掉 TRACE 和 DEBUG 级别的日志 -->
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>INFO</level>
        </filter>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_HOME}/log.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory><!--保存最近30天的日志-->
        </rollingPolicy>
        <encoder>
            <charset>UTF-8</charset>
            <pattern>[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%thread] %-5level %logger{36} : %msg%n</pattern>
        </encoder>
    </appender>

    <!--滚动文件-->
    <appender name="errorFile" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <!-- ThresholdFilter:临界值过滤器，过滤掉 TRACE 和 DEBUG 级别的日志 -->
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>error</level>
        </filter>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_HOME}/error.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory><!--保存最近30天的日志-->
        </rollingPolicy>
        <encoder>
            <charset>UTF-8</charset>
            <pattern>[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%thread] %-5level %logger{36} : %msg%n</pattern>
        </encoder>
    </appender>

    <!--将日志输出到logstack-->
    <!--<appender name="logstash" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>47.93.173.81:7002</destination>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <charset>UTF-8</charset>
        </encoder>
        <keepAliveDuration>5 minutes</keepAliveDuration>
    </appender>-->

    <!--这里如果是info，spring、mybatis等框架则不会输出：TRACE < DEBUG < INFO <  WARN < ERROR-->
    <!--root是所有logger的祖先，均继承root，如果某一个自定义的logger没有指定level，就会寻找
    父logger看有没有指定级别，直到找到root。-->
    <root level="debug">
        <appender-ref ref="stdout"/>
        <appender-ref ref="infoFile"/>
        <appender-ref ref="errorFile"/>
        <!-- <appender-ref ref="logstash"/>-->
    </root>

    <!--为某个包单独配置logger

    比如定时任务，写代码的包名为：com.seentao.task
    步骤如下：
    1、定义一个appender，取名为task（随意，只要下面logger引用就行了）
    appender的配置按照需要即可


    2、定义一个logger:
    <logger name="com.seentao.task" level="DEBUG" additivity="false">
      <appender-ref ref="task" />
    </logger>
    注意：additivity必须设置为false，这样只会交给task这个appender，否则其他appender也会打印com.seentao.task里的log信息。

    3、这样，在com.seentao.task的logger就会是上面定义的logger了。
    private static Logger logger = LoggerFactory.getLogger(Class1.class);
    -->

</configuration>
```

在进行配置的时候，主要要理解或记住以下几点（主要标签的用处）：

appender: 负责定义日志的输出目的地（控制台、日志文件、滚动日志文件，其他如logstash等）。

encoder: 负责定义日志的输出样式和字符编码，如果在控制台出现 ??? 或乱码，则指定编码（一般是UTF-8 ）就好了。

filter: 负责过滤日志，即使logger传来了info级别以上的日志，如果filter中设定了级别为info，则该appender只会将info级别及以上的日志输出到目的地。

rollingPolicy: 负责制定日志文件的滚动规则，是根据日志文件大小还是根据日期进行滚动。

logger: 负责定义我们实际代码中使用的logger。logger中有一个非常重要的属性name，name必须指定。在logback中，logger有继承关系，而所有的logger的祖先是root。


举个例子，如果我们有个类叫UserService，所在的包为com.maxwell.service，在UserService中要打印日志，我们一般会这么写：

```java
①private  Logger logger = LoggerFactory.getLogger(UserService.class);
或者
②private  Logger logger = LoggerFactory.getLogger("com.maxwell.service.UserService");
```


这两种写法是一样的，第①中写法实际会转化为②中的方式来获取一个logger实例。
当我们写下这行代码时，logback会依次检查以下各个logger实例的是否存在，如果不存在则依次创建：

```java
com
com.maxwell
com.maxwell.service
com.maxwell.service.UserService
而创建logger实例的时候，就会去配置文件中查找名字为com、com.maxwell、com.maxwell.service、com.maxwell.service.UserService的logge标签，并根据其中定义的规则创建。所以，假如你在配置文件中没有定义name为上述字符串的logger时，就会找到root这个祖先，根据root标签定义的规则创建logger实例。
```

理解了上面的这一点，想要实现某个包或者某个类单独输出到某日志文件的需求就很好实现了：

定义一个appender，指明日志文件的输出目的地；
定义一个logger，name设为那个包或类的全路径名。如果只想将这个类或包的日志输出到刚才定义的appender中，就将additivity设置为false。
还有就是，上面的参考配置可以保证mybatis的sql语句、spring的日志正常输出到控制台，但由于mybatis的sql语句输出的级别为debug，所以不会输出到name为infoFile的appender中，因为该appender中设置了级别为info的过滤器filter，如果想将mybatis的sql语句也输出到日志文件中，请将info改为debug。
也就是，一条日志想要顺利到达输出目的地，除了logger的级别要低于该级别，appender中的filter中设置的级别也要低于该级别。（TRACE < DEBUG < INFO < WARN < ERROR）

## 3、使用

在需要日志的地方加上`@Slf4j`，使用`log.xxx`即可

```java
@Slf4j
public class ProviderStart {
 
    public static void main(String[] args) {
 
        log.debug("输出DEBUG级别日志");
        log.info("输出INFO级别日志");
        log.warn("输出WARN级别日志");
        log.error("输出ERROR级别日志");
 
    }
}
```

