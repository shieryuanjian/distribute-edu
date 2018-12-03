/**
 * 文件名：IConnectionMonitorThread.java
 *
 * 创建人：胡磊 - lei.hu@99bill.com
 *
 * 创建时间：2017年6月7日 下午5:23:38
 *
 * 版权所有：快钱支付清算信息有限公司
 */
package com.billl99.network;

/**
 * 监控连接池中的链接
 *
 * @author 胡磊 - lei.hu@99bill.com
 * @version 1.0 Created on 2017年6月7日 下午5:23:38
 */
public interface IConnectionMonitorThread {

	/**
	 * 开始监控
	 * 
	 * @author: 胡磊 - lei.hu@99bill.com 
	 * @date: Created on 2017年6月7日 下午5:27:48
	 */
	public void startMonitor();

	/**
	 * 关闭线程
	 *
	 * @author: 胡磊 - lei.hu@99bill.com
	 * @date: Created on 2017年6月7日 下午5:23:57
	 */
	public void shutdown();
}
