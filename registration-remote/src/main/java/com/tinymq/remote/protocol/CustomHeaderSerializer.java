package com.tinymq.remote.protocol;

import cn.hutool.core.util.StrUtil;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CustomHeaderSerializer {
    private CustomHeaderSerializer() { }
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;


    public static byte[] encodeHeader(RemotingCommand msg) {
        // 计算Header 大小
        int headerLen = 0;
        byte[] extBytes = null;
        if(msg.getExtFields() != null && !msg.getExtFields().isEmpty()) {
            extBytes = encodeExtFields(msg.getExtFields());
        }

        int infoLen = 0;
        final boolean useShort = true;
        if(!StrUtil.isEmpty(msg.getInfo())) {
            infoLen += msg.getInfo().getBytes(DEFAULT_CHARSET).length;
        }
        infoLen += useShort ? 2 : 4;            //长度字段
        headerLen = calTotalLen(infoLen, extBytes == null ? 0 : extBytes.length);
        ByteBuffer buf = ByteBuffer.allocate(headerLen);

        // header 长度
        buf.putInt(msg.getVersion());
        buf.putInt(msg.getFlag());
        buf.putInt(msg.getRequestId());
        buf.putInt(msg.getCode());
        if(!StrUtil.isEmpty(msg.getInfo())) {
            buf.put(writeStr(msg.getInfo(), useShort));
        }
        if(extBytes != null) {
            buf.put(extBytes);
        }

        return buf.array();
    }


    public static RemotingCommand decodeCustomHeader(final byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        int version = buf.getInt();
        int flag = buf.getInt();
        int requestId = buf.getInt();
        int code = buf.getInt();
        String info = readStr(buf, true);

        RemotingCommand res = RemotingCommand.createRequest(code);
        res.setVersion(version);
        res.setFlag(flag);
        res.setRequestId(requestId);
        res.setInfo(info);

        if(buf.hasRemaining()) {
            byte[] extBytes = new byte[buf.limit() - buf.position()];
            buf.get(extBytes);
            res.setExtFields(decodeExtField(extBytes));
        }
        return res;
    }




    private static int calTotalLen(int infoLen, int extLen) {
        return 4 // version
                + 4 // flag
                + 4 // requestId
                + 4 // code
                + infoLen // 响应消息
                + extLen;
    }
    public static byte[] encodeExtFields(Map<String, String> extFields) {
        return mapSerailize(extFields);
    }

    public static byte[] writeStr(final String str, boolean useShort) {
        if(str == null) return null;
        int strLen = 0;
        byte[] strBytes = str.getBytes(DEFAULT_CHARSET);

        strLen += strBytes.length;
        ByteBuffer buf = ByteBuffer.allocate(useShort ? strLen + 2 : strLen + 4);
        if(useShort) {
            buf.putShort((short) strLen);
        } else {
            buf.putInt(strLen);
        }
        buf.put(strBytes);
        return buf.array();
    }
    public static String readStr(ByteBuffer buf, boolean useShort) {
        int sLen = 0;
        if(useShort) {
            sLen = buf.getShort();
        } else {
            sLen = buf.getInt();
        }

        byte[] bytes = new byte[sLen];
        buf.get(bytes);
        return new String(bytes, DEFAULT_CHARSET);
    }
    public static byte[] mapSerailize(final Map<String, String> extFields) {
        // keylength: short
        // valLength: int
        if (extFields == null || extFields.isEmpty()) {
            return null;
        }
        int totalLen = 0;
        for (Map.Entry<String, String> entry : extFields.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (k != null && v != null) {
                totalLen = totalLen +
                        2 + k.getBytes(DEFAULT_CHARSET).length +
                        4 + v.getBytes(DEFAULT_CHARSET).length;
            }
        }

        ByteBuffer buffer = ByteBuffer.allocate(totalLen);
        for (Map.Entry<String, String> entry : extFields.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (k != null && v != null) {
                byte[] kBytes = k.getBytes(DEFAULT_CHARSET);
                byte[] vBytes = v.getBytes(DEFAULT_CHARSET);
                buffer.putShort((short) kBytes.length);
                buffer.put(kBytes);
                buffer.putInt(vBytes.length);
                buffer.put(vBytes);
            }
        }
        return buffer.array();
    }

    public static Map<String, String> decodeExtField(final byte[] extBytes) {
        // keylength: short
        // valLength: int
        // {<kLen+key+valLen+val>,<...>}
        Map<String, String> ext = new HashMap<>();
        ByteBuffer wrappedBuffer = ByteBuffer.wrap(extBytes);
        while (wrappedBuffer.hasRemaining()) {
            short keyLen = wrappedBuffer.getShort();
            byte[] keyBytes = new byte[keyLen];
            wrappedBuffer.get(keyBytes, 0, keyLen);

            int valLen = wrappedBuffer.getInt();
            byte[] valBytes = new byte[keyLen];
            wrappedBuffer.get(valBytes, 0, valLen);

            String k = new String(keyBytes, DEFAULT_CHARSET);
            String v = new String(valBytes, DEFAULT_CHARSET);
            ext.put(k, v);
        }
        return ext;
    }
}
