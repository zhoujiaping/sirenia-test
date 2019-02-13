package org.sirenia.test.start;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.sirenia.test.util.JsInvoker;
import org.sirenia.test.util.MethodUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.util.ResourceUtils;

public class HessianMethodInterceptor implements MethodInterceptor, Ordered {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private AspectConf aspectConf;
	public int getOrder() {
		return 0;
	}
	public void setAspectConf(AspectConf aspectConf) {
		this.aspectConf = aspectConf;
	}
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Object[] args = invocation.getArguments();
		Method method = invocation.getMethod();
		String funcName = method.getName();//方法名
		String clazzname = method.getDeclaringClass().getName();
		int endIndex = clazzname.indexOf('$');
		if(endIndex>0){
			clazzname = clazzname.substring(0, endIndex - 1 );
		}
		try{
			String key = aspectConf.getAppName()+"."+clazzname;
			if(aspectConf.methodSet.contains(key+"."+funcName)){
				Object jsObject = JsInvoker.evalFile(ResourceUtils.getFile(aspectConf.getJsDir()+"/"+key.replaceAll("\\.", "/")+".js"));
				Object ret = JsInvoker.invokeJsMethod(jsObject, funcName, args);
				if(ret == null){
					return null;
				}
				return MethodUtil.parseJSONForReturnType(method, ret.toString());
			}
		}catch(Exception e){
			logger.error("JsAspect执行js异常");
			throw new RuntimeException(e);
		}
		Object ret = invocation.proceed();
		return ret;
	}

}