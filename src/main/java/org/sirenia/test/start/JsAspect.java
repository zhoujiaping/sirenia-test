package org.sirenia.test.start;

import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.sirenia.test.util.JsInvoker;
import org.sirenia.test.util.MethodUtil;
import org.sirenia.test.util.ReflectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

@Aspect
public class JsAspect {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private AspectConf aspectConf;
	public void setAspectConf(AspectConf aspectConf) {
		this.aspectConf = aspectConf;
	}
	/**
	 * 环绕通知
	 * 
	 * @param joinPoint
	 *            可用于执行切点的类
	 * @return
	 * @throws Throwable
	 */
	@Around("execution(* com.sfpay..*(..)) or execution(* com.sf..*(..))")
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
		//MethodInvocationProceedingJoinPoint
		//从配置中读取需要调用js的接口，如果在列表中，则调用对应的方法。
		//列表自动更新，通过文件监听，判断配置是否被修改过。
		Signature signature =	joinPoint.getSignature();
		MethodSignature ms = (MethodSignature)signature;
		Object[] args = joinPoint.getArgs();
		String funcName = signature.getName();//方法名
		/*
		 * 对于dubbo接口和hessian接口，在本地的实现类是jdk动态代理。
		 * */
		String clazzname = joinPoint.getTarget().getClass().getName();
		if(clazzname.startsWith("com.sun.proxy")){
			//mybatis mapper接口，hessian接口
			Set<Class<?>> interfaces = ReflectHelper.getAllInterfaces(joinPoint.getTarget().getClass());
			Class<?> clazz = interfaces.iterator().next();
			clazzname = clazz.getName();
		}else if(clazzname.startsWith("com.alibaba.dubbo.common.bytecode.proxy")){
			//dubbo接口
			Object methodInvocation = ReflectHelper.getValueByFieldName(joinPoint, "methodInvocation");
			Object target = ReflectHelper.getValueByFieldName(methodInvocation, "target");
			Object handler = ReflectHelper.getValueByFieldName(target, "handler");
			Object invoker = ReflectHelper.getValueByFieldName(handler, "invoker");
			Object directory = ReflectHelper.getValueByFieldName(invoker, "directory");
			Object serviceType = ReflectHelper.getValueByFieldName(directory, "serviceType");
			Class<?> clazz = (Class<?>) serviceType;
			clazzname = clazz.getName();
		}
		try{
			String key = aspectConf.getAppName()+"."+clazzname;
			if(aspectConf.methodSet.contains(key+"."+funcName)){
				Object jsObject = JsInvoker.evalFile(ResourceUtils.getFile(aspectConf.getJsDir()+"/"+key.replaceAll("\\.", "/")+".js"));
				Object ret = JsInvoker.invokeJsMethod(jsObject, funcName, args);
				if(ret == null){
					return null;
				}
				return MethodUtil.parseJSONForReturnType(ms.getMethod(), ret.toString());
			}
		}catch(Exception e){
			logger.error("JsAspect执行js异常");
			throw new RuntimeException(e);
		}
		Object ret = joinPoint.proceed();
		return ret;
	}

	/**
	 * 前置通知
	 */
	// @Before("execution(* com.xxx.impl..*(..))")
	public void before() {
		System.out.println("前置通知....");
	}

	/**
	 * 后置通知 returnVal,切点方法执行后的返回值
	 */
	// @AfterReturning(value="execution(*
	// com.zejian.spring.springAop.dao.UserDao.addUser(..))",returning =
	// "returnVal")
	public void afterReturning(Object returnVal) {
		System.out.println("后置通知...." + returnVal);
	}

	/**
	 * 抛出通知
	 * 
	 * @param e
	 */
	// @AfterThrowing(value="execution(*
	// com.zejian.spring.springAop.dao.UserDao.addUser(..))",throwing = "e")
	public void afterThrowable(Throwable e) {
		System.out.println("出现异常:msg=" + e.getMessage());
	}

	/**
	 * 无论什么情况下都会执行的方法
	 */
	// @After(value="execution(*
	// com.zejian.spring.springAop.dao.UserDao.addUser(..))")
	public void after() {
		System.out.println("最终通知....");
	}
}
