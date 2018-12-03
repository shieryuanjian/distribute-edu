package com.billl99.network;

import org.apache.http.HttpHost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import javax.net.ssl.SSLContext;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * http连接池管理器工厂
 *
 * @author 胡磊 - lei.hu@99bill.com
 * @version 1.0 Created on 2016年12月28日 上午10:32:37
 */
public class HttpClientConnectionManagerFactoryBean
		implements FactoryBean<HttpClientConnectionManager>, InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(HttpClientConnectionManagerFactoryBean.class);

	private HttpClientConnectionManager manager;

	/**
	 * 设置最大连接数
	 **/
	private static final  int MAX_TOTAL = 400;

	/**
	 * 设置每台主机最大连接数
	 **/
	private static final  int DEFAULT_MAX_PREROUTE = 200;

	/**
	 * 服务端多久没有接收到客户端请求，自动断开连接
	 **/
	private static final  int SO_TIMEOUT = 50 * 1000;

	private Map<String, Integer> targetHttpRouter;

	/**
	 * 自动清理超时链接的线程
	 */
	private IConnectionMonitorThread cleanThread;

	/**
	 * 设置长连接的超时时间
	 */
	private ConnectionKeepAliveStrategy keepAliveStrategy;


	/**
	 * 是否验证证书的域名
	 */
	private static final boolean HOSTNAME_VERIFY = true;




	public void afterPropertiesSet() throws Exception {
		Assert.isTrue(MAX_TOTAL > 0, "The maxTotal must be greater than 0.");
		Assert.isTrue(DEFAULT_MAX_PREROUTE > 0, "The defaultMaxPerRoute must be greater than 0.");
		Assert.isTrue(SO_TIMEOUT > 0, "The defaultSocketTimeout must be greater than 0.");

		PoolingHttpClientConnectionManager connManager = null;
		if (!HOSTNAME_VERIFY) {
			final SSLConnectionSocketFactory sslsf;
			try {
				sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault(), NoopHostnameVerifier.INSTANCE);
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
			final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory> create()
					.register("http", new PlainConnectionSocketFactory()).register("https", sslsf).build();

			connManager = new PoolingHttpClientConnectionManager(registry);
		} else {
			connManager = new PoolingHttpClientConnectionManager();
		}

		connManager.setMaxTotal(MAX_TOTAL);
		connManager.setDefaultMaxPerRoute(DEFAULT_MAX_PREROUTE);
		SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(SO_TIMEOUT).build();
		connManager.setDefaultSocketConfig(socketConfig);

		logger.info("创建HttpClientConnectionManager对象，参数maxTotal={}, defaultMaxPerRoute={}, defaultSocketTimeout={}, ",
				MAX_TOTAL, DEFAULT_MAX_PREROUTE, SO_TIMEOUT);

		if (targetHttpRouter != null) {
			logger.info("针对指定主机进行连接池设置....");

			for (String path : targetHttpRouter.keySet()) {
				URL url = new URL(path);
				int port = url.getPort() > 0 ? url.getPort() : url.getDefaultPort();

				logger.info("指定的主机url为：{}，解析后的host={}, port={}, 最大连接池为：{}", path, url.getHost(), port,
						targetHttpRouter.get(path));

				// 将目标主机的最大连接数增加
				HttpHost httpHost = new HttpHost(url.getHost(), port);
				connManager.setMaxPerRoute(new HttpRoute(httpHost), targetHttpRouter.get(path));
			}
		}

		if (cleanThread == null) {
			cleanThread = new DefaultConnectionMonitorThread(connManager, 60);
		}
		if (keepAliveStrategy == null) {
			keepAliveStrategy = new HttpConnectionKeepAliveStrategy(60000);
		}
		cleanThread.startMonitor();

		this.manager = connManager;
	}


	public HttpClientConnectionManager getObject() throws Exception {
		return manager;
	}


	public Class<?> getObjectType() {
		return this.manager == null ? PoolingHttpClientConnectionManager.class : this.manager.getClass();
	}

	public boolean isSingleton() {
		return true;
	}

	public void destroy() throws Exception {
		logger.info("Begin destory HttpClientConnectionManager...");
		if (manager != null){
			manager.shutdown();
		}
		if (cleanThread != null){
			cleanThread.shutdown();
		}

		logger.info("HttpClientConnectionManager destoryed!");
	}

	public void setTargetHttpRouter(Map<String, Integer> targetHttpRouter) {
		this.targetHttpRouter = targetHttpRouter;
	}


	public void setCleanThread(IConnectionMonitorThread cleanThread) {
		this.cleanThread = cleanThread;
	}

	public void setKeepAliveStrategy(ConnectionKeepAliveStrategy keepAliveStrategy) {
		this.keepAliveStrategy = keepAliveStrategy;
	}

}
