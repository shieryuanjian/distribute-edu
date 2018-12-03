package com.billl99.network;/**
 * 应用模块名称<p>
 * 代码描述<p>
 * Copyright: Copyright (C) 2018 XXX, Inc. All rights reserved. <p>
 * Company: 快钱<p>
 *
 * @author yong.shi.nj
 * @since 2018/12/3 15:04
 */

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

import javax.net.ssl.SSLContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 *@ClassName CloseableHttpClientUtils
 *@Description TODO
 *@Author yong.shi.nj
 *@Date 2018/12/3 15:04
 *@Version 1.0
 **/
public class CloseableHttpClientUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloseableHttpClientUtils.class);



    /**
     * 设置最大连接数
     **/
    private static final  int MAX_TOTAL = 150;
    /**
     * 设置每台主机最大连接数
     **/
    private static final  int DEFAULT_MAX_PREROUTE = 150;

    /**
     * 设置连接超时时间，单位毫秒
     **/
    private static final int DEFAULT_SOCKET_TIMEOUT = 20000;


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
    private Boolean hostnameVerify = true;
    
    /**
     *@Author yong.shi.nj
     *@Description 
     *@Date 15:23 2018/12/3
     *@Param []
     *@return org.apache.http.conn.HttpClientConnectionManager
     **/
    HttpClientConnectionManager initManage() throws MalformedURLException {
        PoolingHttpClientConnectionManager connManager = null;
        if (!hostnameVerify) {
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

        SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(DEFAULT_SOCKET_TIMEOUT).build();
        connManager.setDefaultSocketConfig(socketConfig);

        LOGGER.info("创建HttpClientConnectionManager对象，参数maxTotal={}, defaultMaxPerRoute={}, defaultSocketTimeout={}, ",
                MAX_TOTAL, DEFAULT_MAX_PREROUTE, DEFAULT_SOCKET_TIMEOUT);

        if (targetHttpRouter != null) {
            LOGGER.info("针对指定主机进行连接池设置....");

            for (String path : targetHttpRouter.keySet()) {
                URL url = new URL(path);
                int port = url.getPort() > 0 ? url.getPort() : url.getDefaultPort();

                LOGGER.info("指定的主机url为：{}，解析后的host={}, port={}, 最大连接池为：{}", path, url.getHost(), port,
                        targetHttpRouter.get(path));

                // 将目标主机的最大连接数增加
                HttpHost httpHost = new HttpHost(url.getHost(), port);
                connManager.setMaxPerRoute(new HttpRoute(httpHost), targetHttpRouter.get(path));
            }
        }

        if (cleanThread == null){
            cleanThread = new DefaultConnectionMonitorThread(connManager, 60);

        }
        if (keepAliveStrategy == null){
            keepAliveStrategy = new HttpConnectionKeepAliveStrategy(60000);
        }

        cleanThread.startMonitor();
        
        return connManager;
    }
}
