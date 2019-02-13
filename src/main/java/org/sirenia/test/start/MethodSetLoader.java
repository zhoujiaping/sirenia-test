package org.sirenia.test.start;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;

import org.sirenia.test.util.FileWatcher;
import org.sirenia.test.util.callback.Callback10;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

public class MethodSetLoader {
	private final Logger logger = LoggerFactory.getLogger(MethodSetLoader.class);
	private AspectConf aspectConf;
	public void setAspectConf(AspectConf aspectConf) {
		this.aspectConf = aspectConf;
	}
	public void init(){
		load();
	}
	public void load() {
		File dirFile;
		try {
			String dir = aspectConf.getDir()+"/method-set";
			dirFile = ResourceUtils.getFile(dir);
			String filename = dir+"/method-set.txt";
			parseFile(new File(filename));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		final String dirFilePath = dirFile.getAbsolutePath();
		//开启一个守护线程，监听文件
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				FileWatcher watcher = new FileWatcher().withDir(dirFilePath).withKind(StandardWatchEventKinds.ENTRY_MODIFY);
				watcher.watch(new Callback10<WatchEvent<?>>() {
					@Override
					public void apply(WatchEvent<?> event) {
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
					}
				});
			}
		});
		t.setDaemon(true);
		t.start();
	}
	private void parseFile(File file){
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), "utf-8"));) {
			aspectConf.methodSet.clear();
			String line = null;
			while ((line = br.readLine()) != null) {
				logger.info("{}",line);
				if(!line.matches("^\\s*#.*")){
					aspectConf.methodSet.add(line.trim());
				}
			}
		} catch (Exception e) {
			logger.error("读取文件异常，文件：【{}】", file.getName());
			throw new RuntimeException(e);
		}
	}
}
