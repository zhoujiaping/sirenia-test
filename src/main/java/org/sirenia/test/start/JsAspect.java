package org.sirenia.test.start;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.sirenia.test.util.JsInvoker;
import org.sirenia.test.util.MethodUtil;
import org.sirenia.test.util.ReflectHelper;
import org.sirenia.test.util.UnitTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;

//@Aspect
public class JsAspect {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private JsAspectConf jsAspectConf;

	private SerializeConfig config = new SerializeConfig();

	public JsAspect() {

	}

	public void init() {
		ObjectSerializer toStringSerializer = new ObjectSerializer() {
			@Override
			public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features)
					throws IOException {
				serializer.write(object.toString());
			}
		};
		// config.put(org.apache.catalina.connector.ResponseFacade.class,
		// toStringSerializer);
		List<String> classNames = jsAspectConf.getStringSerializerClasses();
		for (String name : classNames) {
			if (StringUtils.hasText(name)) {
				try {
					Class<?> clazz = Class.forName(name);
					config.put(clazz, toStringSerializer);
				} catch (ClassNotFoundException e) {
					logger.warn("ClassNotFoundException：【{}】", name);
				}
			}
		}
	}
	public void setJsAspectConf(JsAspectConf jsAspectConf) {
		this.jsAspectConf = jsAspectConf;
	}

	/**
	 * 环绕通知
	 * 
	 * @param joinPoint
	 *            可用于执行切点的类
	 * @return
	 * @throws Throwable
	 */
	// @Around("execution(* com.sfpay..*(..)) or execution(* com.sf..*(..))")
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
		// MethodInvocationProceedingJoinPoint
		// 从配置中读取需要调用js的接口，如果在列表中，则调用对应的方法。
		// 列表自动更新，通过文件监听，判断配置是否被修改过。
		Signature signature = joinPoint.getSignature();
		// MethodSignature ms = (MethodSignature)signature;
		Object[] args = joinPoint.getArgs();
		String methodName = signature.getName();// 方法名
		Class<?> targetClazz = joinPoint.getTarget().getClass();
		String clazzname = targetClazz.getName();
		String clazznameRegexp = jsAspectConf.getClazznameRegexp();
		/*
		 * 对于dubbo接口和hessian接口，在本地的实现类是jdk动态代理。
		 */
		if (clazzname.startsWith("com.sun.proxy")) {
			// mybatis mapper接口，hessian接口
			clazzname = findClazzname(targetClazz, clazznameRegexp);
		} else if (clazzname.startsWith("com.alibaba.dubbo.common.bytecode.proxy")) {
			// dubbo接口
			Object methodInvocation = ReflectHelper.getValueByFieldName(joinPoint, "methodInvocation");
			Object target = ReflectHelper.getValueByFieldName(methodInvocation, "target");
			Object handler = ReflectHelper.getValueByFieldName(target, "handler");
			Object invoker = ReflectHelper.getValueByFieldName(handler, "invoker");
			Object directory = ReflectHelper.getValueByFieldName(invoker, "directory");
			Object serviceType = ReflectHelper.getValueByFieldName(directory, "serviceType");
			Class<?> clazz = (Class<?>) serviceType;
			clazzname = findClazzname(clazz, clazznameRegexp);
		} else {
			// 如果目标对象的类名已经匹配了正则，那么取它的接口作为类名
			clazzname = findClazzname(targetClazz, clazznameRegexp);
		}

		String clazznameRegexpExlude = jsAspectConf.getClazznameRegexpExlude();
		if (isExclude(clazzname, clazznameRegexpExlude)) {
			return joinPoint.proceed();
		}
		try {
			String fullFuncName = clazzname + "#" + methodName;
			String simpleFuncName = targetClazz.getSimpleName()+"#"+methodName;
			String dataDir = jsAspectConf.getDataHome() + "/" + UnitTest.getRelativeTestDir();
			Object jsObject = JsInvoker.evalFile(ResourceUtils.getFile(dataDir + "/mock.js"));
			//makeDirNX(new File(dataDir));
			Set<String> methods = JsInvoker.getOwnKeys(jsObject, true);
			String funcName = null; 
			if (methods.contains(fullFuncName)) {
				funcName = fullFuncName;
			}else if(methods.contains(simpleFuncName)){
				funcName = simpleFuncName;
			}
			if (funcName != null) {
				if (isExclude(clazzname, clazznameRegexpExlude)) {
					return joinPoint.proceed();
				}
				Object param = JSON.toJSONString(args, config);
				logger.info("执行Mock【{}】方法【{}】，入参：{}", dataDir, funcName, param);
				Object ret = JsInvoker.invokeJsMethod(jsObject, funcName, param, UnitTest.getDataSetId());
				logger.info("执行Mock【{}】方法【{}】，出参：{}", dataDir, funcName, ret);
				if (ret == null) {
					return null;
				}
				Method method = MethodUtil.getMethodByName(clazzname, methodName);
				// tip:不要用ms.getMethod()，返回的方法签名会不一样，解析json的结果不一样
				return MethodUtil.parseJSONForReturnType(method, ret.toString());
			}
		} catch (Exception e) {
			logger.error("JsAspect执行js异常", e);
			throw new RuntimeException(e);
		}
		Object ret = joinPoint.proceed();
		return ret;
	}

	/*private void makeDirNX(File file) {
		if (file.exists()) {
			return;
		}
		file.mkdirs();
	}*/

	/**
	 * 如果实现了接口，就拿接口名取匹配。匹配到了，则返回接口名，匹配不到，返回原类名
	 * 
	 * @param targetClazz
	 * @param clazznameRegexp
	 * @return
	 */
	private String findClazzname(Class<?> targetClazz, String clazznameRegexp) {
		String clazzname = targetClazz.getName();
		Set<Class<?>> interfaces = ReflectHelper.getAllInterfaces(targetClazz);// 可能会有多个接口，根据包名过滤得到想要的接口
		for (Class<?> i : interfaces) {
			String cn = i.getName();
			if (cn.matches(clazznameRegexp)) {
				return cn;
			}
		}
		return clazzname;
	}

	private boolean isExclude(String clazzname, String clazznameRegexpExlude) {
		return clazznameRegexpExlude != null && clazzname.matches(clazznameRegexpExlude);
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
