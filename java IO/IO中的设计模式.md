# 装饰器模式

**装饰器（Decorator）模式** 可以在不改变原有对象的情况下拓展其功能。



装饰器模式通过**组合替代继承来扩展原始类的功能**，它允许向一个对象添加新的行为，同时又不改变其原有的结构

装饰器模式的核心思想是利用组合来实现对对象的功能扩展。它包含四个角色：

- 抽象组件(Component):定义了对象的基本接口
- 具体组件(ConcreteComponent):实现了抽象组件的接口并提供了基本的功能
- 抽象装饰器(Decorator):实现了抽象组件并组合了抽象组件
- 具体装饰器(ConcreteDecorator)：继承了抽象装饰器，并提供新增的方法

![image-20230410171432295](D:\Typora\workspace\java IO\IO中的设计模式.assets\image-20230410171432295.png)

## 例子

![image-20230410172022028](D:\Typora\workspace\java IO\IO中的设计模式.assets\image-20230410172022028.png)

1. 首先我们定义一个Coffce基类

```java
public abstract class Coffee {

    /**
     *
     * @return 返回价格
     */
    public abstract int getPrice();

    /**
     * 返回名字
     * @return
     */
    public abstract String getName();
}
```

2. 接着 我们定义一个Decorator类继承 我们的Coffice基类

​	

```java
public abstract class Decorator extends Coffee{

    protected Coffee mCoffee;

    /**
     * 通过组合的方式把Coffee对象传递进来
     * @param coffee
     */
    public Decorator(Coffee coffee){
        mCoffee=coffee;
    }
}
```

3. 接下来我们来看我们的子类是怎样实现的

```java
public class MilkDecorator extends Decorator {

    /**
     * 通过组合的方式把Coffee对象传递进来
     *
     * @param coffee
     */
    public MilkDecorator(Coffee coffee) {
        super(coffee);
    }

    @Override
    public int getPrice() {
        return mCoffee.getPrice()+10;
    }

    @Override
    public String getName() {
        return "addMilk";
    }
}
```

4. 接下来不难想象加糖，就奶泡。就摩卡的操作,都是在原来的之上加上配料的价格

```java
return mCoffee.getPrice()+2;
return mCoffee.getPrice()+15;
return mCoffee.getPrice()+20;
```

以后你想要计算加糖，就牛奶,加奶泡的咖啡的价格，只需要这样

```java
mCoffee = new SimpleCoffee();
mCoffee = new SugarDecorator(mCoffee);
mCoffee = new MilkDecorator(mCoffee);
mCoffee = new MilkFoamDecorator(mCoffee);
int price1 = mCoffee.getPrice();
System.out.println("price1="+price1);
```

以后你想要计算加糖，就牛奶咖啡的价格，只需要这样

```java
mCoffee = new SimpleCoffee();
mCoffee = new SugarDecorator(mCoffee);
mCoffee = new MilkDecorator(mCoffee);

int price1 = mCoffee.getPrice();
System.out.println("price1="+price1);
```

优点：

- 把类中的装饰功能从类中搬除，可以简化原来的类
- 可以把类的 核心职责和装饰功能区分开来，结构清晰 明了并且可以去除相关类的重复的装饰逻辑。

## 在IO场景下的应用

在一些继承关系比较复杂的场景（IO 这一场景各种类的继承关系就比较复杂）更加实用。

对于字节流来说， `FilterInputStream` （对应输入流）和`FilterOutputStream`（对应输出流）是装饰器模式的核心，分别用于增强 `InputStream` 和`OutputStream`子类对象的功能。



我们常见的`BufferedInputStream`(字节缓冲输入流)、`DataInputStream` 等等都是`FilterInputStream` 的子类，`BufferedOutputStream`（字节缓冲输出流）、`DataOutputStream`等等都是`FilterOutputStream`的子类。



举个例子，我们可以通过 `BufferedInputStream`（字节缓冲输入流）来增强 `FileInputStream` 的功能。

`BufferedInputStream` 构造函数如下：

```java
public BufferedInputStream(InputStream in) {
    this(in, DEFAULT_BUFFER_SIZE);
}

public BufferedInputStream(InputStream in, int size) {
    super(in);
    if (size <= 0) {
        throw new IllegalArgumentException("Buffer size <= 0");
    }
    buf = new byte[size];
}
```

可以看出，`BufferedInputStream` 的构造函数其中的一个参数就是 `InputStream` 。



`BufferedInputStream` 代码示例：

```java
try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream("input.txt"))) {
    int content;
    long skip = bis.skip(2);
    while ((content = bis.read()) != -1) {
        System.out.print((char) content);
    }
} catch (IOException e) {
    e.printStackTrace();
}
```

如果 `InputStream`的子类比较少的话，这样做是没问题的。不过， `InputStream`的子类实在太多，继承关系也太复杂了。如果我们为每一个子类都定制一个对应的缓冲输入流，那岂不是太麻烦了。



如果你对 IO 流比较熟悉的话，你会发现`ZipInputStream` 和`ZipOutputStream` 还可以分别增强 `BufferedInputStream` 和 `BufferedOutputStream` 的能力。

```java
BufferedInputStream bis = new BufferedInputStream(new FileInputStream(fileName));
ZipInputStream zis = new ZipInputStream(bis);

BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileName));
ZipOutputStream zipOut = new ZipOutputStream(bos);
```

```java
public class InflaterInputStream extends FilterInputStream {
}

public class DeflaterOutputStream extends FilterOutputStream {
}
```

这也是装饰器模式很重要的一个特征，那就是可以对原始类嵌套使用多个装饰器

为了实现这一效果，装饰器类需要跟原始类继承相同的抽象类或者实现相同的接口。上面介绍到的这些 IO 相关的装饰类和原始类共同的父类是 `InputStream` 和`OutputStream`。

对于字符流来说，`BufferedReader` 可以用来增加 `Reader` （字符输入流）子类的功能，`BufferedWriter` 可以用来增加 `Writer` （字符输出流）子类的功能。



# 适配器模式

**适配器（Adapter Pattern）模式** 主要用于接口互不兼容的类的协调工作，你可以将其联想到我们日常经常使用的电源适配器。

适配器模式中存在**被适配的对象**或者类称为 **适配者(Adaptee)** ，**作用于适配者的对象**或者类称为**适配器(Adapter)** 。适配器分为对象适配器和类适配器。**类适配器使用继承关系来实现，对象适配器使用组合关系来实现**。





## 在I/O下的场景

IO 流中的字符流和字节流的接口不同，它们之间可以协调工作就是基于适配器模式来做的，更准确点来说是对象适配器。通过适配器，我们可以将字节流对象适配成一个字符流对象，这样我们可以直接通过字节流对象来读取或者写入字符数据。

`InputStreamReader` 和 `OutputStreamWriter` 就是两个适配器(Adapter)， 同时，它们两个也是字节流和字符流之间的桥梁。`InputStreamReader` 使用 `StreamDecoder` （流解码器）对字节进行解码，**实现字节流到字符流的转换，**`OutputStreamWriter` 使用`StreamEncoder`（流编码器）对字符进行编码，实现字符流到字节流的转换。

`InputStream`和`OutputStream`的子类是Adaptee,`InputStreamReader` 和 `OutputStreamWriter` 是Adaptor



**适配器模式和装饰器模式有什么区别呢？**

1. **装饰器模式** 更侧重于动态地增强原始类的功能，装饰器类需要跟原始类继承相同的抽象类或者实现相同的接口。并且，装饰器模式支持对原始类嵌套使用多个装饰器。
2. **适配器模式** 更侧重于让接口不兼容而不能交互的类可以一起工作，当我们调用适配器对应的方法时，适配器内部会调用适配者类或者和适配类相关的类的方法
3. 适配器和适配者两者不需要继承相同的抽象类或者实现相同的接口。



# 工厂模式

工厂模式用于创建对象，NIO中使用了大量的工厂模式，比如`Files`类的`newInputStream`方法用于创建`InputStream`对象（静态工厂），`Paths` 类的 `get` 方法创建 `Path` 对象（静态工厂）、`ZipFileSystem` 类（`sun.nio`包下的类，属于 `java.nio` 相关的一些内部实现）的 `getPath` 的方法创建 `Path` 对象（简单工厂）。



# 观察者模式

NIO中的文件目录监听服务使用了观察者模式

NIO中文件目录监听服务基于`WatchService`和`Watchable`接口。`WatchService` 属于观察者，`Watchable` 属于被观察者。

`Watchable` 接口定义了一个用于将对象注册到 `WatchService`（监控服务） 并绑定监听事件的方法 `register` 。

```java
public interface Path
    extends Comparable<Path>, Iterable<Path>, Watchable{
}

public interface Watchable {
    WatchKey register(WatchService watcher,
                      WatchEvent.Kind<?>[] events,
                      WatchEvent.Modifier... modifiers)
        throws IOException;
}
```

`WatchService` 用于监听文件目录的变化，同一个 `WatchService` 对象能够监听多个文件目录。

```java
// 创建 WatchService 对象
WatchService watchService = FileSystems.getDefault().newWatchService();

// 初始化一个被监控文件夹的 Path 类:
Path path = Paths.get("workingDirectory");
// 将这个 path 对象注册到 WatchService（监控服务） 中去
WatchKey watchKey = path.register(
watchService, StandardWatchEventKinds...);
```

`Path` 类 `register` 方法的第二个参数 `events` （需要监听的事件）为可变长参数，也就是说我们可以同时监听多种事件。

```java
WatchKey register(WatchService watcher,
                  WatchEvent.Kind<?>... events)
    throws IOException;
```

常用的监听事件有 3 种：

- `StandardWatchEventKinds.ENTRY_CREATE` ：文件创建。

- `StandardWatchEventKinds.ENTRY_DELETE` : 文件删除。

- `StandardWatchEventKinds.ENTRY_MODIFY` : 文件修改。

`register` 方法返回 `WatchKey` 对象，通过`WatchKey` 对象可以获取事件的具体信息比如文件目录下是创建、删除还是修改了文件、创建、删除或者修改的文件的具体名称是什么。

```java
WatchKey key;
while ((key = watchService.take()) != null) {
    for (WatchEvent<?> event : key.pollEvents()) {
      // 可以调用 WatchEvent 对象的方法做一些事情比如输出事件的具体上下文信息
    }
    key.reset();
}
```

`WatchService` 内部是通过一个 daemon thread（守护线程）采用定期轮询的方式来检测文件的变化.