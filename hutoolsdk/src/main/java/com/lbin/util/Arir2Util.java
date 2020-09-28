package com.lbin.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;

import java.util.Map;

public class Arir2Util {

    /**
     *
     * @param aria2
     * @param url
     * @param filename
     * @param dir
     * @return
     */
    public static String aria2Download(String aria2,String url, String filename, String dir) {
        if (StrUtil.containsAny(filename, "/")) {
            filename = StrUtil.replace(filename, "/", "_");
        }
        String json = "{\"jsonrpc\":\"2.0\",\"method\":\"aria2.addUri\",\"id\":\"aria2Download\",\"params\":[[\"" + url + "\"],{\"out\":\"" + filename + "\" ,\"dir\":\"" + dir + "\"}]}";
        return arir2Post(aria2,json);
    }

    /**
     *
     * @param aria2
     * @param url
     * @param dir
     * @return
     */
    public static String aria2Download(String aria2,String url, String dir) {
        String json = "{\"jsonrpc\":\"2.0\",\"method\":\"aria2.addUri\",\"id\":\"aria2Download\",\"params\":[[\"" + url + "\"],{\"dir\":\"" +  dir + "\"}]}";
        return arir2Post(aria2,json);
    }

    /**
     *
     * @param aria2
     * @param url
     * @return
     */
    public static String aria2Download(String aria2,String url) {
        String json = "{\"jsonrpc\":\"2.0\",\"method\":\"aria2.addUri\",\"id\":\"aria2Download\",\"params\":[[\"" + url + "\"],{}]}";
        return arir2Post(aria2,json);
    }

    //删除下载记录
    public static String aria2RemoveDownloadResult(String aria2,String url) {
        String json = "{\"jsonrpc\":\"2.0\",\"method\":\"aria2.removeDownloadResult\",\"id\":\"aria2RemoveDownloadResult\",\"params\":[\"" + url + "\"]}";
        return arir2Post(aria2,json);
    }

    //状态
    public static String aria2Status(String aria2, String url) {
        String json = "{\"jsonrpc\":\"2.0\",\"method\":\"aria2.tellStatus\",\"id\":\"aria2Status\",\"params\":[\"" + url + "\",[\"gid\",\"totalLength\",\"completedLength\",\"status\",\"files\"]]}";
        return arir2Post(aria2, json);
    }

    //查询停止下载列表
    public static String aria2Stop(String aria2) {
        String json = "{\"jsonrpc\":\"2.0\",\"method\":\"aria2.tellStopped\",\"id\":\"aria2Stop\",\"params\":[0,100,[\"gid\",\"totalLength\",\"completedLength\",\"status\",\"files\"]]}";
        return arir2Post(aria2, json);
    }

    //查询等待下载列表
    public static String aria2Waiting(String aria2) {
        String json = "{\"jsonrpc\":\"2.0\",\"method\":\"aria2.tellWaiting\",\"id\":\"aria2Waiting\",\"params\":[0,100,[\"gid\",\"totalLength\",\"completedLength\",\"status\",\"files\"]]}";
        return arir2Post(aria2, json);
    }

    //查询活动下载列表
    public static String aria2Active(String aria2) {
        String json = "{\"jsonrpc\":\"2.0\",\"method\":\"aria2.tellActive\",\"id\":\"aria2Active\",\"params\":[[\"gid\",\"totalLength\",\"completedLength\",\"status\",\"files\"]]}";
        return arir2Post(aria2, json);
    }

    /**
     * Post请求主要
     * @param aria2
     * @param json
     * @return
     */
    private static String arir2Post(String aria2, String json) {
        String result2 = HttpUtil.post(aria2 + "/jsonrpc", json);
        return result2;
    }

    /**
     * json转换
     *
     * @param str
     * @return
     */
    public static Map<String, Object> toJson(String str) {
        Map<String, Object> jsonObject = JSONUtil.parseObj(str);
        jsonObject.put("data", jsonObject.get("result"));
        return jsonObject;
    }

}
