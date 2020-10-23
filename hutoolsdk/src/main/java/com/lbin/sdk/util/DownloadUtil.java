package com.lbin.sdk.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 通用多线程下载工具
 * 依赖 hutool core 和 http 模块
 * 依赖 ftpUtil
 */
public class DownloadUtil {

    private static final Integer count = 5;
    private static List<String> proxyList = new ArrayList<>();

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36";

    private static ThreadPoolExecutor threadPoolExecutor = ThreadUtil.newExecutor(8, 16);

    {
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        proxyList.add("https://search.pstatic.net/common?src=");
        proxyList.add("https://imageproxy.pimg.tw/resize?url=");
        proxyList.add("https://images.weserv.nl/?url=");
        proxyList.add("https://pic1.xuehuaimg.com/proxy/");
    }

    /**
     * 如果给定字符串不是以prefix开头的，在开头补充 prefix
     *
     * @param path
     * @param prefix
     * @return
     */
    public static String AddPrefix(String path, String prefix) {
        path = StrUtil.addPrefixIfNot(path, "/" + prefix + "/");
        return path;
    }

    /**
     * 去掉指定前缀
     *
     * @param path
     * @param prefix
     * @return
     */
    public static String DelPrefix(String path, String prefix) {
        path = StrUtil.removePrefix(path, prefix);
        path = StrUtil.removePrefix(path, "/" + prefix + "/");
        return path;
    }

    public static List<String> dowlownThreadString(List<Map<String, Object>> downloadItemList) {
        List<Map<String, Object>> list = dowlownThread(downloadItemList);
        List<String> stringList = new ArrayList<>();
        for (Map<String, Object> downloadItem : list) {
            stringList.add((String) downloadItem.get("local"));
        }
        return stringList;
    }

    public static List<Map<String, Object>> downloadTempImgThread(List<Map<String, Object>> downloadItemList) {
        for (int i = 0; i < downloadItemList.size(); i++) {
            Map<String, Object> downloadItem = downloadItemList.get(i);
            downloadTempImgThread(downloadItem);
        }
        return downloadItemList;
    }

    public static List<Map<String, Object>> dowlownThread(List<Map<String, Object>> downloadItemList) {
        List<Map<String, Object>> list = new ArrayList<>();
        ThreadPoolExecutor threadPoolExecutor = ThreadUtil.newExecutor(8, 16);
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        for (int i = 0; i < downloadItemList.size(); i++) {
            Map<String, Object> downloadItem = downloadItemList.get(i);
            downloadItem.put("index", i);
            try {
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        Map<String, Object> downloadItemThread = downloadItem;
                        try {
                            String download = download(downloadItemThread);
                            downloadItemThread.put("local", download);
                            downloadItem.put("local", download);
                        } catch (Exception e) {
                            System.err.println(downloadItemThread);
                            e.printStackTrace();
                        } finally {
                            try {
                                list.add(downloadItemThread);
                            } catch (Exception e) {
                                e.printStackTrace();
                                list.add(downloadItemThread);
                            }
                        }
                    }
                });
            } catch (RejectedExecutionException e) {
                ThreadUtil.sleep(5000);
            }
        }
        int size = 0;
        while (list.size() < downloadItemList.size()) {
            ThreadUtil.sleep(5000);
            if (size == list.size()) {
                ThreadUtil.sleep(30000);
                if (size == list.size()) {
                    break;
                }
            } else {
                size = list.size();
            }
        }
        return downloadItemList;
    }

    public static Map<String, Object> getDownloadItemTemp(String path, String fileName, String url, Map<String, String> header) {
        Map<String, Object> downloadItem = getDownloadItem(path, fileName, url, header);
        return getDownloadTempMap(downloadItem);
    }

    public static Map<String, Object> getDownloadItem(String path, String fileName, String url, Map<String, String> header) {
        Map<String, Object> downloadItem = new HashMap<>();
        downloadItem.put("path", path);
        downloadItem.put("fileName", fileName);
        downloadItem.put("url", url);
        downloadItem.put("header", header);
        return downloadItem;
    }

    /**
     * @param path
     * @param fileName
     * @param url
     * @param header
     * @return
     */

    public static String download(String path, String fileName, String url, Map<String, String> header) {
        return download(getDownloadItem(path, fileName, url, header));
    }

    /**
     * 下载（主）
     *
     * @return
     */
    public static String download(Map<String, Object> downloadItem) {
        String path = (String) downloadItem.get("path");
        String fileName = (String) downloadItem.get("fileName");
        String url = (String) downloadItem.get("url");
        Map<String, String> header = (Map<String, String>) downloadItem.get("header");
        try {
            fileName = FileUtil.getName(URLUtil.getPath(fileName));
        } catch (Exception e) {
            if (StrUtil.containsAny(FileUtil.extName(fileName), "?")) {
                String extName = FileUtil.extName(fileName);
                extName = extName.substring(0, extName.indexOf("?") + 1);
                fileName = FileUtil.mainName(fileName) + "." + extName;
            }
        }
        boolean upload = false;
        if (exist(path + fileName)) {
            return getDownloadUrl(path, fileName);
        }
        if (!StrUtil.containsAny(url, "http")) {
            upload = copy(url, path, fileName);
        } else {
            for (int i = 0; i < count; i++) {
                upload = baseDownload(path, fileName, downloadInputStream(url, header));
                if (upload) {
                    break;
                }
            }
            if (!upload) {
                String ext = FileUtil.extName(url);
                String replace = "jpg";
                if (StrUtil.containsAny(url, "jpg")) {
                    ext = "jpg";
                    replace = "png";
                } else if (StrUtil.containsAny(url, "png")) {
                    ext = "png";
                    replace = "jpg";
                }
                String extUrl = StrUtil.replace(url, ext, replace);
                String extName = StrUtil.replace(fileName, ext, replace);
                delete(path + extName);
                upload = baseDownload(path, extName, downloadInputStream(extUrl, header));
                if (upload) {
                    delete(path + fileName);
                    fileName = extName;
                } else {
                    delete(path + extName);
                    upload = proxy(path, fileName, url, header);
                }
            }
        }
        String download = null;
        if (upload && exist(path + fileName)) {
            download = getDownloadUrl(path, fileName);
        } else {
            System.err.println("下载失败：" + url + "-" + path + fileName);
            delete(path + fileName);
        }
        return download;
    }

    /**
     * 代理
     *
     * @param path
     * @param fileName
     * @param url
     * @param header
     * @return
     */
    private static boolean proxy(String path, String fileName, String url, Map<String, String> header) {
        boolean upload = false;
        if (StrUtil.count(url, "http") > 1) {
            url = "http" + StrUtil.subAfter(url, "http", true);
        }
        for (String proxy : proxyList) {
            upload = baseDownload(path, fileName, downloadInputStream(proxy + url, header));
            if (upload) {
                break;
            }
        }
        return upload;
    }

    /**
     * 下载（次）
     *
     * @param path
     * @param fileName
     * @param fileStream
     * @return
     */
    public static String download(String path, String fileName, InputStream fileStream) {
        fileName = FileUtil.getName(URLUtil.getPath(fileName));
        boolean upload = false;
        for (int i = 0; i < count; i++) {
            upload = baseDownload(path, fileName, fileStream);
            if (upload) {
                break;
            }
        }
        String download = null;
        if (upload) {
            download = getDownloadUrl(path, fileName);
        } else {
            FileUtil.del(getDownloadUrl(path, fileName));
        }
        return download;
    }

    /**
     * 检测文件存在并返回
     *
     * @param downloadItem
     * @return
     */
    public static String existGet(Map<String, Object> downloadItem) {
        String path = (String) downloadItem.get("path");
        String fileName = (String) downloadItem.get("fileName");
        String url = (String) downloadItem.get("url");
        if (exist(path + fileName)) {
            url = getDownloadUrl(downloadItem);
        }
        return url;
    }

    /**
     * 检测文件存在
     *
     * @param downloadItem
     * @return
     */
    public static boolean exist(Map<String, Object> downloadItem) {
        String path = (String) downloadItem.get("path");
        String fileName = (String) downloadItem.get("fileName");
        return exist(path + fileName);
    }

    /**
     * 检测文件存在
     *
     * @param path
     * @return
     */
    public static boolean exist(String path) {
        boolean e = false;
        if (path == null || StrUtil.containsAny(path, "http")) {
            return e;
        }
        path = getPath(path);
        e = FileUtil.exist(path);
        if (e) {
            e = !FileUtil.isEmpty(FileUtil.file(path));
        }
        return e;
    }

    /**
     * 获取真实路径
     *
     * @param path
     * @return
     */
    public static String getPath(String path) {
        return path;
    }

    /**
     * 删除文件
     *
     * @param path
     * @return
     */
    public static boolean delete(String path) {
        path = getPath(path);
        FileUtil.del(path);
        return false;
    }

    /**
     * 通用缓存
     *
     * @return
     */
    public static String downloadTempImgThread(Map<String, Object> downloadItem) {
        Map<String, Object> downloadTempMap = getDownloadTempMap(downloadItem);
        String path = (String) downloadTempMap.get("path");
        String fileName = (String) downloadTempMap.get("fileName");
        String url = (String) downloadTempMap.get("url");
        if (exist(path + fileName)) {
            return getDownloadUrl(path, fileName);
        } else {
            threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    Map<String, Object> downloadItemThread = downloadTempMap;
                    try {
                        String download = baseDownloadTempImg(downloadItemThread);
                    } catch (Exception e) {
                        System.err.println(downloadItemThread);
                        e.printStackTrace();
                    }
                }
            });
            return url;
        }
    }


    /**
     * 通用缓存
     *
     * @return
     */
    public static String downloadTempImg(Map<String, Object> downloadItem) {
        downloadItem = getDownloadTempMap(downloadItem);
        return baseDownloadTempImg(downloadItem);
    }

    /**
     * 通用缓存（base）
     *
     * @return
     */
    public static String baseDownloadTempImg(Map<String, Object> downloadItem) {
        String path = (String) downloadItem.get("path");
        String fileName = (String) downloadItem.get("fileName");
        String url = (String) downloadItem.get("url");
        boolean exist = exist(path + fileName);
        String img = "";
        if (exist) {
            img = getDownloadUrl(path, fileName);
        } else {
            img = download(downloadItem);
            if (img == null) {
                img = url;
            }
        }
        return img;
    }

    /**
     * 通用缓存Map处理
     *
     * @param downloadItem
     * @return
     */

    public static Map<String, Object> getDownloadTempMap(Map<String, Object> downloadItem) {
        String pathchild = "default";
        if (downloadItem.get("path") != null) {
            pathchild = (String) downloadItem.get("path");
        }
        String path = getTempPath(pathchild);
        String url = (String) downloadItem.get("url");
        downloadItem.put("path", path);
        String fileName = (String) downloadItem.get("fileName");
        if (fileName == null) {
            List<String> list = StrUtil.splitTrim(FileUtil.getName(url), "?");
            if (list.size() > 0) {
                fileName = list.get(0);
            } else {
                fileName = FileUtil.getName(url);
            }
        }
        downloadItem.put("fileName", fileName);
        return downloadItem;
    }

    public static String getTempPath(String path) {
        if (StrUtil.containsAny(path, "img/temp/")) {
            return path;
        }
        return "img/temp/" + path + "/";
    }

    /**
     * 本地文件复制
     *
     * @param local
     * @param path
     * @param filename
     * @return
     */
    public static boolean copylocal(String local, String path, String filename) {
        String src = local;
        if (exist(path + filename)) {
            return true;
        }
        try {
            FileUtil.copy(src, getPath(path + filename), true);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 缓存文件复制
     *
     * @param url
     * @param path
     * @param filename
     * @return
     */
    public static boolean copy(String url, String path, String filename) {
        String src = getPath(url);
        return copylocal(src, path, filename);
    }


    /**
     * 下载（流）
     *
     * @param path
     * @param fileName
     * @param fileStream
     * @return
     */
    private static boolean baseDownload(String path, String fileName, InputStream fileStream) {
        path = StrUtil.removePrefix(path, "/");
        path = StrUtil.addSuffixIfNot(path, "/");
        boolean bool = false;
        bool = downloadIO(getPath(path), fileName, fileStream);
        return bool;
    }

    /**
     * 获取FileUrl
     *
     * @param path
     * @param fileName
     * @return
     */
    public static String getFileUrl(String path, String fileName) {
        return path + fileName;
    }


    /**
     * 获得访问URL
     *
     * @param downloadItem
     * @return
     */
    public static String getDownloadUrl(Map<String, Object> downloadItem) {
        String path = (String) downloadItem.get("path");
        String fileName = (String) downloadItem.get("fileName");
        return getDownloadUrl(path, fileName);
    }

    /**
     * 获得访问URL
     *
     * @param path
     * @param fileName
     * @return
     */
    public static String getDownloadUrl(String path, String fileName) {
        return path + fileName;
    }


    /**
     * 地址转换流
     *
     * @param url
     * @param header
     * @return
     * @throws IOException
     */
    public static InputStream downloadInputStream(String url, Map<String, String> header) {
        InputStream inputStream = null;
        URLConnection urlConnection = null;
        try {
            urlConnection = URLUtil.url(url).openConnection();
            if (header == null || header.size() == 0) {
                urlConnection.setRequestProperty("User-Agent", USER_AGENT);
            } else {
                for (Map.Entry<String, String> entry : header.entrySet()) {
                    urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            inputStream = urlConnection.getInputStream();
        } catch (Exception e) {
//            System.err.println("URL TO IO失败:" + url);
        }
        return inputStream;
    }


    /**
     * 工具下载(可靠性不高，但可以不获取真实地址)
     *
     * @param path
     * @param fileName
     * @param fileStream
     * @return
     */
    private static boolean downloadIO(String path, String fileName, InputStream fileStream) {
        String s = StrUtil.addSuffixIfNot(path, "/");
        File file = FileUtil.touch(s + fileName);
        return downloadIO(file, fileStream);
    }

    /**
     * 工具下载(可靠性不高，但可以不获取真实地址)
     *
     * @param file
     * @param fileStream
     * @return
     */
    private static boolean downloadIO(File file, InputStream fileStream) {
        boolean upload = false;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = fileStream;
            outputStream = FileUtil.getOutputStream(file);
            upload = downloadIO(outputStream, inputStream);
            outputStream.flush();
            if (upload) {
                if (file.exists()) {
                    upload = file.length() > 0;
                } else {
                    upload = false;
                }
            }
        } catch (Exception e) {
            upload = false;
//            System.err.println("IO下载失败：" + path + fileName + "-----API");
        } finally {
            IoUtil.close(inputStream);
            IoUtil.close(outputStream);
        }
        return upload;
    }

    /**
     * 工具下载(可靠性不高，但可以不获取真实地址)
     *
     * @param outfileStream
     * @param fileStream
     * @return
     * @throws IORuntimeException
     */
    private static boolean downloadIO(OutputStream outfileStream, InputStream fileStream) throws IORuntimeException {
        boolean upload = false;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        inputStream = fileStream;
        outputStream = outfileStream;
        long io = IoUtil.copyByNIO(inputStream, outputStream, IoUtil.DEFAULT_BUFFER_SIZE, null);
        upload = io > 128;
        return upload;
    }

}
