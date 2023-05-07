package com.tinymq.core;


import com.tinymq.core.dto.AppendEntriesRequest;
import com.tinymq.core.dto.AppendEntriesResponse;
import com.tinymq.core.dto.VoteRequest;
import com.tinymq.core.dto.VoteResponse;
import com.tinymq.core.exception.RegistrationVoteException;
import com.tinymq.core.exception.ReplicatedLogException;

public interface ConsensusService {

    /**
     * 投票
     */
    VoteResponse invokeVote(final String addr, final VoteRequest voteRequest, final long timeoutMillis)
            throws RegistrationVoteException;


    /**
     * AppendEntriesRequest for heart beat or replicate log
     */
    AppendEntriesResponse replicateLog(final String addr, final AppendEntriesRequest appendEntriesRequest,
                                       long timeoutMillis, boolean isHearbeat)
        throws ReplicatedLogException;
}
