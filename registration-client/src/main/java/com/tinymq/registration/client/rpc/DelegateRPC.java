package com.tinymq.registration.client.rpc;

import com.tinymq.registration.client.dto.SendRequest;
import com.tinymq.registration.client.dto.SendResult;
import com.tinymq.registration.client.exception.SendException;

public interface DelegateRPC {

    SendResult send(SendRequest req, long timeoutMillis) throws SendException;

}
