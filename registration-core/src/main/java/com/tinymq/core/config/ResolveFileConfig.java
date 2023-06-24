package com.tinymq.core.config;

import cn.hutool.setting.yaml.YamlUtil;
import io.netty.util.internal.StringUtil;

import java.io.File;
import java.util.Map;

public class ResolveFileConfig {
    private Map configMap;

    public ResolveFileConfig(String filePath) {
        resolveFile(filePath);
    }

    public void resolveFile(String filePath) {
        this.configMap = YamlUtil.loadByPath(filePath, Map.class);
    }

    public Object fromKey(String key) {
        if(StringUtil.isNullOrEmpty(key)) {
            return null;
        }
        final String[] split = key.split("\\.");
        Map levelMap = configMap;
        for(int i = 0; i < split.length - 1; i++) {
            levelMap = (Map) configMap.get(split[i]);
            if(levelMap == null) {
                return null;
            }
        }
        return levelMap.get(split[split.length - 1]);
    }
}
