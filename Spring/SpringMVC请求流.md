# 核心架构的具体流程步骤



![image-20230426105629656](D:\Typora\workspace\Spring\SpringMVC请求流.assets\image-20230426105629656.png)



步骤如上：

1. **首先用户发送请求——> `DispatcherServlet`**，收到请求后，`DispatcherServlet`不会自己处理，而是作为统一访问点。
2. **`DispatcherServlet`——>`HandlerMapping`**，`HandlerMapping`会将请求映射到`HandlerExecutionChain`对象（包含一个Handler和多个`HandlerInterceptor`拦截器）
3. **`DispatcherServlet`——>`HandlerAdapter`**，`HandlerAdapter` 将会把处理器包装为适配器，从而支持多种类型的处理器， 即适配器设计模式的应用，从而很容易支持很多类型的处理器；
4. **`HandlerAdapter`——>处理器功能处理方法的调用** ，`HandlerAdapter` 将会根据适配的结果调用真正的处理器的功能处 理方法，完成功能处理；并返回一个`ModelAndView` 对象
5. **`ModelAndView` 的逻辑视图名——> `ViewResolver`**，`ViewResolver` 将把逻辑视图名解析为具体的`View`，通过这种策 略模式，很容易更换其他视图技术；
6. **返回控制权给`DispatcherServlet`**，由`DispatcherServlet` 返回响应给用户，到此一个流程结束。



补充：

1. **Filter(ServletFilter)**：进入Servlet前可以有preFilter, Servlet处理之后还可有postFilter
2. **LocaleResolver**：在视图解析/渲染时，还需要考虑国际化(Local)，显然这里需要有LocaleResolver.

