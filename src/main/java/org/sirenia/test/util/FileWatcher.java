package org.sirenia.test.util;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;

import org.sirenia.test.util.Callback.Callback10;

public class FileWatcher {
	private String dir;
	private List<Kind<Path>> kindList = new ArrayList<>();
	private WatchService watcher;
	public FileWatcher withDir(String dir){
		this.dir = dir;
		return this;
	}
	/**
	 * 参考StandardWatchEventKinds
	 * @param kind
	 * @return
	 */
	public FileWatcher withKind(Kind<Path> kind){
		kindList.add(kind);
		return this;
	}
	/**
	 * 参考WatchEvent
	 * @param cb
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public void watch(Callback10<WatchEvent<?>> cb) throws IOException, InterruptedException{
			watchInternal(cb);
	}
	public void unwatch(){
		try {
			if(watcher!=null){
				watcher.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	private void watchInternal(Callback10<WatchEvent<?>> cb) throws IOException, InterruptedException{
		watcher = FileSystems.getDefault().newWatchService();
		Path path = FileSystems.getDefault().getPath(dir);
		//WatchKey key = path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
		WatchKey key = path.register(watcher, kindList.toArray(new Kind[kindList.size()]));
		while (true) {  
            key = watcher.take();  
            for (WatchEvent<?> event : key.pollEvents()) { 
               /* //获取文件名
                String fileName = event.context().toString();
                //检查文件名是否符合要求
                if("test.txt".equals(fileName)){
                    String filePath = path.toFile().getAbsolutePath()+File.separator+fileName;
                    System.out.println(new JFile(filePath).text());
                }*/
            	cb.apply(event);
            }
            key.reset();  
        }  
	}
	public static void main(String[] args) throws IOException, InterruptedException {
		final int[] count = {0};
		final FileWatcher w = new FileWatcher().withDir("d:/tomcat/test").withKind(StandardWatchEventKinds.ENTRY_MODIFY);
		Thread t = new Thread(new Runnable(){
			@Override
			public void run() {
				try{
					w.watch(new Callback10<WatchEvent<?>>(){
						@Override
						public void apply(WatchEvent<?> event) {
							String filename = event.context().toString();
							System.out.println(filename);
							count[0]++;
							if(count[0]%3 == 0){
							}
						}
					});
					System.out.println("after");
				}catch(ClosedWatchServiceException e){
					System.out.println("取消监听");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		t.setDaemon(true);
		t.start();
		Thread.sleep(3000);
		w.unwatch();
		System.in.read();
	}
}
