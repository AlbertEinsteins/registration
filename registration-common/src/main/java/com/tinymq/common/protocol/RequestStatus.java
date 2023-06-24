package com.tinymq.common.protocol;

/**
 * 集群与外部交互在extFields的附加状态信息
 */
public enum RequestStatus {
    NOT_LEADER(0),

    READ_SUCCESS(1),

    WRITE_SUCCESS(2),

    EXCEPTION_OCCURRED(3);



    public final int code;
    RequestStatus(int code) {
        this.code = code;
    }

    public static RequestStatus fromCode(int code) {
        for(RequestStatus requestStatus: RequestStatus.values()) {
            if(requestStatus.code == code) {
                return requestStatus;
            }
        }
        return null;
    }
}
