package org.sirenia.test.start;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;

import org.sirenia.test.util.Callback.Callback10;
import org.sirenia.test.util.FileWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

public class MethodSetLoader {
	private final Logger logger = LoggerFactory.getLogger(MethodSetLoader.class);
	private JsAspectConf jsAspectConf;
	public void setJsAspectConf(JsAspectConf jsAspectConf) {
		this.jsAspectConf = jsAspectConf;
	}
	public void init(){
		load();
	}
	private void makeFileNX(File file) throws IOException {
		if(file.exists()){
			return;
		}
		file.getParentFile().mkdirs();
		file.createNewFile();
	}
	public void load() {
		File dirFile;
		try {
			String dir = jsAspectConf.getDataDir()+"/method-set";
			dirFile = ResourceUtils.getFile(dir);
			String filename = dir+"/method-set.txt";
			makeFileNX(new File(filename));
			parseFile(new File(filename));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		final String dirFilePath = dirFile.getAbsolutePath();
		//开启一个守护线程，监听文件
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try{
					FileWatcher watcher = new FileWatcher().withDir(dirFilePath).withKind(StandardWatchEventKinds.ENTRY_MODIFY);
					watcher.watch(new Callback10<WatchEvent<?>>() {
						@Override
						public void apply(WatchEvent<?> event) {
							try {
								String filename = event.context().toString();
								// Kind<?> kind = event.kind();
								// System.out.println(kind.type());
								File file = new File(dirFilePath, filename);
								// 優化:不要文件有修改就重新加载文件，而是先记录修改，用的时候才重新加载
								// String fileText = readFileText(file);
								// String key = getKey(file);
								//System.out.println(file.getName());
								logger.info("文件监听处理：【{}】",file.getName());
								parseFile(file);
								// fileTextCache.put(file.getAbsolutePath(), fileText);
							} catch (Exception e) {
								logger.error("处理文件监听异常",e);
							}
						}
					});
				}catch(Exception e){
					logger.error("监听文件异常",e);
					throw new RuntimeException(e);
				}
			}
		});
		t.setDaemon(true);
		t.start();
	}
	private void parseFile(File file){
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), "utf-8"));) {
			jsAspectConf.methodSet.clear();
			String line = null;
			while ((line = br.readLine()) != null) {
				logger.info("{}",line);
				if(!line.matches("^\\s*#.*")){
					jsAspectConf.methodSet.add(line.trim());
				}
			}
		} catch (Exception e) {
			logger.error("读取文件异常，文件：【{}】", file.getName());
			throw new RuntimeException(e);
		}
	}
}
