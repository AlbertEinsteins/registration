package com.tinymq.core;

import com.tinymq.core.dto.AppendEntriesRequest;
import com.tinymq.core.dto.AppendEntriesResponse;
import com.tinymq.core.dto.VoteRequest;
import com.tinymq.core.dto.VoteResposne;
import com.tinymq.core.exception.RegistrationVoteException;

public interface ConsensusService {

    /**
     * 投票
     */
    VoteResposne invokeVote(final String addr, final VoteRequest voteRequest, final long timeoutMillis)
            throws RegistrationVoteException;

    /**
     * 同步数据
     */
    AppendEntriesResponse copyCommitLog(final String addr, final AppendEntriesRequest appendEntriesRequest);
}
