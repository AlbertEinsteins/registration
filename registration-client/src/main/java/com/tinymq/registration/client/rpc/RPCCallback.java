package com.tinymq.registration.client.rpc;

public interface RPCCallback<Resp> {
    void doOperate(final Resp response);
}
