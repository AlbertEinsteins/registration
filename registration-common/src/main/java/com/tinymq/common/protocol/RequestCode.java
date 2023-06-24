package com.tinymq.common.protocol;

import com.tinymq.common.utils.ServiceThread;

public class RequestCode {

    //========== 集群内部通信
    public static final int APPENDENTRIES_EMPTY = 10;
    public static final int REIGISTRATION_REQUESTVOTE = 11;
    public static final int APPENDENTRIES = 12;

    //========== 集群与外部交互
    public static final int REGISTRATION_CLIENT_READ = 13;
    public static final int REGISTRATION_CLIENT_WRITE = 14;

}
