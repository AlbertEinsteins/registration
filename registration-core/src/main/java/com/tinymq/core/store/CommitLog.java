package com.tinymq.core.store;

import com.tinymq.core.RegistrationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommitLog {
    private static final Logger LOG = LoggerFactory.getLogger(CommitLog.class);

    public static String savePath = RegistrationConfig.DEFAULT_COMMIT_LOG_FILEPATH;
    public static String saveFile = "store.log";



}
