package com.billl99.network;

/**
 * <p>[描述信息：说明类的基本功能]</p>
 *
 * @author 丁建新 - jianxin.ding@99bill.com
 * @version 1.0  Created on 2018/11/20 15:51
 */
public class HttpTimeoutDto {

    private int socketTimeoutMills;

    private int connectTimeoutMills;

    public HttpTimeoutDto(int socketTimeoutMills, int connectTimeoutMills) {
        this.socketTimeoutMills = socketTimeoutMills;
        this.connectTimeoutMills = connectTimeoutMills;
    }

    public int getSocketTimeoutMills() {
        return socketTimeoutMills;
    }

    public void setSocketTimeoutMills(int socketTimeoutMills) {
        this.socketTimeoutMills = socketTimeoutMills;
    }

    public int getConnectTimeoutMills() {
        return connectTimeoutMills;
    }

    public void setConnectTimeoutMills(int connectTimeoutMills) {
        this.connectTimeoutMills = connectTimeoutMills;
    }
}
