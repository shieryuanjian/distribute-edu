package com.billl99.network;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *@ClassName HttpUtil
 *@Description http工具类
 *@Author yong.shi.nj
 *@Date 2018/11/30 10:39
 *@Version 1.0
 **/
public class HttpUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUtil.class);

    /**
     * 设置统一的编码格式UTF-8
     **/
    private static final String ENCODING = "UTF-8";
    /**
     * 设置连接超时时间，单位毫秒
     **/
    private static final int CONNECT_TIMEOUT = 20000;
    /**
     *设置获取数据超时时间（服务端响应时间），单位毫秒
     **/
    private static final int SOCKET_TIMEOUT = 50000;

    /**
     *@Author yong.shi.nj
     *@Description http get 调用
     *@Date 16:45 2018/11/30
     *@Param [reqURL, encodeCharset]
     *@return java.lang.String
     **/

    public static String sendGetRequest(String reqURL) {

        LOGGER.info("HTTP请求地址：" + reqURL);

        // 响应内容
        String respContent = "";

        // 创建httpClient实例
        HttpClient httpClient = getClient();

        // 创建HttpGet
        HttpGet httpGet = new HttpGet(reqURL);

        try {
            // 执行GET请求
            HttpResponse response = httpClient.execute(httpGet);
            respContent = getHttpResponseResult(response);
        } catch (Exception e) {
            LOGGER.error("请求通信[" + reqURL + "]时异常,堆栈轨迹如下", e);
        } finally {
            // 关闭连接,释放资源
            httpClient.getConnectionManager().shutdown();
        }

        LOGGER.info("HTTP应答报文：" + respContent);
        return respContent;
    }

    /**
     *@Author yong.shi.nj
     *@Description http post 请求
     *@Date 16:57 2018/11/30
     *@Param [reqURL, reqData, encodeCharset（默认UTF-8）, contentType(建议使用application/json)]
     *@return java.lang.String
     **/

    public static String sendPostRequest(String reqURL, String reqData, String encodeCharset, String contentType) {
        LOGGER.info("HTTP请求地址：" + reqURL);
        // 响应内容
        String respContent = "";
        // 创建httpClient实例
        HttpClient httpClient = getClient();
        // httpPost
        HttpPost httpPost = new HttpPost(reqURL);

        // 由于下面使用的是new
        // StringEntity(....),所以默认发出去的请求报文头中CONTENT_TYPE值为text/plain;
        // charset=ISO-8859-1
        // 这就有可能会导致服务端接收不到POST过去的参数,比如运行在Tomcat6.0.36中的Servlet,所以我们手工指定CONTENT_TYPE头消息
        httpPost.setHeader(HTTP.CONTENT_TYPE, contentType + "; charset=" + ENCODING);

        try {

            httpPost.setEntity(new StringEntity(reqData == null ? "" : reqData, ENCODING));

            HttpResponse response = httpClient.execute(httpPost);
            respContent = getHttpResponseResult(response);

        } catch (Exception e) {
            LOGGER.error("请求通信[" + reqURL + "]时异常,堆栈轨迹如下", e);
        } finally {
            // 关闭连接,释放资源
            httpClient.getConnectionManager().shutdown();
        }

        return respContent;

    }

    /**
     *@Author yong.shi.nj
     *@Description 获取httpclient
     *@Date 17:05 2018/11/30
     *@Param []
     *@return org.apache.http.client.HttpClient
     **/

    private static HttpClient getClient(){
        HttpClient httpClient = new DefaultHttpClient();

        // 设置代理服务器
        httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECT_TIMEOUT);
        httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, SOCKET_TIMEOUT);
        return httpClient;
    }

    /**
     *@Author yong.shi.nj
     *@Description 处理http返回信息
     *@Date 17:16 2018/11/30
     *@Param [response]
     *@return java.lang.String
     **/
    private static String getHttpResponseResult(HttpResponse response) throws IOException {

        String respContent = "通信失败";
        HttpEntity entity = response.getEntity();

        if (null != entity) {
            respContent = EntityUtils.toString(entity, ENCODING);
            EntityUtils.consume(entity);
        }

        StringBuilder respHeaderDatas = new StringBuilder();
        for (Header header : response.getAllHeaders()) {
            respHeaderDatas.append(header.toString()).append("\r\n");
        }

        // HTTP应答状态行信息
        String respStatusLine = response.getStatusLine().toString();
        // HTTP应答报文头信息
        String respHeaderMsg = respHeaderDatas.toString().trim();
        // HTTP应答报文体信息
        String respBodyMsg = respContent;

       LOGGER.info("HTTP应答完整报文\n" + respStatusLine + "\r\n" + respHeaderMsg + "\r\n" + respBodyMsg + "");
        return respContent;
    }

    public static String sendPostSSLRequest(String reqURL, Map<String, String> params, String encodeCharset) {

        String responseContent = "通信失败";

        //获取http连接
        HttpClient httpClient = getClient();

        // 创建TrustManager()，用于解决javax.net.ssl.SSLPeerUnverifiedException: peer not authenticated
        X509TrustManager trustManager = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            }
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        // 创建HostnameVerifier，用于解决javax.net.ssl.SSLException: hostname in certificate didn't match:
        // <123.125.97.66> != <123.125.97.241>

        X509HostnameVerifier hostnameVerifier = new X509HostnameVerifier() {
            public void verify(String host, SSLSocket ssl) throws IOException {
            }
            public void verify(String host, X509Certificate cert) throws SSLException {
            }
            public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
            }
            public boolean verify(String arg0, SSLSession arg1) {
                return true;
            }
        };

        try {
            // TLS1.0与SSL3.0基本上没有太大的差别,可粗略理解为TLS是SSL的继承者，但它们使用的是相同的SSLContext
            SSLContext sslContext = SSLContext.getInstance(SSLSocketFactory.TLS);
            // 使用TrustManager来初始化该上下文,TrustManager只是被SSL的Socket所使用
            sslContext.init(null, new TrustManager[] { trustManager }, null);
            // 创建SSLSocketFactory
            SSLSocketFactory socketFactory = new SSLSocketFactory(sslContext, hostnameVerifier);
            // 通过SchemeRegistry将SSLSocketFactory注册到HttpClient上
            httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", 443, socketFactory));
            // 创建HttpPost
            HttpPost httpPost = new HttpPost(reqURL);

            // 由于下面使用的是new
            // UrlEncodedFormEntity(....),所以这里不需要手工指定CONTENT_TYPE为application/x-www-form-urlencoded

            // 因为在查看了HttpClient的源码后发现,UrlEncodedFormEntity所采用的默认CONTENT_TYPE就是application/x-www-form-urlencoded

            // httpPost.setHeader(HTTP.CONTENT_TYPE,
            // "application/x-www-form-urlencoded; charset=" + encodeCharset);

            // 构建POST请求的表单参数
            if (null != params) {
                List<NameValuePair> formParams = new ArrayList<NameValuePair>();
                for (Map.Entry<String, String> entry : params.entrySet()) {

                    formParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }
                httpPost.setEntity(new UrlEncodedFormEntity(formParams, ENCODING));
            }

            HttpResponse response = httpClient.execute(httpPost);

            HttpEntity entity = response.getEntity();
            responseContent = getHttpResponseResult(response);
        } catch (Exception e) {
            LOGGER.error("请求通信[" + reqURL + "]时异常,堆栈轨迹如下", e);
        } finally {
            // 关闭连接,释放资源
            httpClient.getConnectionManager().shutdown();
        }

        return responseContent;

    }

}
