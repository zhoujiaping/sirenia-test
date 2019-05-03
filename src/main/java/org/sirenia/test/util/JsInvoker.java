package org.sirenia.test.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StreamUtils;

/**
 * 这个工具类，可以执行js脚本。js脚本中可以调用java类或者java对象的方法。返回值可以是java对象。 主要可以用来为java添加动态性。
 * 和java动态加载类的方式相比，这里的方式不会污染jvm环境。 和使用groovy相比，不用引入groovy一大堆包。对java支持没groovy好。
 * 和使用http代理相比，这里是本地调用，性能上有优势，在处理事务时不会引入分布式事务问题。
 * 
 * 重复加载文件的问题已经通过缓存解决。 增加了目录监听功能，如果文件发生修改，会在下一次执行该脚本的函数时，读取文件内容兵保存到缓存。
 * 重复eval脚本内容的问题已经通过缓存解决。 注意脚本的执行上下文，如果有多个脚本文件，后面eval的脚本变量（包括函数）会覆盖前面eval的脚本变量，
 * 所以需要开发者自己控制。
 * 
 * 其实脚本方案，这个有严重缺陷。通过脚本引擎执行js，js无法直接做文件io、网络io等。
 * https://www.cnblogs.com/qiumingcheng/p/7355456.html
 * https://docs.oracle.com/javase/8/docs/technotes/guides/scripting/nashorn/toc.
 * html
 * 
 * js脚本中的load不会返回到java。所以不用那种方式。
 * 
 * 参考 jdk.nashorn.internal.objects.Global
 */
public class JsInvoker {
	public static final Logger logger = LoggerFactory.getLogger(JsInvoker.class);
	public static ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
	private static Invocable invocable = (Invocable) engine;
	private static Map<String, Object> jsObjectCache = new ConcurrentHashMap<>();
	private static Map<String, Long> filelastModifyTimeStamp = new ConcurrentHashMap<>();// filename->文件修改的时间戳
	private static Map<String, Method> scriptObjectMirrorMethods = new ConcurrentHashMap<>();
	private static String template;
	private static Lock lock = new ReentrantLock();
	/**
	 * 每一个js file经过eval之后都要返回一个js对象。
	 * 
	 */
	static {
		try {
			// Bindings bindings =
			// engine.getBindings(ScriptContext.ENGINE_SCOPE);
			// bindings.put("__root",
			// getKey(ResourceUtils.getFile("classpath:js")));
			DefaultResourceLoader rl = new DefaultResourceLoader();
			Resource r = rl.getResource("classpath:template/mock.js");
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(r.getInputStream(), Charset.forName("utf-8")))) {
				String line = null;
				StringBuilder text = new StringBuilder();
				while ((line = br.readLine()) != null) {
					text.append(line).append(System.lineSeparator());
				}
				template = text.toString();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String readFileText(File file) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append(System.lineSeparator());
			}
			return sb.toString();
		}
	}

	private static String getKey(File file) {
		return file.getAbsolutePath();
	}

	/*
	 * private static void genCode(){ for(int i=1;i<=20;i++){
	 * System.out.println("invoker.invoke"+i+" = function(target,method,args){"
	 * ); List<String> params = new ArrayList<>(); for(int j=0;j<i;j++){
	 * params.add("args["+j+"]"); } System.out.println(
	 * "    return target[method]("+String.join(",", params)+");");
	 * System.out.println("};"); } }
	 */
	/**
	 * 返回js对象
	 * 
	 * @param file
	 * @return
	 * @throws ScriptException
	 * @throws IOException 
	 */
	public static Object evalFile(File file) throws ScriptException, IOException {
		try {
			lock.lock();
			mkFileNx(file);
			String key = getKey(file);
			Long prevLastModifyTimeStamp = filelastModifyTimeStamp.get(key);
			Long lastModifyTimeStamp = file.lastModified();
			String fileText = null;
			if (lastModifyTimeStamp.equals(prevLastModifyTimeStamp)) {
				return jsObjectCache.get(key);
			} else {
				fileText = readFileText(file);
				Object jsObject = engine.eval(fileText);
				jsObjectCache.put(key, jsObject);
				filelastModifyTimeStamp.put(key, lastModifyTimeStamp);
				return jsObject;
			}
		} finally {
			lock.unlock();
		}
	}
	private static void mkFileNx(File file) throws IOException{
		if(!file.exists()){
			File parent = file.getParentFile();
			if(!parent.exists()){
				if(!parent.mkdirs()){
					throw new RuntimeException("创建目录【"+parent.getAbsolutePath()+"】失败");
				}
			}
			if(!file.createNewFile()){
				throw new RuntimeException("创建文件【"+file.getAbsolutePath()+"】失败");
			}
			StreamUtils.copy(template, Charset.forName("utf-8"), new FileOutputStream(file));
		}
	}
	public static Object evalText(String key, String text) throws ScriptException {
		try {
			lock.lock();
			if (key == null) {
				return engine.eval(text);
			}
			Object jsObject = jsObjectCache.get(key);
			if (jsObject == null) {
				jsObject = engine.eval(text);
				jsObjectCache.put(key, jsObject);
			}
			return jsObject;
		} finally {
			lock.unlock();
		}
	}

	public static Object invokeJavaStaticMethod(String clazzName, String method, Object... args) throws ClassNotFoundException, ScriptException, NoSuchMethodException, IOException {
		return invokeJavaStaticMethod(Class.forName(clazzName), method, args);
	}

	public static Object invokeJavaStaticMethod(Class<?> clazz, String method, Object... args) throws ScriptException, NoSuchMethodException, IOException {
		try {
			lock.lock();
			Object invoker = evalFile(ResourceUtils.getFile("classpath:js/builtin/java.js"));
			return invocable.invokeMethod(invoker, "invokeStatic", clazz.getName(), method, args);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 
	 * @param target
	 *            java对象
	 * @param method
	 *            java方法名
	 * @param args
	 *            java参数
	 * @return
	 * @throws ScriptException 
	 * @throws NoSuchMethodException 
	 * @throws IOException 
	 */
	public static Object invokeJavaMethod(Object target, String method, Object... args) throws ScriptException, NoSuchMethodException, IOException {
		try {
			lock.lock();
			Object invoker = evalFile(ResourceUtils.getFile("classpath:js/builtin/java.js"));
			return invocable.invokeMethod(invoker, "invoke", target, method, args);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 
	 * @param target
	 *            js对象
	 * @param method
	 *            js方法名
	 * @param args
	 *            js参数
	 * @return
	 * @throws ScriptException 
	 * @throws NoSuchMethodException 
	 */
	public static Object invokeJsMethod(Object target, String method, Object... args) throws NoSuchMethodException, ScriptException {
		try {
			lock.lock();
			Object res = invocable.invokeMethod(target, method, args);
			return res;
		} finally {
			lock.unlock();
		}
	}

	public static Set<String> getOwnKeys(Object jsObject, boolean all) throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String methodName = "getOwnKeys";
		Method method = scriptObjectMirrorMethods.get(methodName);
		if (method == null) {
			method = jsObject.getClass().getMethod(methodName, boolean.class);
			scriptObjectMirrorMethods.put(methodName, method);
		}
		String[] keys = (String[]) method.invoke(jsObject, all);
		Set<String> set = new TreeSet<>();
		for (int i = 0; i < keys.length; i++) {
			set.add(keys[i]);
		}
		return set;
	}

	/**
	 * @param target
	 * @param method
	 * @param args
	 * @return
	 * @throws ScriptException 
	 * @throws NoSuchMethodException 
	 * @throws IOException 
	 */
	public static Object invokeJsMethodReturnJSON(Object target, String method, Object... args) throws ScriptException, NoSuchMethodException, IOException {
		try {
			lock.lock();
			Object jsInvoker = evalFile(ResourceUtils.getFile("classpath:js/builtin/js.js"));
			Object res = invocable.invokeMethod(jsInvoker, "invoke", target, method, args);
			return res;
		} finally {
			lock.unlock();
		}
	}

	public static void main(String[] args)
			throws InterruptedException, ScriptException, NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, SecurityException, IOException {
		// ScriptObjectMirror m;
		Object obj = evalFile(new File("d:/tomcat/test/DataSyncService.js"));
		Object res = obj.getClass().getMethod("hasMember", String.class).invoke(obj, "send");
		res = obj.getClass().getMethod("getOwnKeys", boolean.class).invoke(obj, true);
		System.out.println(res);
		/*
		 * Object http =
		 * evalFile(ResourceUtils.getFile("classpath:js/builtin/http.js"));
		 * Object res = invokeJsMethod(http, "post", "http://www.baidu.com",
		 * ""); System.out.println(res); res = invokeJavaMethod(new Date(),
		 * "getTime"); System.out.println(res); res =
		 * invokeJavaMethod(LocalDate.now(), "atTime", 10, 20, 30); //
		 * LocalDate.now().atTime(10, 20, 30); System.out.println(res); res =
		 * invokeJavaStaticMethod(LocalDate.class, "now");
		 * System.out.println(res); // Object res = invokeMethod2(new
		 * Date(),"getTime"); // System.out.println(res); Object httptest =
		 * evalFile(ResourceUtils.getFile("classpath:js/httptest.js")); res =
		 * invokeJsMethod(httptest, "test"); System.out.println(res);
		 * 
		 * res = invokeJsMethodReturnJSON(httptest, "testjson");
		 * System.out.println(res);
		 */ }

}
