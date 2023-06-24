package com.tinymq;

import com.tinymq.core.config.RegistrationConfig;
import com.tinymq.core.exception.AppendLogException;
import com.tinymq.core.store.DefaultCommitLogService;
import com.tinymq.core.store.CommitLogEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.charset.StandardCharsets;

@RunWith(JUnit4.class)
public class CommitLogTest {
    private DefaultCommitLogService defaultCommitLogService;

    @Before
    public void init() {
        RegistrationConfig registrationConfig = new RegistrationConfig("registraion.yaml");
        this.defaultCommitLogService = new DefaultCommitLogService(registrationConfig);
        this.defaultCommitLogService.start();
    }

    @Test
    public void testAppend() throws AppendLogException {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
        CommitLogEntry commitLogEntry = CommitLogEntry.createUnCommited(1, bytes);
        this.defaultCommitLogService.appendLog(commitLogEntry);
        this.defaultCommitLogService.appendLog(commitLogEntry);
        this.defaultCommitLogService.appendLog(commitLogEntry);

        commitLogEntry = this.defaultCommitLogService.getByOffset(0);
        System.out.println(commitLogEntry.getCreatedTerm());
        System.out.println(new String(commitLogEntry.getBody()));
    }
}
