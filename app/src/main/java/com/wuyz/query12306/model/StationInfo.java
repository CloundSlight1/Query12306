package com.wuyz.query12306.model;

/**
 * Created by wuyz on 2016/9/23.
 *
 */

public class StationInfo {
    public String name;
    public String code;
    public String pingYin;

    public StationInfo(String name, String code, String pingYin) {
        this.name = name;
        this.code = code;
        this.pingYin = pingYin;
    }

    @Override
    public String toString() {
        return name;
    }
}
