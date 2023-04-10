package com.tinymq.remote.protocol;

import cn.hutool.json.JSONUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class JSONSerializer {
    private JSONSerializer() { }

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    public static byte[] encode(Object o) {
        String jsonStr = JSONUtil.toJsonStr(o);
        if(jsonStr != null) {
            return jsonStr.getBytes(CHARSET);
        }
        return null;
    }
    public static <T> T decode(final byte[] bytes, Class<T> tClass) {
        String jsonStr = new String(bytes, CHARSET);
        return fromJsonStr(jsonStr, tClass);
    }

    public static String toJson(Object o) {
        return JSONUtil.toJsonStr(o);
    }
    public static <T> T fromJsonStr(final String jsonStr, Class<T> tClass) {
        return JSONUtil.toBean(jsonStr, tClass);
    }
}
