package org.sirenia.test.start;

import java.util.HashSet;
import java.util.Set;

public class AspectConf {
	//private Logger logger = LoggerFactory.getLogger(this.getClass());
	private String appName;
	
	public Set<String> methodSet = new HashSet<>();
	private String dir = "d:/test-test";
	
	public void setAppName(String appName) {
		this.appName = appName;
	}
	public String getAppName() {
		return appName;
	}
	public Set<String> getMethodSet() {
		return methodSet;
	}
	public void setDir(String dir) {
		this.dir = dir;
	}
	public String getDir() {
		return dir;
	}
	public String getJsSetDir(){
		return dir+"/method-set";
	}
	public String getJsDir(){
		return dir+"/method-js";
	}
	
	
}
