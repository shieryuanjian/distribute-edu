package com.billl99.network;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.Resource;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;

public class CloseableHttpClientFactoryBean implements FactoryBean<CloseableHttpClient>, InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(CloseableHttpClientFactoryBean.class);

	private CloseableHttpClient httpClient = null;

	@Resource(name = "httpClientConnectionManager")
	private HttpClientConnectionManager httpConnManager;

	/**
	 * HTTP连接超时:20s
	 */
	private static final int CONNECT_TIMEOUT = 20 * 1000;

	/**
	 * 读取超时时间：30s
	 **/
	private static final int SOCKET_TIMEOUT = 30 * 1000;


	/**
	 * 默认超时时间，如果没有定义连接和读取超时时间，默认使用此时间
	 **/
	private static final int DEFAULT_SOCKET_TIMEOUT = 20 * 1000;

	/**
	 * 从连接池获取连接的超时时间:1s
	 */
	private static final int CONNECT_REQUEST_TIMEOUT = 1 * 1000;


	/**
	 * 是否支持重定向
	 */
	private static final boolean REDIRECTS_ENABLED = false;

	/**
	 * 重定向次数
	 **/
	private static final int MAX_REDIRECTS = 0;

	/**
	 * 失败重试次数：3次
	 **/
	private static final int REQUEST_RETRY_COUNT = 3;

	/**
	 * 是否支持GZIP压缩
	 **/
	private static final boolean ENABLE_GZIP = false;


	public void afterPropertiesSet() throws Exception {
		logger.info(
				"创建httpclient对象，connectTimeout={}, redirectsEnabled={}, maxRedirects={}, socketTimeout={}, requestRetryCount={}",
				CONNECT_TIMEOUT, REDIRECTS_ENABLED, MAX_REDIRECTS, SOCKET_TIMEOUT, REQUEST_RETRY_COUNT);

		RequestConfig.Builder builder = RequestConfig.custom();

		// if (staleConnectionCheckEnabled != null) {
		// builder.setStaleConnectionCheckEnabled(staleConnectionCheckEnabled);
		// }

		builder.setRedirectsEnabled(REDIRECTS_ENABLED);

		if (MAX_REDIRECTS > 0) {
			builder.setMaxRedirects(MAX_REDIRECTS);
		}
		// 不允许进行鉴权
		builder.setAuthenticationEnabled(false);
		builder.setConnectTimeout(CONNECT_TIMEOUT);
		builder.setSocketTimeout(CONNECT_TIMEOUT);
		builder.setConnectionRequestTimeout(CONNECT_REQUEST_TIMEOUT);
		// 请求重试处理
		HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {

			public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
				logger.info("httpclient 判断是否可以补偿查询，当前第 {} 次，异常名：{}。", executionCount,
						exception == null ? "null" : exception.getClass().getName());

				// 如果已经重试了3次，就放弃
				if (executionCount >= REQUEST_RETRY_COUNT) {
					return false;
				}
				// 如果服务器丢掉了连接，那么就重试
				if (exception instanceof NoHttpResponseException) {
					return true;
				}
				// 如果服务器丢掉了连接，那么就重试
				if (exception instanceof java.net.SocketException) {
					// 针对新增的判断逻辑，增加日志监控
					logger.info("java.net.SocketException异常情况下，返回继续重试标志，错误信息：{}，第 {} 次。", exception.getMessage(),
							executionCount);

					return true;
				}
				// 不要重试SSL握手异常
				if (exception instanceof SSLHandshakeException) {
					return false;
				}
				// 超时
				if (exception instanceof InterruptedIOException) {
					return false;
				}
				// 目标服务器不可达
				if (exception instanceof UnknownHostException) {
					return false;
				}
				// 连接被拒绝
				if (exception instanceof ConnectTimeoutException) {
					return false;
				}
				// SSL握手异常
				if (exception instanceof SSLException) {
					return false;
				}

				HttpClientContext clientContext = HttpClientContext.adapt(context);
				HttpRequest request = clientContext.getRequest();
				// 如果请求是幂等的，就再次尝试
				if (!(request instanceof HttpEntityEnclosingRequest)) {
					return true;
				}

				return !clientContext.isRequestSent();
			}
		};

		RequestConfig requestConf = builder.build();
		HttpClientBuilder htBuilder = HttpClients.custom().setConnectionManager(httpConnManager)
				.setDefaultRequestConfig(requestConf).setRetryHandler(httpRequestRetryHandler);

		if (ENABLE_GZIP) {
			logger.info("启用了Gzip压缩....");

			// 拦截器：增加gzip压缩请求
			htBuilder.addInterceptorFirst(new HttpRequestInterceptor() {

				public void process(final HttpRequest request, final HttpContext context)
						throws HttpException, IOException {
					if (!request.containsHeader("Accept-Encoding")) {
						request.addHeader("Accept-Encoding", "gzip");
					}
				}
			});

			// 拦截器：返回增加gzip解压
			htBuilder.addInterceptorLast(new HttpResponseInterceptor() {

				public void process(final HttpResponse response, final HttpContext context)
						throws HttpException, IOException {
					HttpEntity entity = response.getEntity();
					if (entity != null) {
						Header ceheader = entity.getContentEncoding();
						if (ceheader != null) {
							HeaderElement[] codecs = ceheader.getElements();
							for (int i = 0; i < codecs.length; i++) {
								if (codecs[i].getName().equalsIgnoreCase("gzip")) {
									response.setEntity(new GzipDecompressingEntity(response.getEntity()));
									return;
								}
							}
						}
					}
				}
			});
		}

		httpClient = htBuilder.build();
	}

	public CloseableHttpClient getObject() throws Exception {
		return httpClient;
	}

	public Class<?> getObjectType() {
		return httpClient == null ? CloseableHttpClient.class : httpClient.getClass();
	}

	public boolean isSingleton() {
		return false;
	}

	public void setHttpConnManager(HttpClientConnectionManager httpConnManager) {
		this.httpConnManager = httpConnManager;
	}
}
