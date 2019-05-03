package org.sirenia.test.start;

import java.lang.reflect.Method;

import org.sirenia.test.util.JsInvoker;
import org.sirenia.test.util.MethodUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcResult;

public class DubboConsumerMethodFilter implements Filter {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private volatile JsAspectConf jsAspectConf;
	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		if(jsAspectConf==null){
			jsAspectConf = AppContextHolder.getApplicationContext().getBean(JsAspectConf.class);
		}
		String interfaceName = invoker.getInterface().getName();
		Object[] args = invocation.getArguments();
		String methodName = invocation.getMethodName();
		try{
			String key = jsAspectConf.getDataHome();
			Object jsObject = JsInvoker.evalFile(ResourceUtils.getFile(key+"/mock.js"));
			if(JsInvoker.getOwnKeys(jsObject, true).contains(interfaceName.replaceAll("\\.", "/")+"#"+methodName)){
				Object ret = JsInvoker.invokeJsMethod(jsObject, methodName, args);
				if(ret == null){
					return null;
				}
				Method method = MethodUtil.getMethodByName(interfaceName, methodName);
				RpcResult result = new RpcResult();
				Object value = MethodUtil.parseJSONForReturnType(method , ret.toString());
				result.setValue(value);
				return result;
			}
		}catch(Exception e){
			logger.error("JsAspect执行js异常");
			throw new RuntimeException(e);
		}
		Result r = invoker.invoke(invocation);
		return r;
	}

}
