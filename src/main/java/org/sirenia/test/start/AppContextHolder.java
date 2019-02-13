package org.sirenia.test.start;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.WebApplicationContext;
/**
 * 注意：这个类要在application-mvc.xml中配置，如果在application.xml中配置，只能获取到ApplicationContext
 * 无法获取到WebApplicationContext
 */
public class AppContextHolder implements ApplicationContextAware{
	private static WebApplicationContext webApplicationContext;
	private static ApplicationContext applicationContext;
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		ApplicationContext parent = applicationContext.getParent();
		if(parent==null){
			AppContextHolder.applicationContext = applicationContext;
		}else{
			AppContextHolder.applicationContext = parent;
			AppContextHolder.webApplicationContext = (WebApplicationContext) applicationContext;
		}
	}
	public static ApplicationContext getApplicationContext(){
		return applicationContext;
	}
	public static WebApplicationContext getWebApplicationContext(){
		return webApplicationContext;
	}
	public static Object getBean(String name){
		Object bean = applicationContext.getBean(name);
		if(bean==null){
			bean = webApplicationContext.getBean(name);
		}
		return bean;
	}
}
