package org.sirenia.test.start;

import java.util.HashSet;
import java.util.Set;

public class JsAspectConf {
	//private Logger logger = LoggerFactory.getLogger(this.getClass());
	private String appName;
	
	public Set<String> methodSet = new HashSet<>();
	private String dataDir = "d:/jyd-test";
	
	public void setAppName(String appName) {
		this.appName = appName;
	}
	public String getAppName() {
		return appName;
	}
	public Set<String> getMethodSet() {
		return methodSet;
	}
	public String getDataDir() {
		return dataDir;
	}
	public void setDataDir(String dataDir) {
		this.dataDir = dataDir;
	}
	public String getJsSetDir(){
		return dataDir+"/method-set";
	}
	public String getJsDir(){
		return dataDir+"/method-js";
	}
	
	
}
