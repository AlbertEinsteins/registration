package com.tinymq.common.protocol;

import com.tinymq.common.utils.ServiceThread;

public final class RequestCode {
    private RequestCode() {}

    // 举例子，当前code不用 <code, requestProcessor>
    public static final int BEGIN_START = 0;

    public static final int APPENDENTRIES_EMPTY = 10;
    public static final int REIGISTRATION_REQUESTVOTE = 11;
    public static final int APPENDENTRIES = 12;


    public static final int REGISTRATION_CLIENT_READ = 13;
    public static final int REGISTRATION_CLIENT_WRITE = 14;

}
