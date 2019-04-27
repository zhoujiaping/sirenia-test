package org.sirenia.test.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.sirenia.test.util.Callback.Callback00;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 该工具可以解决启动时服务依赖问题。 延迟执行，还可以加快系统的启动速度。 比如有些模块的组件在启动时调用一些服务，查询数据缓存起来。
 * 后面再用的时候都不用再调用服务。这样虽然避免了多次调用服务，减少了资源占用，提升了性能， 但是增加了耦合性（启动时的时序耦合）。
 * 使用该工具后的效果，可以保证一些操作最多只执行一次。并且不用在启动时执行。 不过这要对代码稍微调整一下。
 * 可以作为spring的lazy-init的协作工具。使一部分代码即时执行，另一部分代码延迟执行。
 * 
 * @author zjp
 */
public class Defer {
	private static final Logger logger = LoggerFactory.getLogger(Defer.class);

	/** 存放懒执行的key */
	private static Map<String, String> keys = new ConcurrentHashMap<>();
	private static final String VALUE = "";

	/**
	 * 保证只执行一次
	 * 
	 * @param key
	 * @param cb
	 */
	public static void once(String key, Callback00 cb) {
		if (!keys.containsKey(key)) {
			synchronized (Defer.class) {
				if (!keys.containsKey(key)) {
					logger.info("初始化【{}】开始", key);
					keys.put(key, VALUE);
					cb.apply();
					logger.info("初始化【{}】完成", key);
				}
			}

		}
	}

	public static void once(Class<?> klass, Callback00 cb) {
		once(klass.getName(), cb);
	}

	public static void main(String[] args) {
		Defer.once("test", () -> {
			System.out.println("defer");
		});
	}
}
