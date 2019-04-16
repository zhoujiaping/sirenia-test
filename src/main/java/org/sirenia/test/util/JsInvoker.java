package org.sirenia.test.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.time.LocalDate;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.sirenia.test.util.Callback.Callback10;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

/**
 * 这个工具类，可以执行js脚本。js脚本中可以调用java类或者java对象的方法。返回值可以是java对象。
 * 主要可以用来为java添加动态性。
 * 和java动态加载类的方式相比，这里的方式不会污染jvm环境。
 * 和使用groovy相比，不用引入groovy一大堆包。对java支持没groovy好。
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
 * 参考
 * jdk.nashorn.internal.objects.Global
 */
public class JsInvoker {
	private static final Logger logger = LoggerFactory.getLogger(JsInvoker.class);
	public static ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
	private static Invocable invocable = (Invocable) engine;
	private static LRUCache<String, Object> jsObjectCache = new LRUCache<>(1000);
	private static LRUCache<String, String> fileChangeCache = new LRUCache<>(1000);// filename->""
	private static Lock lock = new ReentrantLock();
	private static long lockTime = 1000 * 10;
	private static TimeUnit timeUnit = TimeUnit.SECONDS;
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
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String readFileText(File file) {
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append(System.lineSeparator());
			}
			return sb.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void watch(final String dir) {
		Thread t = new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					watchInternal(dir);
				} catch (Exception e) {
					logger.error("监听js文件目录" + dir + "异常", e);
					throw new RuntimeException(e);
				}
			}
		});
		t.setDaemon(true);
		t.start();
	}

	private static void watchInternal(String dir) {
		File dirFile;
		try {
			dirFile = ResourceUtils.getFile(dir);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		final String dirFilePath = dirFile.getAbsolutePath();
		FileWatcher watcher = new FileWatcher().withDir(dirFilePath).withKind(StandardWatchEventKinds.ENTRY_MODIFY);
		watcher.watch(new Callback10<WatchEvent<?>>() {
			@Override
			public void apply(WatchEvent<?> event) {
				String filename = event.context().toString();
				File file = new File(dirFilePath, filename);
				// 優化:不要文件有修改就重新加载文件，而是先记录修改，用的时候才重新加载
				// String fileText = readFileText(file);
				String key = getKey(file);
				markChanged(key);
				System.out.println(file.getName());
				// fileTextCache.put(file.getAbsolutePath(), fileText);
			}
		});
	}
	public static void markChanged(String key){
		if (jsObjectCache.containsKey(key)) {// 只有在缓存中有的才需要标记为已改变
			fileChangeCache.put(key, "");
		}
	}

	private static String getKey(File file) {
		return file.getAbsolutePath();
	}

	public static void main(String[] args)
			throws InterruptedException, ScriptException, NoSuchMethodException, FileNotFoundException {
		watch("classpath:js");
		Object http = evalFile(ResourceUtils.getFile("classpath:js/builtin/http.js"));
		Object res = invokeJsMethod(http, "post", "http://www.baidu.com", "");
		System.out.println(res);
		res = invokeJavaMethod(new Date(), "getTime");
		System.out.println(res);
		res = invokeJavaMethod(LocalDate.now(), "atTime", 10, 20, 30);
		// LocalDate.now().atTime(10, 20, 30);
		System.out.println(res);
		res = invokeJavaStaticMethod(LocalDate.class, "now");
		System.out.println(res);
		// Object res = invokeMethod2(new Date(),"getTime");
		// System.out.println(res);
		Object httptest = evalFile(ResourceUtils.getFile("classpath:js/httptest.js"));
		res = invokeJsMethod(httptest, "test");
		System.out.println(res);
		
		res = invokeJsMethodReturnJSON(httptest,"testjson");
		System.out.println(res);
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
	 */
	public static Object evalFile(File file) {
		try {
			lock.tryLock(lockTime, timeUnit);
			String key = getKey(file);
			String fileText = null;
			if (fileChangeCache.containsKey(key)) {
				fileText = readFileText(file);
				fileChangeCache.remove(key);
				Object jsObject = engine.eval(fileText);
				jsObjectCache.put(key, jsObject);
				return jsObject;
			} else {
				Object jsObject = jsObjectCache.get(key);
				if (jsObject == null) {
					fileText = readFileText(file);
					jsObject = engine.eval(fileText);
					jsObjectCache.put(key, jsObject);
				}
				return jsObject;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			lock.unlock();
		}
	}
	public static Object evalText(String key,String text) {
		try {
			lock.tryLock(lockTime, timeUnit);
			if(key==null){
				return engine.eval(text);
			}
			if (fileChangeCache.containsKey(key)) {
				fileChangeCache.remove(key);
				Object jsObject = engine.eval(text);
				jsObjectCache.put(key, jsObject);
				return jsObject;
			} else {
				Object jsObject = jsObjectCache.get(key);
				if (jsObject == null) {
					jsObject = engine.eval(text);
					jsObjectCache.put(key, jsObject);
				}
				return jsObject;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			lock.unlock();
		}
	}

	public static Object invokeJavaStaticMethod(String clazzName, String method, Object... args) {
		try {
			return invokeJavaStaticMethod(Class.forName(clazzName), method, args);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Object invokeJavaStaticMethod(Class<?> clazz, String method, Object... args) {
		try {
			lock.tryLock(lockTime, timeUnit);
			Object invoker = evalFile(ResourceUtils.getFile("classpath:js/builtin/java.js"));
			return invocable.invokeMethod(invoker, "invokeStatic", clazz.getName(), method, args);
		} catch (Exception e) {
			throw new RuntimeException(e);
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
	 */
	public static Object invokeJavaMethod(Object target, String method, Object... args) {
		try {
			lock.tryLock(lockTime, timeUnit);
			Object invoker = evalFile(ResourceUtils.getFile("classpath:js/builtin/java.js"));
			return invocable.invokeMethod(invoker, "invoke", target, method, args);
		} catch (Exception e) {
			throw new RuntimeException(e);
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
	 */
	public static Object invokeJsMethod(Object target, String method, Object... args) {
		try {
			lock.tryLock(lockTime, timeUnit);
			Object res = invocable.invokeMethod(target, method, args);
			return res;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			lock.unlock();
		}
	}
	/**
	 * @param target
	 * @param method
	 * @param args
	 * @return
	 */
	public static Object invokeJsMethodReturnJSON(Object target, String method, Object... args) {
		try {
			lock.tryLock(lockTime, timeUnit);
			Object jsInvoker = evalFile(ResourceUtils.getFile("classpath:js/builtin/js.js"));
			Object res = invocable.invokeMethod(jsInvoker,"invoke",target, method, args);
			return res;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			lock.unlock();
		}
	}
}
