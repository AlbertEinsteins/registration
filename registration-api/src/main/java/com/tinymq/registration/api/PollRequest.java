package com.tinymq.registration.api;

import com.tinymq.core.dto.outer.StateModel;

public interface PollRequest {
    PollResult getByKey(StateModel key);
}
