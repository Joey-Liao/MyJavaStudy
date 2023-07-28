# Kafka性能全景

![image-20230516202435092](D:\Typora\workspace\Kafka\kafka高效的原因.assets\image-20230516202435092.png)

从性能瓶颈上看，逃不出3个问题

- 磁盘
- 网络
- 复杂度

对于kafka这种网络分布式队列来说，**磁盘**和**网络**更是优化的重点。争对上述问题，解决方法可以抽象为：

- 并发
- 压缩
- 批量
- 缓存
- 算法

知道了问题和解决办法，kafka中有哪些角色可以成为优化点

- Producer
- Consumer
- Broker

这就是思考方式。`提出问题` > `列出问题点` > `列出优化方法` > `列出具体可切入的点` > `tradeoff和细化实现`。

![image-20230516215150589](D:\Typora\workspace\Kafka\kafka高效的原因.assets\image-20230516215150589.png)



# 优化生产消息

## Kafka批量与压缩

Kafka Producer 向 Broker 发送消息不是一条消息一条消息的发送。使用过 Kafka 的同学应该知道，Producer 有两个重要的参数：`batch.size`和`linger.ms`。这两个参数就和 Producer 的批量发送有关。

![image-20230516212646307](D:\Typora\workspace\Kafka\kafka高效的原因.assets\image-20230516212646307.png)

发送消息依次经过以下处理器：

- Serialize：键和值都根据传递的序列化器进行序列化。优秀的序列化方式可以提高网络传输的效率。
- Partition：决定将消息写入主题的哪个分区，默认情况下遵循 murmur2 算法。自定义分区程序也可以传递给生产者，以控制应将消息写入哪个分区。
- Compress：默认情况下，在 Kafka 生产者中不启用压缩.Compression 不仅可以更快地从生产者传输到代理，还可以在复制过程中进行更快的传输。压缩有助于提高吞吐量，降低延迟并提高磁盘利用率。
- Accumulate：`Accumulate`顾名思义，就是一个消息累计器。其内部为每个 Partition 维护一个`Deque`双端队列，队列保存将要发送的批次数据，`Accumulate`将数据累计到一定数量，或者在一定过期时间内，便将数据以批次的方式发送出去。记录被累积在主题每个分区的缓冲区中。根据生产者批次大小属性将记录分组。主题中的每个分区都有一个单独的累加器 / 缓冲区。
- Group Send：记录累积器中分区的批次按将它们发送到的代理分组。批处理中的记录基于 batch.size 和 linger.ms 属性发送到代理。记录由生产者根据两个条件发送。当达到定义的批次大小或达到定义的延迟时间时。

# 优化存储消息

## Kafka 网络模型 I/O多路复用

先引用 Kafka 2.8.0 源码里 SocketServer 类中一段很关键的注释：

> ![图片](https://mmbiz.qpic.cn/mmbiz_png/AaabKZjib2kYbxGGME9DnsLibIAX9X7C1ia1PVOctU1M5SmkRjWLGVGHl3txP1WVVaDVPcDo4bPRFbdEhyxRnBP7w/640?wx_fmt=png&wxfrom=5&wx_lazy=1&wx_co=1)

Kafka 自己实现了网络模型做 RPC。底层基于 Java NIO，采用和 Netty 一样的 Reactor 线程模型。

![image-20230516215510227](D:\Typora\workspace\Kafka\kafka高效的原因.assets\image-20230516215510227.png)



通俗点记忆就是 1 + N + M

> 1：表示 1 个 Acceptor 线程，负责监听新的连接，然后将新连接交给 Processor 线程处理。
>
> N：表示 N 个 Processor 线程，每个 Processor 都有自己的 selector，负责从 socket 中读写数据。
>
> M：表示 M 个 KafkaRequestHandler 业务处理线程，它通过调用 KafkaApis 进行业务处理，然后生成 response，再交由给 Processor 线程。

对于 IO 有所研究的同学，应该清楚：Reactor 模式正是采用了很经典的 IO 多路复用技术，它可以复用一个线程去处理大量的 Socket 连接，从而保证高性能。Netty 和 Redis 为什么能做到十万甚至百万并发？它们其实都采用了 Reactor 网络通信模型。

Reacotr 模型主要分为三个角色

- Reactor：把 IO 事件分配给对应的 handler 处理
- Acceptor：处理客户端连接事件
- Handler：处理非阻塞的任务

在传统阻塞 IO 模型中，每个连接都需要独立线程处理，当并发数大时，创建线程数多，占用资源；采用阻塞 IO 模型，连接建立后，若当前线程没有数据可读，线程会阻塞在读操作上，造成资源浪费

针对传统阻塞 IO 模型的两个问题，Reactor 模型基于池化思想，避免为每个连接创建线程，连接完成后将业务处理交给线程池处理；基于 IO 复用模型，多个连接共用同一个阻塞对象，不用等待所有的连接。遍历到有新数据可以处理时，操作系统会通知程序，线程跳出阻塞状态，进行业务逻辑处理

![image-20230516212334114](D:\Typora\workspace\Kafka\kafka高效的原因.assets\image-20230516212334114.png)

其中包含了一个`Acceptor`线程，用于处理新的连接，`Acceptor` 有 N 个 `Processor` 线程 select 和 read socket 请求，N 个 `Handler` 线程处理请求并响应，即处理业务逻辑。



## Kafka分区并发

Kafka 的 Topic 可以分成多个 Partition，每个 Paritition 类似于一个队列，保证数据有序。同一个 Group 下的不同 Consumer 并发消费 Paritition，分区实际上是调优 Kafka 并行度的最小单元，因此，可以说，每增加一个 Paritition 就增加了一个消费并发。

![image-20230516213816109](D:\Typora\workspace\Kafka\kafka高效的原因.assets\image-20230516213816109.png)

## 是不是分区数越多越好呢？

- 越多的分区需要打开更多的文件句柄
- 客户端 / 服务器端需要使用的内存就越多：客户端 producer 有个参数 batch.size，默认是 16KB。它会为每个分区缓存消息，一旦满了就打包将消息批量发出。
- 降低高可用性：分区越多，每个 Broker 上分配的分区也就越多，当一个发生 Broker 宕机，那么恢复时间将很长。



## Kafka顺序写

一般来说写磁盘很慢，完成一次磁盘 IO，需要经过`寻道`、`旋转`和`数据传输`三个步骤。其中`寻道`、`旋转`占用了绝大多数时间，

![image-20230516203020264](D:\Typora\workspace\Kafka\kafka高效的原因.assets\image-20230516203020264.png)

因此，如果在写磁盘的时候省去`寻道`、`旋转`可以极大地提高磁盘读写的性能。

Kafka 采用`顺序写`文件的方式来提高磁盘写入性能。`顺序写`文件，基本减少了磁盘`寻道`和`旋转`的次数。磁头再也不用在磁道上乱舞了，而是一路向前飞速前行。

Kafka 中每个分区是一个有序的，不可变的消息序列，新的消息不断追加到 Partition 的末尾，在 Kafka 中 Partition 只是一个逻辑概念，Kafka 将 Partition 划分为多个 Segment，每个 Segment 对应一个物理文件，Kafka 对 segment 文件追加写，这就是顺序写文件。

## Kafka PageCache

![image-20230516210057627](D:\Typora\workspace\Kafka\kafka高效的原因.assets\image-20230516210057627.png)

producer 生产消息到 Broker 时，Broker 会使用 fwrite() 系统调用【对应到 Java NIO 的 FileChannel.write() API】按偏移量写入数据，此时数据都会先写入`page cache`。



consumer 消费消息时，Broker 使用 sendfile() 系统调用【对应 FileChannel.transferTo() API】，零拷贝地将数据从 page cache 传输到 broker 的 Socket buffer，再通过网络传输。

`page cache`中的数据会随着内核中 flusher 线程的调度以及对 sync()/fsync() 的调用写回到磁盘，就算进程崩溃，也不用担心数据丢失。另外，如果 consumer 要消费的消息不在`page cache`里，才会去磁盘读取，并且会顺便预读出一些相邻的块放入 page cache，以方便下一次读取。

因此如果 Kafka producer 的生产速率与 consumer 的消费速率相差不大，那么就能几乎只靠对 broker page cache 的读写完成整个生产 - 消费过程，磁盘访问非常少。





# 优化消费消息

## Kafka零拷贝

我们从 Kafka 的场景来看，Kafka Consumer 消费存储在 Broker 磁盘的数据，从读取 Broker 磁盘到网络传输给 Consumer，期间涉及哪些系统交互

下面这个过程是不采用零拷贝技术时，从磁盘中读取文件然后通过网卡发送出去的流程，可以看到：经历了 4 次拷贝，4 次上下文切换。

![](D:\Typora\workspace\Kafka\kafka高效的原因.assets\image-20230516220924040.png)

如果采用零拷贝技术（底层通过 sendfile 方法实现），流程将变成下面这样。可以看到：只需 3 次拷贝以及 2 次上下文切换，显然性能更高。

![](D:\Typora\workspace\Kafka\kafka高效的原因.assets\image-20230516220950699.png)



1.  第一次：读取磁盘文件到操作系统内核缓冲区；
2.  第二次：将内核缓冲区的数据，copy 到应用程序的 buffer；
3.  第三步：将应用程序 buffer 中的数据，copy 到 socket 网络发送缓冲区；
4.  第四次：将 socket buffer 的数据，copy 到网卡，由网卡进行网络传输。

`零拷贝`技术，英文——`Zero-Copy`。`零拷贝`就是尽量去减少上面数据的拷贝次数，从而减少拷贝的 CPU 开销，减少用户态内核态的上下文切换次数，从而优化数据传输的性能。

常见的零拷贝思路主要有三种：

- 直接 I/O：数据直接跨过内核，在用户地址空间与 I/O 设备之间传递，内核只是进行必要的虚拟存储配置等辅助工作；

- 避免内核和用户空间之间的数据拷贝：当应用程序不需要对数据进行访问时，则可以避免将数据从内核空间拷贝到用户空间；

- 写时复制：数据不需要提前拷贝，而是当需要修改的时候再进行部分拷贝。





Kafka 使用到了 `mmap` 和 `sendfile` 的方式来实现`零拷贝`。分别对应 Java 的 `MappedByteBuffer` 和 `FileChannel.transferTo`。

使用 Java NIO 实现`零拷贝`，如下：

```java
FileChannel.transferTo()
```

![image-20230516204512974](D:\Typora\workspace\Kafka\kafka高效的原因.assets\image-20230516204512974.png)



![image-20230516205805940](D:\Typora\workspace\Kafka\kafka高效的原因.assets\image-20230516205805940.png)

我们将**副本数从四减少到三**，并且这些副本中只有一个涉及 CPU。我们还将**上下文切换的数量从四个减少到了两个**。这是一个很大的改进，但是还没有查询零副本。当运行 Linux 内核 2.4 及更高版本以及支持收集操作的网络接口卡时，后者可以作为进一步的优化来实现。如下所示。

![image-20230516205939563](D:\Typora\workspace\Kafka\kafka高效的原因.assets\image-20230516205939563.png)

根据前面的示例，调用`transferTo()`方法会使设备通过 DMA 引擎将数据读取到内核读取缓冲区中。但是，使用`gather`操作时，读取缓冲区和套接字缓冲区之间没有复制。取而代之的是，给 NIC 一个指向读取缓冲区的指针以及偏移量和长度，该偏移量和长度由 DMA 清除。CPU 绝对不参与复制缓冲区。

## 稀疏索引

如何提高读性能，大家很容易想到的是：索引。Kafka 所面临的查询场景其实很简单：能按照 offset 或者 timestamp 查到消息即可。

如果采用 B Tree 类的索引结构来实现，每次数据写入时都需要维护索引（属于随机 IO 操作），而且还会引来「页分裂」这种比较耗时的操作。而这些代价对于仅需要实现简单查询要求的 Kafka 来说，显得非常重。所以，B Tree 类的索引并不适用于 Kafka。

相反，哈希索引看起来却非常合适。为了加快读操作，如果只需要在内存中维护一个「从 offset 到日志文件偏移量」的映射关系即可，每次根据 offset 查找消息时，从哈希表中得到偏移量，再去读文件即可。（根据 timestamp 查消息也可以采用同样的思路）

但是哈希索引常驻内存，显然没法处理数据量很大的情况，Kafka 每秒可能会有高达几百万的消息写入，一定会将内存撑爆。

可我们发现消息的 offset 完全可以设计成有序的（实际上是一个单调递增 long 类型的字段），这样消息在日志文件中本身就是有序存放的了，我们便没必要为每个消息建 hash 索引了，完全可以将消息划分成若干个 block，只索引每个 block 第一条消息的 offset 即可，先根据大小关系找到 block，然后在 block 中顺序搜索，这便是 Kafka “稀疏索引” 的设计思想。

![image-20230516220659561](D:\Typora\workspace\Kafka\kafka高效的原因.assets\image-20230516220659561.png)



## mmap

利用稀疏索引，已经基本解决了高效查询的问题，但是这个过程中仍然有进一步的优化空间，那便是通过 mmap（memory mapped files） 读写上面提到的稀疏索引文件，进一步提高查询消息的速度。

> 注意：mmap 和 page cache 是两个概念，网上很多资料把它们混淆在一起。此外，还有资料谈到 Kafka 在读 log 文件时也用到了 mmap，通过对 2.8.0 版本的源码分析，这个信息也是错误的，其实只**有索引文件的读写才用到了 mmap.**



究竟如何理解 mmap？前面提到，**常规的文件操作为了提高读写性能，使用了 Page Cache 机制，但是由于页缓存处在内核空间中，不能被用户进程直接寻址，所以读文件时还需要通过系统调用，将页缓存中的数据再次拷贝到用户空间中。**



而采用 mmap 后，它将**磁盘文件与进程虚拟地址做了映射**，并不会招致系统调用，以及额外的内存 copy 开销，从而提高了文件读取效率。

![image-20230516220834166](D:\Typora\workspace\Kafka\kafka高效的原因.assets\image-20230516220834166.png)



> mmap 有多少字节可以映射到内存中与地址空间有关，32 位的体系结构只能处理 4GB 甚至更小的文件。Kafka 日志通常足够大，可能一次只能映射部分，因此读取它们将变得非常复杂。然而，索引文件是稀疏的，它们相对较小。将它们映射到内存中可以加快查找过程，这是内存映射文件提供的主要好处。

# 文件结构

Kafka 消息是以 Topic 为单位进行归类，各个 Topic 之间是彼此独立的，互不影响。每个 Topic 又可以分为一个或多个分区。每个分区各自存在一个记录消息数据的日志文件。

Kafka 每个分区日志在物理上实际按大小被分成多个 Segment。

![image-20230516214107319](D:\Typora\workspace\Kafka\kafka高效的原因.assets\image-20230516214107319.png)

- segment file 组成：由 2 大部分组成，分别为 index file 和 data file，此 2 个文件一一对应，成对出现，后缀”.index”和“.log”分别表示为 segment 索引文件、数据文件。
- segment 文件命名规则：partion 全局的第一个 segment 从 0 开始，后续每个 segment 文件名为上一个 segment 文件最后一条消息的 offset 值。数值最大为 64 位 long 大小，19 位数字字符长度，没有数字用 0 填充。

index 采用稀疏索引，这样每个 index 文件大小有限，Kafka 采用`mmap`的方式，直接将 index 文件映射到内存，这样对 index 的操作就不需要操作磁盘 IO。`mmap`的 Java 实现对应 `MappedByteBuffer` 。

> mmap 是一种内存映射文件的方法。即将一个文件或者其它对象映射到进程的地址空间，实现文件磁盘地址和进程虚拟地址空间中一段虚拟地址的一一对映关系。实现这样的映射关系后，进程就可以采用指针的方式读写操作这一段内存，而系统会自动回写脏页面到对应的文件磁盘上，即完成了对文件的操作而不必再调用 read,write 等系统调用函数。相反，内核空间对这段区域的修改也直接反映用户空间，从而可以实现不同进程间的文件共享。

Kafka 充分利用二分法来查找对应 offset 的消息位置：

![image-20230516214323839](D:\Typora\workspace\Kafka\kafka高效的原因.assets\image-20230516214323839.png)

1. 按照二分法找到小于 offset 的 segment 的.log 和.index
2. 用目标 offset 减去文件名中的 offset 得到消息在这个 segment 中的偏移量。
3. 再次用二分法在 index 文件中找到对应的索引。
4. 到 log 文件中，顺序查找，直到找到 offset 对应的消息。

