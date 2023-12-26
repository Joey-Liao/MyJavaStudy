# Java8中的Streams API详解

Java 8 中的 Stream 是对集合（Collection）对象功能的增强，它专注于对集合对象进行各种便利、高效的聚合操作（aggregate operation），或者大批量数据操作（bulk data operation）。Stream API 借助于 Lambda 表达式，极大的提高编程效率和程序可读性。同时它提供串行和并行两种模式进行汇聚操作，并发模式能够充分利用多核处理器的优势，使用 fork/join 并行方式来拆分任务和加速处理过程。通常编写并行代码很难而且容易出错, 但使用 Stream API 无需编写一行多线程的代码，就可以很方便地写出高性能的并发程序。

## 1. 前言

详细讨论之前，先展示一个排序、取值案例在 Java 8 之前版本的实现：

```java
// 在原文实现代码基础上添加枚举类 Transaction
enum Transaction {
    GROCERY(0, "grocery"), NONE(1, "none");

    private final int id;
    private final String value;

    Transaction(int id, String value) {
        this.id = id;
        this.value = value;
    }

    public Object getType() { return this; }
    public int getId() { return this.id; }
    public String getValue() { return this.value; }
}

public static void main(String[] args) {
    // 使用泛型初始化一个 Transaction 集合，代替原文使用的原始类型
    List<Transaction> transactions = new ArrayList<>() {
        {
            add(Transaction.NONE);
            add(Transaction.GROCERY);
        }
    };

    List<Transaction> groceryTransactions = new Arraylist<>();
    for(Transaction t: transactions){
        if(t.getType() == Transaction.GROCERY){
            groceryTransactions.add(t);
        }
    }
    Collections.sort(groceryTransactions, new Comparator(){
        public int compare(Transaction t1, Transaction t2){
            return t2.getValue().compareTo(t1.getValue());
        }
    });
    List<Integer> transactionIds = new ArrayList<>();
    for(Transaction t: groceryTransactions){
        transactionsIds.add(t.getId());
    }
｝
```

在 Java 8 中使用 Stream 实现的版本更加简洁：

```java
List<Integer> transactionsIds = transactions.parallelStream().
    filter(t -> t.getType() == Transaction.GROCERY).
    sorted(comparing(Transaction::getValue).reversed()).
    map(Transaction::getId).
    collect(toList());
```



## 2. 总览



### 2.1 什么是流

Stream不是集合元素，不是数据结构，不保存数据，它与算法和计算有关，更像一个高级Iterator。

原始版本的 Iterator，用户只能显式地一个一个遍历元素并对其执行某些操作；使用 Stream，用户只要给出需要对其包含的元素执行什么操作，比如：「过滤掉长度大于 10 的字符串」、「获取每个字符串的首字母」等，Stream 会隐式地在内部进行遍历，做出相应的数据转换。

Stream 的处理过程是单向、不可往复的，数据只能遍历一次，遍历过一次后即用尽了，就好比流水从面前流过，一去不复返。Stream 的另外一大特点是，数据源本身可以是无限的。

和迭代器不同的是，Stream 可以并行化操作，迭代器只能命令式地、串行化操作。顾名思义，当使用串行方式去遍历时，只能读取一个 item 后再读下一个 item。而使用并行去遍历时，数据会被分成多个段，每一段都在不同的线程中处理，然后将结果一起输出。Stream 的**并行操作依赖于 Java 7 中引入的 Fork/Join 框架**（JSR166y）来拆分任务和加速处理过程。Java 的并行 API 演变历程概括如下：

- 1.0-1.4 中的 java.lang.Thread
- 5.0 中的 java.util.concurrent
- 6.0 中的 Phasers 等
- 7.0 中的 Fork/Join 框架
- 8.0 中的 Lambda



### 2.2 流的构成

当我们使用一个流的时候，通常包括三个基本步骤：

- 获取一个数据源（source）
- 数据转换
- 执行操作获取想要的结果

每次转换，**原有 Stream 对象不改变，返回一个新的 Stream 对象**（可以有多次转换），这就允许对其操作可以像链条一样排列，变成一个管道。



### 2.3 生成Steam

#### 从Collection和数组

- Collection.stream()
- Collection.parallelStream()
- Arrays.stream(T aaray)

#### 从BufferedReader

- BufferedReader.lines()

#### 静态工厂

- IntStream.range()
- Stream.of()

#### 其他

- Random.ints()
- BitSet.stream()
- Pattern.splitAsStream(java.lang.CharSequence)
- JarFile.stream()



## 3. 流的操作

流操作分为中间操作和终端操作，两者结合形成流水线。流水线由一个源（如集合、数组、生成函数或 I/O 通道）、零个或多个中间操作（如 Stream.filter 或 Stream.map）和一个终端操作（如 Stream.forEach 或 Stream.reduce）组成。

### 3.1 Intermediate

一个流可以后面跟随零个或多个 intermediate 操作。其目的主要是打开流，做出某种程度的**数据映射/过滤**，然后返回一个新的流，交给下一个操作使用。这类操作都是**惰性化的（lazy），就是说，仅仅调用到这类方法，并没有真正开始流的遍历**。常见的操作如下：

**map (mapToInt, flatMap 等)、 filter、 distinct、 sorted、 peek、 limit、 skip、 parallel、 sequential、 unordered**

### 3.2 Terminal 

一个流只能有一个terminal操作，当这个操作执行后，流就被用光了，无法再被操作。所以Terminal操作一定是流的最后一个操作。Terminal操作的执行，才会真正开始流的遍历，并且会生成一个结果，或者是一个side effect。常见的操作如下：

**forEach、 forEachOrdered、 toArray、 reduce、 collect、 min、 max、 count、 anyMatch、 allMatch、 noneMatch、 findFirst、 findAny、 iterator**

###  3.3 short-circuiting

指

- 对于一个intermediate操作，如果接收的是一个无限大的Stream，但返回一个有限的新Stream
- 对于一个terminal操作，如果它接受的是一个无限大的Stream，但能在有限的时间计算出结果

**当操作一个无限大的 Stream，而又希望在有限时间内完成操作，则在管道内拥有一个 short-circuiting 操作是必要非充分条件**。常见的操作如下：

**anyMatch、 allMatch、 noneMatch、 findFirst、 findAny、 limit**



## 4. 流的使用

简单说，对 Stream 的使用就是实现一个 filter-map-reduce 过程，产生一个最终结果，或者导致一个副作用（side effect）。



### 4.1 Steam构造与转换

常见样例：

```java
//1. Individual values
Stream stream=Stream.of("a","b","c");

//2. Arrays
String[] strArrays=new String[]{"a","b","c"};
stream=Stream.of(strArrays);
stream=Arrays.stream(strArrays);

//3. Collections
List<String> list=new ArrayList<>(Arrays.asList(strArrays));
stream=list.stream();
```



对于基本类型，有三种对应的包装类型Stream：IntStream，LongStream、DoubleStream，这是为了减少boxing和unboxing的耗时

案例：数值流的构造

```java
IntStream.of(new int[]{1, 2, 3}).forEach(System.out::println);
IntStream.range(1, 3).forEach(System.out::println);
IntStream.rangeClosed(1, 3).forEach(System.out::println);
```

案例：Stream转换为其他数据结构

```java
//Stream->array
String[] strs=stream.toArray(String[]::new);

//Stream->Collection
List<String> list=stream.collect(Collectors.toList());
List<String> list=stream.collect(Collectors.toCollection(ArrayList::new));

Set<String> set=stream.collect(Collectors.toSet());
Stack<String> stack=stream.collect(Collectors.toCollection(Stack::new));

//Stream->String
String str=stream.collect(Collectors.joining()).toString();
```

一个 Stream 只可以使用一次，上面的代码为了简洁而重复使用了数次。



### 4.2 Stream典型用法

#### map、flatMap

map的作用是把input stream的每一个元素 映射 到output stream的另外一个元素。

案例：将所有单词转换为大写

```java
List<String> output=input.stream()
	.map(String::toUpperCase)
	.collect(Collectors.toList());
```

案例：生成每个数的平方

```java
List<Integer> nums=new ArrayList<>(Arrays.asList(1,2,3,4));
List<Integer> nums=nums.stream()
	.map(n->n*n)
	.collect(Collectors.toList());
```

从上面例子可以看出，**map 生成的是个 1:1 映射**，每个输入元素，都按照规则转换成为另外一个元素。还有一些场景，是**一对多映射关系的，这时需要 flatMap**。

案例：一对多。flatMap 把 input Stream 中的层级结构扁平化，就是将最底层元素抽出来放到一起，最终 output 的新 Stream 里面已经没有 List 了，都是直接的数字。

```java
Stream<List<Integer>> inputStream = Stream.of(
    Arrays.asList(1),
    Arrays.asList(2, 3),
    Arrays.asList(4, 5, 6)
);

Stream<Integer> outputStream = inputStream
    .flatMap((childList) -> childList.stream());
```

#### filter

filter对原始Stream进行过滤，通过过滤的元素被留下来形成一个新的Stream

案例：留下偶数

```java
int[] nums=new int[]{1,2,3,4,5,6};
Integer[] evens=Stream.of(nums)
	.filter(a->a%2==0)
	.toArray(Integer[]::new);
```

案例：把单词挑出来，这段代码首先把每行的单词用 flatMap 整理到新的 Stream，然后保留长度不为 0 的，就是整篇文章中的全部单词了。

```java
List<String> list=reader.lines()
	.flatMap(line->Stream.of(line.split(REGEXP)))
	.filter(a->a.length()>0)
	.collect(Collector.toList());
```

#### forEach、peek

forEach方法接受一个Lambda表达式，然后再Stream的每个元素上执行该表达式。

案例：打印男性姓名

```java
//stream
person.stream()
	.filter(a->a.getGender()==Person.sex.MALE)
	.forEach(a->System.out.println(a.getName()));
```

```java
// other
for (Person p : roster) {
    if (p.getGender() == Person.Sex.MALE) {
        System.out.println(p.getName());
    }
}
```

对一个人员集合遍历，找出男性并打印姓名。可以看出来，forEach 是为 Lambda 而设计的，保持了最紧凑的风格。而且 Lambda 表达式本身是可以重用的，非常方便。当需要为多核系统优化时，可以 parallelStream().forEach()，只是此时原有元素的**次序没法保证**，并行的情况下将改变串行时操作的行为，此时 forEach 本身的实现不需要调整，而 Java8 以前的 for 循环 code 可能需要加入额外的多线程逻辑。

另外一点需要注意，**forEach 是 terminal 操作，因此它执行后，Stream 的元素就被「消费」掉了，你无法对一个 Stream 进行两次 terminal 运算。**下面的代码是错误的：

```java
stream.forEach(element -> doOneThing(element));
stream.forEach(element -> doAnotherThing(element));
```



相反，具有相似功能的 intermediate 操作 peek 可以达到上述目的。如下是出现在该 api javadoc 上的一个示例。

案例：使用 peek 对每个元素执行操作并返回一个新的 Stream

```java
Stream.of("one", "two", "three", "four")
    .filter(e -> e.length() > 3)
    .peek(e -> System.out.println("Filtered value: " + e))
    .map(String::toUpperCase)
    .peek(e -> System.out.println("Mapped value: " + e))
    .collect(Collectors.toList());
```

**forEach 不能修改自己包含的本地变量值，也不能用 break/return 之类的关键字提前结束循环。**



#### findFirst

**这是一个 termimal 兼 short-circuiting 操作，它总是返回 Stream 的第一个元素，或者空**。这里比较重点的是它的**返回值类型：Optional**。这也是一个模仿 Scala 语言中的概念，作为一个容器，它可能含有某值，或者不包含。使用它的目的是尽可能避免 NullPointerException。

案例：Optional 的两个用例

```java
String strA = " abcd ", strB = null;
print(strA);
print("");
print(strB);
getLength(strA);
getLength("");
getLength(strB);

public static void print(String text) {
     // Java 8
     Optional.ofNullable(text).ifPresent(System.out::println);
     // Pre-Java 8
     if (text != null) {
         System.out.println(text);
     }
 }

public static int getLength(String text) {
    // Java 8
    return Optional.ofNullable(text).map(String::length).orElse(-1);
    // Pre-Java 8
    // return if (text != null) ? text.length() : -1;
}
```



在更复杂的 if (xx != null) 的情况中，使用 Optional 代码的可读性更好，而且它提供的是编译时检查，能极大的降低 NPE 这种 Runtime Exception 对程序的影响，或者迫使程序员更早的在编码阶段处理空值问题，而不是留到运行时再发现和调试。

Stream 中的 findAny、max/min、reduce 等方法等返回 Optional 值。还有例如 IntStream.average() 返回 OptionalDouble 等等。

#### reduce

该方法主要将Stream元素组合起来。它提供一个初始值（种子），然后依照运算规则（BinaryOperator），和前面 Stream 的第一个、第二个、第 n 个元素组合。从这个意义上说，字符串拼接、数值的 sum、min、max、average 都是特殊的 reduce。例如 Stream 的 sum 就相当于 `Integer sum = integers.reduce(0, (a, b) -> a+b);` 或 `Integer sum = integers.reduce(0, Integer::sum);`。也有没有起始值的情况，这时会把 Stream 的前面两个元素组合起来，返回的是 Optional。

```java
// 字符串连接，concat = "ABCD"
String concat = Stream.of("A", "B", "C", "D").reduce("", String::concat);
// 求最小值，minValue = -3.0
double minValue = Stream.of(-1.5, 1.0, -3.0, -2.0).reduce(Double.MAX_VALUE, Double::min);
// 求和，sumValue = 10, 有起始值
int sumValue = Stream.of(1, 2, 3, 4).reduce(0, Integer::sum);
// 求和，sumValue = 10, 无起始值
sumValue = Stream.of(1, 2, 3, 4).reduce(Integer::sum).get();
// 过滤，字符串连接，concat = "ace"
concat = Stream.of("a", "B", "c", "D", "e", "F").
    filter(x -> x.compareTo("Z") > 0).
    reduce("", String::concat);
```

上面代码例如第一个示例的 reduce()，第一个参数（空白字符）即为起始值，第二个参数（String::concat）为 BinaryOperator。这类有起始值的 reduce() 都返回具体的对象。而对于第四个示例没有起始值的 reduce()，由于可能没有足够的元素，返回的是 Optional，请留意这个区别。



#### limit、skip

limit 返回 Stream 的前面 n 个元素；skip 则是扔掉前 n 个元素

案例：limit 和 skip 对运行次数的影响

```java
// 定义一个 Person 类
private class Person {
    public int no;
    private String name;

    public Person (int no, String name) {
        this.no = no;
        this.name = name;
    }
    public String getName() {
        System.out.print(name + ",");
        return name;
    }
}

public void testLimitAndSkip() {
    List<Person> persons = new ArrayList<>();
    for (int i = 1; i <= 10000; i++) {
        Person person = new Person(i, "name" + i);
        persons.add(person);
    }
    List<String> personList2 = persons.stream()
        .map(Person::getName)
        .limit(10).skip(3)
        .collect(Collectors.toList());
    System.out.println("\n" + personList2);
}
```

```java
name1,name2,name3,name4,name5,name6,name7,name8,name9,name10,

[name4, name5, name6, name7, name8, name9, name10]
```

这是一个有 10，000 个元素的 Stream，但在 short-circuiting 操作 limit 和 skip 的作用下，管道中 map 操作指定的 getName() 方法的执行次数为 limit 所限定的 10 次，而最终返回结果在跳过前 3 个元素后只有后面 7 个返回。

有一种情况是 limit/skip 无法达到 short-circuiting 目的的，就是把它们放在 Stream 的排序操作后，原因跟 sorted 这个 intermediate 操作有关：此时系统并不知道 Stream 排序后的次序如何，所以 sorted 中的操作看上去就像完全没有被 limit 或者 skip 一样。

案例：limit 和 skip 对 sorted 后的运行次数无影响

```java
private class Person {
    public int no;
    private String name;

    public Person (int no, String name) {
        this.no = no;
        this.name = name;
    }
    public String getName() { return name; }

    @Override
    public String toString() {
        return "{no=" + no + ", name='" + name + "'}";
    }
}

public void testLimitAndSkip() {
    Set<Person> persons = new HashSet<>();
    for (int i = 1; i <= 5; i++) {
        Person person = new Person(i, "name" + i);
        persons.add(person);
    }
    System.out.println(persons);
    List<Person> personList2 = persons.stream()
        .sorted((p1, p2) ->p1.getName().compareTo(p2.getName()))
        .limit(2)
        .collect(Collectors.toList());
    System.out.println("\n" + personList2);
}

// 或优化为：
public void testLimitAndSkip() {
    Set<Person> persons = new HashSet<>();
    for (int i = 1; i <= 5; i++) {
        Person person = new Person(i, "name" + i);
        persons.add(person);
    }
    System.out.println(persons);
    List<Person> personList2 = persons.stream()
        .sorted(Comparator.comparing(Person::getName))
        .limit(2)
        .collect(Collectors.toList());
    System.out.println("\n" + personList2);
}
```



上面的示例对案例做了微调，将元素容器修改为 Set，以便能清晰显示出排序效果。仅对 5 个元素的 Stream 排序，然后进行 limit 操作。输出结果为：

```java
[{no=1, name='name1'}, {no=5, name='name5'}, {no=4, name='name4'}, {no=3, name='name3'}, {no=2, name='name2'}]

[{no=1, name='name1'}, {no=2, name='name2'}]
```



即虽然最后的返回元素数量是 2，但整个管道中的 sorted 表达式执行次数没有像前面例子相应减少。

最后有一点需要注意的是，对一个 parallel 的 Steam 管道来说，如果其元素是有序的，那么 limit 操作的成本会比较大，因为它的返回对象必须是前 n 个也有一样次序的元素。取而代之的策略是取消元素间的次序，或者不要用 parallel Stream。

#### sorted

对Stream排序通过sorted进行，它比数组排序更强的地方在于可以先对Stream进行各类map、filter、limit、skip、distinct来减少元素数量后，再排序，达到减少程序运行时间的目的。

案例：排序前使用limit和skip优化

```java
private class Person {
    public int no;
    private String name;

    public Person (int no, String name) {
        this.no = no;
        this.name = name;
    }
    public String getName() { return name; }

    @Override
    public String toString() {
        return "{no=" + no + ", name='" + name + "'}";
    }
}

public void testLimitAndSkip() {
    List<Person> persons = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
        Person person = new Person(i, "name" + i);
        persons.add(person);
    }
    System.out.println(persons);
    List<Person> personList2 = persons.stream()
        .limit(2)
        .sorted((p1, p2) -> p1.getName().compareTo(p2.getName()))
        .collect(Collectors.toList());
    System.out.println("\n" + personList2);
}
```

结果会简单很多：

```
[{no=1, name='name1'}, {no=2, name='name2'}, {no=3, name='name3'}, {no=4, name='name4'}, {no=5, name='name5'}]

[{no=1, name='name1'}, {no=2, name='name2'}]
```



当然，这种优化是有 business logic 上的局限性的：即不要求排序后再取值。



#### min、max、distinct

min 和 max 的功能也可以通过对 Stream 元素先排序，再 findFirst 来实现，但前者的性能会更好，为 O(n)，而 sorted 的成本是 O(n log n)。同时它们作为特殊的 reduce 方法被独立出来也是因为求最大最小值是很常见的操作。

案例：找出最长一行的长度

```java
BufferedReader br=new BufferedReader(new FileReader("c:\\ss.log"));
int longest=br.lines()
	.map(a->a.length())//.mapToInt(String::length)
	.max()
	.getAsInt();
br.close();
```

案例：使用 distinct 来找出全文不重复的单词，转小写，并排序

```java
List<String> words = br.lines()
	.flatMap(a->Stream.of(a.split(" ")))
	.filter(a->a.length()>0)
	.map(String::toLowerCase)
	.distinct()
	.sorted()
	.collect(Collector.toList());
br.close();
```

#### sum

案例：通过stream()获取当前item的source，filter 和 mapToInt是 intermediate 操作，进行数据筛选和转换，最后一个 sum() 为 terminal 操作，对符合条件的全部小物件作重量求和。

```java
int sum = widgets.stream()
    .filter(w -> w.getColor() == RED)
    .mapToInt(w -> w.getWeight())
    .sum();
```

#### Match

Stream 有三个 match 方法，从语义上说：

- allMatch：Stream 中全部元素符合传入的 predicate，返回 true
- anyMatch：Stream 中只要有一个元素符合传入的 predicate，返回 true
- noneMatch：Stream 中没有一个元素符合传入的 predicate，返回 true

它们都不是要遍历全部元素才能返回结果。例如 allMatch 只要一个元素不满足条件，就 skip 剩下的所有元素，返回 false。对清单 13 中的 Person 类稍做修改，加入一个 age 属性和 getAge 方法。

案例：使用 Match

```java
class Person {
    public int no;
    private String name;
    private int age;

    public Person(int no, String name, int age) {
        this.no = no;
        this.name = name;
        this.age = age;
    }
    public String getName() { return name; }
    public int getAge() { return age; }

    @Override
    public String toString() {
        return "{no=" + no + ", name='" + name + "'}";
    }
}

public void testMatch() {
    List<Person> persons = new ArrayList<>() {
        {
            add(new Person(1, "name" + 1, 10));
            add(new Person(2, "name" + 2, 21));
            add(new Person(3, "name" + 3, 34));
            add(new Person(4, "name" + 4, 6));
            add(new Person(5, "name" + 5, 55));
        }
    };
    boolean isAllAdult = persons.stream()
            .allMatch(p -> p.getAge() > 18);
    System.out.println("All are adult? " + isAllAdult);
    boolean isThereAnyChild = persons.stream()
            .anyMatch(p -> p.getAge() < 12);
    System.out.println("Any child? " + isThereAnyChild);
}
```

输出结果：

```java
All are adult? false
Any child? true
```



### 4.3 进阶：用 Collectors 来进行 reduction 操作

java.util.stream.Collectors 类的主要作用就是辅助进行各类有用的 reduction 操作，例如转变输出为 Collection，把 Stream 元素进行归组。

#### groupingBy 和 partitioningBy

案例：按照年龄归组

```java
Map<Integer,List<Person>> personGroups=Stream.generate(new PersonSupplier())
	.limit(100)
	.collect(Collectors.groupingBy(Person::getAge));
Iterator it=personGroups.entrySet().iterator();
while(it.hasNext()){
	Map.Entry<Integer, List<Person>> persons = (Map.Entry) it.next();
    System.out.println("Age " + persons.getKey() + " = " + persons.getValue().size());
}
```

上面的 code，首先生成 100 人的信息，然后按照年龄归组，相同年龄的人放到同一个 list 中，可以看到如下的输出：

```java
Age 0 = 2
Age 1 = 2
Age 5 = 2
Age 8 = 1
Age 9 = 1
Age 11 = 2
……
```

案例：按照未成年人和成年人归组

```java
Map<Boolean, List<Person>> children = Stream.generate(new PersonSupplier())
    .limit(100)
    .collect(Collectors.partitioningBy(p -> p.getAge() < 18));
System.out.println("Children number: " + children.get(true).size());
System.out.println("Adult number: " + children.get(false).size());
```



输出结果：

```java
Children number: 23
Adult number: 77
```



在使用条件「年龄小于 18」进行分组后可以看到，不到 18 岁的未成年人是一组，成年人是另外一组。partitioningBy 其实是一种特殊的 groupingBy，它依照条件测试的是否两种结果来构造返回的数据结构，get(true) 和 get(false) 能即为全部的元素对象。

总之，Stream 的特性可以归纳为：

- 不是数据结构
- 它**没有内部存储**，它只是用操作管道从 source（数据结构、数组、generator function、IO channel）抓取数据。
- 它也**绝不修改自己所封装的底层数据结构的数据**。例如 Stream 的 filter 操作会产生一个不包含被过滤元素的新 Stream，而不是从 source 删除那些元素。
- **所有 Stream 的操作必须以 lambda 表达式为参数**
- 不支持索引访问
- 你可以请求第一个元素，但无法请求第二个，第三个，或最后一个。不过请参阅下一项。
- 很容易生成数组或者 List
- **惰性化**
- 很多 Stream 操作是向后延迟的，一直到它弄清楚了最后需要多少数据才会开始。
- Intermediate 操作永远是惰性化的。
- **并行能力**
- 当一个 Stream 是并行化的，就不需要再写多线程代码，所有对它的操作会自动并行进行的。
- 可以是无限的。集合有固定大小，Stream 则不必。limit(n) 和 findFirst() 这类的 short-circuiting 操作可以对无限的 Stream 进行运算并很快完成。