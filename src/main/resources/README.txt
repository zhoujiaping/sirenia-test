项目简介
为了支持开发测试，解决hessian、dubbo以及本地调用时的一系列问题，
比如要保持和远程服务的数据状态一致，要保证远程服务可用，不能灵活设置方法执行结果，某些测试场景难以获得基础数据等，
特开发此项目。
只需要简单的配置，即可将dubbo接口、hessian接口甚至本地被spring管理的bean的方法，都可以通过js函数进行mock。
支持js文件内容动态更新。

1、配置依赖
<dependency>
	<groupId>org.sirenia</groupId>
	<artifactId>sirenia-test</artifactId>
	<version>0.0.1-SNAPSHOT</version>
</dependency>


2、spring aop配置
将classpath下的beans/local-beans-aspecttest.xml拷贝到对应的目录，保证被spring的配置文件import。


3、方法拦截配置
新建文件
D:\sirenia-test\method-set\method-set.txt
格式
${project.name}.classname.methodname
表示拦截该方法
如下
#mymodule.com.xxxxxx.service.DemoService.aMethod
mymodule.com.xxxxxx.service.DemoService.bMethod
#号开头的行表示注释
修改method-set.txt文件内容时，系统会自动重新读取其中的内容，不需要重新启动。



4、方法执行替换
新建文件

D:/sirenia-test/method-js/mymodule/com/xxxxxx/service/DemoService.js
内容
(function(){
	return {
		aMethod :function (args){
			print(args)
			print(JSON.stringify(args))
			return JSON.stringify({key1:'value1',key2:'value2'});
		}
	}
})();
表示aMethod这个方法调用，不调用真实的方法，而是调用js里面对应的方法。
注：
此处的js，是jdk内置的js，不能用es6；
js文件支持内容变更时自动重新加载；


局限性：
不是所有的接口都能支持，如果接口返回值类型比较特殊，比如List<?>，则会导致错误。


todolist
1、优化目录结构设计(已完成)
2、添加功能：可以执行spring bean的任意方法
    应用启动后，获取spring上下文。开启一个服务，监听端口，当接收到请求，就调用对应的bean的方法。