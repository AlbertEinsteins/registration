package com.tinymq;

import com.tinymq.core.status.DefaultKVStateModel;
import com.tinymq.core.status.KVStateModel;
import org.junit.Test;

public class StateTest {
    @Test
    public void test() {
        KVStateModel kvStateModel = new DefaultKVStateModel();
        KVStateModel template = new DefaultKVStateModel();

        kvStateModel.setKey("123");
        kvStateModel.setVal("xxx");

        byte[] encoded = kvStateModel.encode(kvStateModel);
        KVStateModel stateModel = template.decode(encoded);
        System.out.println(stateModel);
    }

}
