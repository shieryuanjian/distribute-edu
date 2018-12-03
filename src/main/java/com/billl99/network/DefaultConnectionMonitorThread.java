/**
 * 文件名：ConnectionMonitorThread.java
 *
 * 创建人：胡磊 - lei.hu@99bill.com
 *
 * 创建时间：2017年6月7日 下午5:02:10
 *
 * 版权所有：快钱支付清算信息有限公司
 */
package com.billl99.network;

import org.apache.http.conn.HttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 清理过期的链接
 *
 * @author 胡磊 - lei.hu@99bill.com
 * @version 1.0 Created on 2017年6月7日 下午5:02:10
 */
public class DefaultConnectionMonitorThread extends Thread implements IConnectionMonitorThread {

	private static final Logger logger = LoggerFactory.getLogger(DefaultConnectionMonitorThread.class);

	private final HttpClientConnectionManager connMgr;

	private volatile boolean shutdown;

	/**
	 * 空闲时间  单位：秒
	 */
	private int idleTime;

	/**
	 * 
	 * @param connMgr
	 * @param idleTime	空闲时间  单位：秒
	 */
	public DefaultConnectionMonitorThread(HttpClientConnectionManager connMgr, int idleTime) {
		super();
		this.connMgr = connMgr;
		this.idleTime = idleTime;
		shutdown = false;
		this.setName("idle-connection-monitor");  
	    this.setDaemon(true); 

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see  com.billl99.network.IConnectionMonitorThread#startMonitor()
	 * @author: 胡磊 - lei.hu@99bill.com 
	 * @date: Created on 2017年6月7日 下午5:28:23
	 */
	public void startMonitor() {
		this.start();
	}

	@Override
	public void run() {
		try {
			while (!shutdown) {
				synchronized (connMgr) {
					Thread.sleep(2000L);
					// 关闭过期的连接
					connMgr.closeExpiredConnections();
					// 关闭空闲时间超过指定时间的连接
					connMgr.closeIdleConnections(idleTime, TimeUnit.SECONDS);
				}
			}
		} catch (InterruptedException ex) {
			// 调试好后，这里的异常日志可以屏蔽掉
			logger.error("", ex);
		}
	}

	public void shutdown() {
		synchronized (connMgr) {  
	        shutdown = true;  
	        notifyAll();  
	    }
	}
}
