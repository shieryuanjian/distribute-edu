/**
 * 文件名：HttpConnectionKeepAliveStrategy.java
 *
 * 创建人：胡磊 - lei.hu@99bill.com
 *
 * 创建时间：2017年6月7日 下午4:47:05
 *
 * 版权所有：快钱支付清算信息有限公司
 */
package com.billl99.network;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HttpContext;

/**
 * <p>[描述信息：说明类的基本功能]</p>
 *
 * @author 胡磊 - lei.hu@99bill.com
 * @version 1.0 Created on 2017年6月7日 下午4:47:05
 */
public class HttpConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {

	/**
	 * 长连接保持的时间--单位：毫秒
	 */
	private int keepAlive;

	/**
	 * 
	 * @param keepAlive	长连接保持的时间--单位：毫秒
	 */
	public HttpConnectionKeepAliveStrategy(int keepAlive) {
		this.keepAlive = keepAlive;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.apache.http.conn.ConnectionKeepAliveStrategy#getKeepAliveDuration(org.apache.http.HttpResponse, org.apache.http.protocol.HttpContext)
	 * @author: 胡磊 - lei.hu@99bill.com 
	 * @date: Created on 2017年6月7日 下午4:47:31
	 */
	public long getKeepAliveDuration(HttpResponse httpresponse, HttpContext httpcontext) {
		HeaderElementIterator it = new BasicHeaderElementIterator(httpresponse.headerIterator("Keep-Alive"));
		do {
			if (!it.hasNext()){
				break;
			}
			HeaderElement he = it.nextElement();
			String param = he.getName();
			String value = he.getValue();
			if (value != null && param.equalsIgnoreCase("timeout")){
				try {
					return Long.parseLong(value) * 1000L;
				} catch (NumberFormatException ignore) {
				}
			}

		} while (true);

		// 可以针对特殊路由，单独设置保持时间
		// HttpHost target = (HttpHost) context.getAttribute(
		// ExecutionContext.HTTP_TARGET_HOST);
		// if ("www.99bill.com".equalsIgnoreCase(target.getHostName())) {
		// return 5*1000;
		// }

		return (long) keepAlive;
	}

}
