package com.lbin.sdk.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import java.io.File;
import java.util.List;

public class remore {

    private final static String[] re={"#",
            " ",
            "+",
            "-",
            "_͜ʖ",
            "(_͡°_͜ʖ_͡°)",
            "(_͡°_͜ʖ_͡°)",
            "͡°",
            "'",
            "/",
            "@",
            "×",
            "・",
            "～",
            "＆",
            "！",
            "？",
            "[",
            "]",
            "－",
            "!"
    };


    public static void main(String[] args) {
        String path = "W:\\volume\\Pictures\\galleries\\cg\\cg";
        ls(path);
        rename(path);

    }

    private static void ls(String path) {
        File[] files = FileUtil.ls(path);
        for (File file : files) {
            String name = file.getName();
            if ( FileUtil.isDirectory(file)) {
                ls(file.getAbsolutePath());
                name=replace(file,name);
//                System.out.println(file.getAbsolutePath());


            }
        }
    }

    private static void rename(String path) {
        List<File> files = FileUtil.loopFiles(path);
        for (File file : files) {
            String name = file.getName();
            name=replace(file,name);
        }
    }

    private static String replace(File file, String name) {
        if (StrUtil.containsAnyIgnoreCase(name, re)) {
            name = replace(name);
            FileUtil.rename(file, name, false, true);
        }
        return name;
    }

    private static String replace(String name) {
        for (String s : re) {
            name = StrUtil.replace(name, s, "_");
        }
        System.out.println(name);
        return name;
    }
}
