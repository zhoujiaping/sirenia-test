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
	private JsAspectConf jsAspectConf;
	public int getOrder() {
		return 0;
	}
	public void setAspectConf(JsAspectConf jsAspectConf) {
		this.jsAspectConf = jsAspectConf;
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
			String key = jsAspectConf.getDataHome();
			Object jsObject = JsInvoker.evalFile(ResourceUtils.getFile(key+"/mock.js"));
			if(JsInvoker.getOwnKeys(jsObject, true).contains(clazzname.replaceAll("\\.", "/")+"#"+funcName)){
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
