package org.sirenia.test.start;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.sirenia.test.util.ReflectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class LogAspect {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private static final String DateFormat = "yyyyMMdd";
	private JsAspectConf jsAspectConf;
	private SerializeConfig config = new SerializeConfig();

	public LogAspect() {
	}
	public void setJsAspectConf(JsAspectConf jsAspectConf) {
		this.jsAspectConf = jsAspectConf;
	}
	public void init(){
		ObjectSerializer toStringSerializer = new ObjectSerializer() {
			@Override
			public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features)
					throws IOException {
				serializer.write(object.toString());
			}
		};
		//config.put(org.apache.catalina.connector.ResponseFacade.class, toStringSerializer);
		try {
			List<String> classNames = jsAspectConf.getStringSerializerClasses();
			for(String name : classNames){
				if(StringUtils.hasText(name)){
					Class<?> clazz = Class.forName("");
					config.put(clazz, toStringSerializer);
				}
			}
		/*	Class<?> responseClass = Class.forName("org.apache.catalina.connector.ResponseFacade");
			Class<?> requestClass = Class.forName("");
			Class<?> sessionClass = Class.forName("");
			config.put(requestClass, toStringSerializer);
			config.put(sessionClass, toStringSerializer);
*/		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * 环绕通知
	 * 
	 * @param joinPoint
	 *            可用于执行切点的类
	 * @return
	 * @throws Throwable
	 */
	public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
		boolean proceed = false;// joinPoint有没有process
		Object ret = null;
		try {
			Signature signature = joinPoint.getSignature();
			Object[] args = joinPoint.getArgs();
			String funcName = signature.getName();// 方法名
			Class<?> targetClazz = joinPoint.getTarget().getClass();
			String clazzname = targetClazz.getName();
			String clazznameRegexp = jsAspectConf.getClazznameRegexp();
			/*
			 * 对于dubbo接口和hessian接口，在本地的实现类是jdk动态代理。
			 */
			if (clazzname.startsWith("com.sun.proxy")) {
				// mybatis mapper接口，hessian接口
				clazzname = findClazzname(targetClazz, clazznameRegexp );
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
			if (isExclude(clazzname, clazznameRegexpExlude )) {
				return joinPoint.proceed();
			}
			String appName = jsAspectConf.getAppName();
			// String mkey = key + "." + funcName;
			// 记录方法的执行
			saveInvoke(appName , clazzname, funcName);
			// 保存入参到文件
			/*
			 * #issue 1
			 * 被拦截方法中有HttpRequest、HttpSession、HttpResponse等参数时，这样的对象不应该通过json序列化
			 * 因为会调用getInputStream这样的方法。
			 */
			Object[] argsToLog = new Object[args.length];
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];
				if (arg != null) {

				}
			}
			saveParam(appName, clazzname, funcName, argsToLog);
			try {
				ret = joinPoint.proceed();
			} catch (Exception e) {
				// 保存异常到文件
				saveException(appName, clazzname, funcName, e);
			} finally {
				proceed = true;
			}
			// 保存出参到文件
			saveResult(appName, clazzname, funcName, ret);
		} catch (Exception e) {
			logger.warn("LogAspect执行js异常", e);
			if (!proceed) {
				ret = joinPoint.proceed();
			}
		}
		return ret;
	}

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

	private void saveInvoke(String appName, String clazzname, String funcName) throws Exception {
		String date = new SimpleDateFormat(DateFormat).format(new Date());
		String dataDir = jsAspectConf.getDataDir();
		File file = new File(dataDir , date + "/method-invoke.log");
		makeFileNX(file);
		appendToFile(file, appName + "." + clazzname + "." + funcName);
	}

	private void appendToFile(File file, String string) throws Exception {
		try (BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(file, true), "utf-8"))) {
			writer.write(string);
			writer.append(System.lineSeparator());
		}
	}

	private void makeParentFileNX(File file) {
		if (file.exists()) {
			return;
		}
		file.getParentFile().mkdirs();
	}

	private void makeFileNX(File file) throws IOException {
		if (file.exists()) {
			return;
		}
		file.getParentFile().mkdirs();
		file.createNewFile();
	}

	private void saveResult(String appName, String clazzname, String funcName, Object ret) throws Exception {
		String date = new SimpleDateFormat(DateFormat).format(new Date());
		String dataDir = jsAspectConf.getDataDir();
		File file = new File(dataDir , date + "/" + appName + "/" + clazzname.replaceAll("\\.", "/"));
		makeParentFileNX(file);
		appendToFile(file, funcName + "出参：" + System.lineSeparator() + JSON.toJSONString(ret, true));
	}

	private void saveException(String appName, String clazzname, String funcName, Exception e) throws Exception {
		String date = new SimpleDateFormat(DateFormat).format(new Date());
		String dataDir = jsAspectConf.getDataDir();
		File file = new File(dataDir, date + "/" + appName + "/" + clazzname.replaceAll("\\.", "/"));
		makeParentFileNX(file);
		appendToFile(file, funcName + "出参（异常）：" + System.lineSeparator() + e.getClass().getName()
				+ System.lineSeparator() + e.getMessage());
	}

	private void saveParam(String appName, String clazzname, String funcName, Object[] args) throws Exception {
		String date = new SimpleDateFormat(DateFormat).format(new Date());
		String dataDir = jsAspectConf.getDataDir();
		File file = new File(dataDir, date + "/" + appName + "/" + clazzname.replaceAll("\\.", "/"));
		makeParentFileNX(file);
		// beforeFilter
		String in = JSON.toJSONString(args, config, SerializerFeature.PrettyFormat);
		appendToFile(file, funcName + "入参：" + in);
	}
}
