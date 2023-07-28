# 整体架构

![image-20230714162731676](D:\Typora\workspace\SpringSecurity\SpringSecurity流程总结.assets\image-20230714162731676.png)

## 认证

- AuthenticationManager: 负责处理认证。主要实现类为 ProviderManager，在 ProviderManager 中管理了众多 AuthenticationProvider 实例。在一次完整的认证流程中，Spring Security 允许存在多个 AuthenticationProvider ，用来实现多种认证方式，这些 AuthenticationProvider 都是由 ProviderManager 进行统一管理的。

  

- Authentication：存放认证以及认证成功的信息

  - getAuthorities 	 获取用户权限信息

  - getCredentials 	获取用户凭证信息，一般指密码

  - getDetails 			 获取用户详细信息

  - getPrincipal 		 获取用户身份信息，用户名、用户对象等

  - isAuthenticated   用户是否认证成功

    

- SecurityContextHolder  ：用来获取登录之后用户信息，SecurityContextHolder 中的数据保存默认是通过ThreadLocal 来实现的，使用 ThreadLocal 创建的变量只能被当前线程访问，不能被其他线程访问和修改，也就是用户数据和请求线程绑定在一起。
  

## 授权

- AccessDecisionManager (访问决策管理器)，用来决定此次访问是否被允许
- AccessDecisionVoter (访问决定投票器)，投票器会检查用户是否具备应有的角色，进而投出赞成、反对或者弃权票。
- ConfigAttribute，用来保存授权时的角色信息



# 重定向到Login页面流程分析

![image-20220111100643506](D:\Typora\workspace\SpringSecurity\SpringSecurity流程总结.assets\image-20220111100643506.png)

1. 请求 /hello 接口，在引入 spring security 之后会先经过一些列过滤器
2. 在请求到达 FilterSecurityInterceptor时，发现请求并未认证。请求拦截下来，并抛出 AccessDeniedException 异常。
3. 抛出 AccessDeniedException 的异常会被 ExceptionTranslationFilter 捕获，这个 Filter 中会调用 LoginUrlAuthenticationEntryPoint#commence 方法给客户端返回 302，要求客户端进行重定向到 /login 页面。
4. 客户端发送 /login 请求。
5. /login 请求会再次被拦截器中 DefaultLoginPageGeneratingFilter 拦截到，并在拦截器中返回生成登录页面。

**就是通过这种方式，Spring Security 默认过滤器中生成了登录页面，并返回！**





# 一些配置项



## UserDetailService （配置用户名认证方法）

 UserDetailService 是顶层父接口，接口中 loadUserByUserName 方法是用来在认证时进行用户名认证方法，默认实现使用是内存实现，如果想要修改数据库实现我们只需要自定义 UserDetailService 实现，最终返回 UserDetails 实例即可。





`AuthenticationEntryPoint` 和 `AuthenticationFailureHandler` 是 Spring Security 中用于处理身份验证失败的不同机制。

1. **AuthenticationEntryPoint**：
   - `AuthenticationEntryPoint` 是用于处理未经身份验证的请求的入口点。
   - 当用户尝试访问受保护的资源但未经身份验证时，`AuthenticationEntryPoint` 负责触发身份验证过程，例如重定向到登录页面或返回 HTTP 401 未经授权状态码。
   - `AuthenticationEntryPoint` 的 `commence()` 方法在用户未经身份验证时被调用。
2. **AuthenticationFailureHandler**：
   - `AuthenticationFailureHandler` 用于处理身份验证失败的情况。
   - 当身份验证过程中发生错误或验证凭据无效时，`AuthenticationFailureHandler` 用于决定如何响应该失败，例如返回自定义错误页面或返回特定的 JSON 响应。
   - `AuthenticationFailureHandler` 的 `onAuthenticationFailure()` 方法在身份验证失败时被调用。

主要区别在于：

- `AuthenticationEntryPoint` 主要处理未经身份验证的请求，它负责触发身份验证过程。
- `AuthenticationFailureHandler` 则处理身份验证过程中的失败情况，它决定如何响应身份验证失败。

在某些情况下，这两种机制可以结合使用。例如，当身份验证失败时，`AuthenticationFailureHandler` 可以在处理失败时调用 `AuthenticationEntryPoint` 来触发身份验证过程。这样可以确保在验证失败的情况下，用户被正确地重定向到登录页面或返回适当的响应。