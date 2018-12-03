package com.billl99.network;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;


public class HttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClient.class);

    @Autowired
    private CloseableHttpClient httpclient;

    /**
     * HTTP连接超时:20s
     */
    private static final int CONNECT_TIMEOUT = 20 * 1000;

    /**
     * 读取超时时间：30s
     **/
    private static final int SOCKET_TIMEOUT = 30 * 1000;


    private static final int SO_TIMEOUT = 50 * 1000;

    /**
     * @return java.lang.String
     * @Author yong.shi.nj
     * @Description 使用json格式请求
     * @Date 18:16 2018/12/3
     * @Param [requestStr, url]
     **/

    public String doPost(String requestStr, String url) throws Exception {
        return doPost(requestStr, url, "application/json", "UTF-8");
    }

    /**
     * @return java.lang.String
     * @Author yong.shi.nj
     * @Description http请求
     * @Date 18:15 2018/12/3
     * @Param [requestStr, url, contentType, charset]
     **/

    public String doPost(String requestStr, String url, String contentType, String charset) throws Exception {
        String responseString = "";
        CloseableHttpResponse httpResponse = null;
        HttpPost httpPost = null;

        try {
            httpPost = new HttpPost(url);
            httpPost.setEntity(new StringEntity(requestStr, ContentType.create(contentType, charset)));

            LOGGER.info("start to send request... url={}, requestString={}", url, requestStr);
            CloseableHttpClient closeHttpClient = getHttpClient();
            httpResponse = closeHttpClient.execute(httpPost);

            int resStatu = httpResponse.getStatusLine().getStatusCode();
            HttpEntity entity = httpResponse.getEntity();
            responseString = EntityUtils.toString(entity, charset);
            LOGGER.info("HttpUtil postHttpRequest | resStatu={}, respContent={}", resStatu, responseString == null ? "null" :
                    responseString.length() > 500 ? responseString.substring(0, 500) : responseString);

            if (resStatu != 200) {
                responseString = "连接失败";
                return responseString;
            }

            return responseString;
        } finally {
            if (httpResponse != null) {
                try {
                    EntityUtils.consume(httpResponse.getEntity());
                    httpResponse.close();
                } catch (Exception e) {
                    LOGGER.error("HttpUtil postHttpRequest close httpResponse failed.| Error", e);
                }
            }

            try {
                if (httpPost != null) {
                    httpPost.releaseConnection();
                }
            } catch (Exception e) {
                LOGGER.error("HttpUtil postHttpRequest releaseConnection failed.", e);
            }
        }
    }

    /**
     * @return org.apache.http.impl.client.CloseableHttpClient
     * @Author yong.shi.nj
     * @Description 获取http连接
     * @Date 18:14 2018/12/3
     * @Param [timeoutDto]
     **/
    private CloseableHttpClient getHttpClient() {
        //设置一个是否使用http连接池的开关
        String flag = "1";

        //这边要在常量类中定义一个参数，不要使用魔法数字
        if ("1".equals(flag)) {
            LOGGER.info("使用http client连接池...");
            return httpclient;
        }

        LOGGER.info("新建http链接，而非使用连接池...");
        // 默认禁用ssl证书校验
        final SSLConnectionSocketFactory sslsf;
        try {
            sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault(), NoopHostnameVerifier.INSTANCE);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectionRequestTimeout(SO_TIMEOUT);
        builder.setConnectTimeout(CONNECT_TIMEOUT);
        builder.setSocketTimeout(SOCKET_TIMEOUT);
        RequestConfig requestConfig = builder.build();

        CloseableHttpClient httpClientNew = HttpClients.custom().setSSLSocketFactory(sslsf)
                .setDefaultRequestConfig(requestConfig).build();

        return httpClientNew;
    }
}
