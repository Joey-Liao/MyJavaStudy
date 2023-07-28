# JVM重要参数总结



![image-20230405154007154](D:\Typora\workspace\JVM\JVM重要参数总结.assets\image-20230405154007154-16806804119161.png)



## 1 堆内存相关

> Java 虚拟机所管理的内存中最大的一块，Java 堆是所有线程共享的一块内存区域，在虚拟机启动时创建。**此内存区域的唯一目的就是存放对象实例，几乎所有的对象实例以及数组都在这里分配内存。**



### 1.1 显示指定堆内存

与性能有关的最常见实践之一是根据应用程序要求初始化堆内存。如果我们需要指定最小和最大堆大小（推荐显示指定大小），以下参数可以帮助你实现：

```java
-Xms<heap size>[unit]
-Xmx<heap size>[unit]
```

- heap size 表示要初始化内存的具体大小
- unit表示要初始化内存的单位（g,m,k）

For example:如果我们要为JVM分配最小2GB和最大5GB大小的堆内存：

```java
-Xms2G -Xmx5G
```

### 1.2 显示新生代内存

在堆总可用内存分配完之后，第二大的影响因素是为`Young Generation`在堆内存所占的比例。（默认情况下，YG 的最小大小为 1310 *MB*，最大大小为*无限制*。）

1. **通过`-XX:NewSize`和`-XX:MaxNewSize`指定**

```java
-XX:NewSize=<young size>[unit]
-XX:MaxNewSize=<young size>[unit]
```

For example:为新生代分配最小256m的内存，最大1024的内存：

```java
-XX:NewSize=256m
-XX:MaxNewSize=1024m
```

2. **通过`-Xmn<young size>[unit]`指定**

我们要为 新生代分配 256m 的内存（NewSize 与 MaxNewSize 设为一致），我们的参数应该这样来写：

```
-Xmn256m
```

GC 调优策略中很重要的一条经验总结是这样说的：

> 将新对象预留在新生代，**由于 Full GC 的成本远高于 Minor GC**，因此尽可能将对象分配在新生代是明智的做法，实际项目中根据 GC 日志分析新生代空间大小分配是否合理，适当通过“-Xmn”命令调节新生代大小，**最大限度降低新对象直接进入老年代的情况**。

### 1.3 显示指定方法区/元空间大小

**从 Java 8 开始，如果我们没有指定 Metaspace 的大小，随着更多类的创建，虚拟机会耗尽所有可用的系统内存（永久代并不会出现这种情况）。**

**元空间使用的是本地内存。下面是一些常用参数：**

- `-XX:MetaspaceSize=N` #设置 Metaspace 的初始大小（是一个常见的误区，后面会解释）
- `-XX:MaxMetaspaceSize=N` #设置 Metaspace 的最大大小

**注意：**

1. `Metaspace` 的初始容量并不是 `-XX:MetaspaceSize` 设置，无论 `-XX:MetaspaceSize` 配置什么值，对于 64 位 JVM 来说，`Metaspace` 的初始容量都是 21807104（约 20.8m）。

2. `Metaspace` 由于使用不断扩容到`-XX:MetaspaceSize`参数指定的量，就会发生 FGC，且之后每次 `Metaspace` 扩容都会发生 Full GC。

   也就是说，`MetaspaceSize` 表示 `Metaspace` 使用过程中触发 Full GC 的阈值，只对触发起作用。



## 2 垃圾收集相关

### 2.1 垃圾收集器

JVM 具有四种类型的 GC 实现：

- 串行垃圾收集器
- 并行垃圾收集器
- CMS 垃圾收集器
- G1 垃圾收集器

可以使用以下参数声明这些实现：

```bash
-XX:+UseSerialGC
-XX:+UseParallelGC
-XX:+UseParNewGC
-XX:+UseG1GC
```



### 2.2 GC 日志记录

生产环境上，或者其他要测试 GC 问题的环境上，一定会配置上打印 GC 日志的参数，便于分析 GC 相关的问题。

```bash
# 必选
# 打印基本 GC 信息
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
# 打印对象分布
-XX:+PrintTenuringDistribution
# 打印堆数据
-XX:+PrintHeapAtGC
# 打印Reference处理信息
# 强引用/弱引用/软引用/虚引用/finalize 相关的方法
-XX:+PrintReferenceGC
# 打印STW时间
-XX:+PrintGCApplicationStoppedTime

# 可选
# 打印safepoint信息，进入 STW 阶段之前，需要要找到一个合适的 safepoint
-XX:+PrintSafepointStatistics
-XX:PrintSafepointStatisticsCount=1

# GC日志输出的文件路径
-Xloggc:/path/to/gc-%t.log
# 开启日志文件分割
-XX:+UseGCLogFileRotation
# 最多分割几个文件，超过之后从头文件开始写
-XX:NumberOfGCLogFiles=14
# 每个文件上限大小，超过就触发分割
-XX:GCLogFileSize=50M

```



这里推荐了非常多优质的 JVM 实践相关的文章，推荐阅读，尤其是 JVM 性能优化和问题排查相关的文章。



- [JVM 参数配置说明 - 阿里云官方文档 - 2022](https://help.aliyun.com/document_detail/148851.html)
- [JVM 内存配置最佳实践 - 阿里云官方文档 - 2022open in new window](https://help.aliyun.com/document_detail/383255.html)
- [求你了，GC 日志打印别再瞎配置了 - 思否 - 2022open in new window](https://segmentfault.com/a/1190000039806436)
- [一次大量 JVM Native 内存泄露的排查分析（64M 问题） - 掘金 - 2022open in new window](https://juejin.cn/post/7078624931826794503)
- [一次线上 JVM 调优实践，FullGC40 次/天到 10 天一次的优化过程 - HeadpDump - 2021open in new window](https://heapdump.cn/article/1859160)
- [听说 JVM 性能优化很难？今天我小试了一把！ - 陈树义 - 2021open in new window](https://shuyi.tech/archives/have-a-try-in-jvm-combat)
- [你们要的线上 GC 问题案例来啦 - 编了个程 - 2021open in new window](https://mp.weixin.qq.com/s/df1uxHWUXzhErxW1sZ6OvQ)
- [Java 中 9 种常见的 CMS GC 问题分析与解决 - 美团技术团队 - 2020open in new window](https://tech.meituan.com/2020/11/12/java-9-cms-gc.html)
- [从实际案例聊聊 Java 应用的 GC 优化-美团技术团队 - 美团技术团队 - 2017](https://tech.meituan.com/2017/12/29/jvm-optimize.html)

