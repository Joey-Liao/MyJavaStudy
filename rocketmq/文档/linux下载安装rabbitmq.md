# linux安装RocketMQ

## 1.1 下载RocketMQ

下载地址：https://rocketmq.apache.org/dowloading/releases/

注意选择版本，这里我们选择4.9.2的版本，后面使用alibaba时对应

下载地址：

https://archive.apache.org/dist/rocketmq/4.9.2/rocketmq-all-4.9.2-bin-release.zip

## 1.2 上传服务器

在root目录下创建文件夹

```shell
  mkdir rocketmq  
```

将下载后的压缩包上传到阿里云服务器或者虚拟机中去

![image-20240130205540837](linux下载安装rabbitmq.assets/image-20240130205540837.png)

```shell
  unzip rocketmq-all-4.9.2-bin-release.zip  
```

如果你的服务器没有unzip命令，则下载安装一个

```shell
  yum install unzip  
```

目录分析

​                 ![image-20240130205636758](linux下载安装rabbitmq.assets/image-20240130205636758.png)     

- Benchmark：包含一些性能测试的脚本；

- Bin：可执行文件目录；

- Conf：配置文件目录；

- Lib：第三方依赖；

- LICENSE：授权信息;

- NOTICE：版本公告；

## 1.3 配置环境变量

```shell
  vim /etc/profile  
```

在文件末尾添加

```shell
  export NAMESRV_ADDR=**阿里云公网IP**:9876  
```

## 1.4 修改nameServer的运行脚本

进入bin目录下，修改runserver.sh文件,将71行和76行的Xms和Xmx等改小一点

```shell
  vim runserver.sh  
```

![image-20240130205835048](linux下载安装rabbitmq.assets/image-20240130205835048.png)

## 1.5 修改broker的运行脚本

进入bin目录下，修改runbroker.sh文件,修改67行

![image-20240130205856663](linux下载安装rabbitmq.assets/image-20240130205856663.png)

## 1.6 修改broker的配置文件

进入conf目录下，修改broker.conf文件

![image-20240130205914666](linux下载安装rabbitmq.assets/image-20240130205914666.png)

**添加参数解释**

**namesrvAddr**：nameSrv地址 可以写localhost因为nameSrv和broker在一个服务器

**autoCreateTopicEnable**：自动创建主题，不然需要手动创建出来

**brokerIP1**：broker也需要一个公网ip，如果不指定，那么是阿里云的内网地址，我们再本地无法连接使用

## 1.7 启动

首先在安装目录下创建一个logs文件夹，用于存放日志

```shell
mkdir logs
```

![image-20240130210054079](linux下载安装rabbitmq.assets/image-20240130210054079.png)

一次运行两条命令

启动nameSrv

```shell
  nohup sh bin/mqnamesrv >  ./logs/namesrv.log &  
```

启动broker 这里的-c是指定使用的配置文件

  

```shell
nohup sh bin/mqbroker -c  conf/broker.conf > ./logs/broker.log &  
```

查看启动结果

![image-20240130210140255](linux下载安装rabbitmq.assets/image-20240130210140255.png)

## 1.8 RocketMQ控制台的安装RocketMQ-Console

Rocketmq 控制台可以可视化MQ的消息发送！

旧版本源码是在rocketmq-external里的rocketmq-console，新版本已经单独拆分成dashboard

网址： https://github.com/apache/rocketmq-dashboard

下载地址：

https://github.com/apache/rocketmq-dashboard/archive/refs/tags/rocketmq-dashboard-1.0.0.zip

下载后解压出来，在跟目录下执行

  **mvn  clean package -Dmaven.test.skip=true**  

![image-20240130210223960](linux下载安装rabbitmq.assets/image-20240130210223960.png)

![image-20240130210234636](linux下载安装rabbitmq.assets/image-20240130210234636.png)

将jar包上传到服务器上去

![image-20240130210255509](linux下载安装rabbitmq.assets/image-20240130210255509.png)

然后运行

```shell
  nohup  java -jar ./rocketmq-dashboard-1.0.0.jar rocketmq.config.namesrvAddr=127.0.0.1:9876  > ./rocketmq-4.9.3/logs/dashboard.log & 
```

 命令拓展:--server.port指定运行的端口

--rocketmq.config.namesrvAddr=127.0.0.1:9876 指定namesrv地址

访问： [http://localhost:8001](http://localhost:8081) 

运行访问端口是8001，如果从官网拉下来打包的话，默认端口是8080

![image-20240130210346568](linux下载安装rabbitmq.assets/image-20240130210346568.png)

# docker安装RocketMQ

## 2.1 下载RockerMQ需要的镜像

```shell
  docker pull  rocketmqinc/rocketmq  
  docker pull  styletang/rocketmq-console-ng  
```

## 2.2 启动NameServer服务

### 2.2.1 创建NameServer数据存储路径

```shell
mkdir -p  /home/rocketmq/data/namesrv/logs /home/rocketmq/data/namesrv/store  
```

### 2.2.2 启动NameServer容器

```shell
docker run -d --name rmqnamesrv -p 9876:9876 -v /home/rocketmq/data/namesrv/logs:/root/logs -v /home/rocketmq/data/namesrv/store:/root/store -e "MAX_POSSIBLE_HEAP=100000000" rocketmqinc/rocketmq sh mqnamesrv
```

## 2.3 启动Broker服务

### 2.3.1 创建Broker数据存储路径 

```shell
mkdir -p  /home/rocketmq/data/broker/logs /home/rocketmq/data/broker/store  
```

### 2.3.2 创建conf配置文件目录

```shell
mkdir /home/rocketmq/conf
```

### 2.3.3 在配置文件目录下创建broker.conf配置文件 

```shell
# 所属集群名称，如果节点较多可以配置多个
brokerClusterName = DefaultCluster
#broker名称，master和slave使用相同的名称，表明他们的主从关系
brokerName = broker-a
#0表示Master，大于0表示不同的slave
brokerId = 0
#表示几点做消息删除动作，默认是凌晨4点
deleteWhen = 04
#在磁盘上保留消息的时长，单位是小时
fileReservedTime = 48
#有三个值：SYNC_MASTER，ASYNC_MASTER，SLAVE；同步和异步表示Master和Slave之间同步数据的机制；
brokerRole = ASYNC_MASTER
#刷盘策略，取值为：ASYNC_FLUSH，SYNC_FLUSH表示同步刷盘和异步刷盘；SYNC_FLUSH消息写入磁盘后才返回成功状态，ASYNC_FLUSH不需要；
flushDiskType = ASYNC_FLUSH
# 设置broker节点所在服务器的ip地址
brokerIP1 = 你服务器外网ip

```

### 2.3.4 启动Broker容器

```
docker run -d --name rmqbroker --link rmqnamesrv:namesrv  -p 10911:10911 -p 10909:10909 -v   /home/rocketmq/data/broker/logs:/root/logs -v  /home/rocketmq/data/broker/store:/root/store -v  /home/rocketmq/conf/broker.conf:/opt/rocketmq-4.4.0/conf/broker.conf  --privileged=true -e "NAMESRV_ADDR=namesrv:9876" -e  "MAX_POSSIBLE_HEAP=200000000" rocketmqinc/rocketmq sh mqbroker -c  /opt/rocketmq-4.4.0/conf/broker.conf  
```

## 2.4 启动Broker容器

```shell
docker run -d --name rmqadmin -e "JAVA_OPTS=-Drocketmq.namesrv.addr=47.95.167.234:9876 \
-Dcom.rocketmq.sendMessageWithVIPChannel=false \
-Duser.timezone='Asia/Shanghai'" -v /etc/localtime:/etc/localtime -p 9999:8080 styletang/rocketmq-console-ng
```

## 2.5 正常启动后的docker ps

![image-20240130211116532](linux下载安装rabbitmq.assets/image-20240130211116532.png)

![image-20240130211126668](linux下载安装rabbitmq.assets/image-20240130211126668.png)