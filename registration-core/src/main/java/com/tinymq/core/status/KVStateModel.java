package com.tinymq.core.status;

import com.tinymq.remote.protocol.JSONSerializer;

import java.io.Serializable;

/**
 * except the interface,
 *  the sub-class must implmented the get/set method
 */
public interface KVStateModel extends Codec<KVStateModel> {

    void setKey(String key);

    void setVal(String val);

    String getKey();

    String getVal();

    // how to decode self.
    KVStateModel decode(final byte[] bytes);

    byte[] encode(final KVStateModel kvStateModel);
}
