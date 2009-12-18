package uk.co.pols.bamboo.gitplugin.client.commands;

import com.atlassian.bamboo.repository.RepositoryException;
import org.jmock.integration.junit3.MockObjectTestCase;
import org.jmock.Expectations;

import java.io.File;
import java.io.IOException;

public class ExecutorGitListRemoteCommandTest extends MockObjectTestCase {
    private static final String GIT_EXE = "git";

    private File tmpDir;
    private final CommandExecutor commandExecutor = mock(CommandExecutor.class);
    private static final String REPO = "repo";
    private static final String BRANCH = "branch";

    public void setUp() {
        tmpDir = new File(System.getProperty("java.io.tmpdir"));
    }

    public void testCorrectlyExtractsCommitHashFromResultWithTab() throws Exception {
        mockReturnValue("1cc17a8203f7c5c82e89ae5f687d12b7be65951e\trefs/heads/master");
        assertCommandReturns("1cc17a8203f7c5c82e89ae5f687d12b7be65951e");
    }

    public void testCorrectlyExtractsCommitHashFromResultWithADifferentSHA() throws Exception {
        mockReturnValue("ca23c1bc865fbd60b4d0c5dcdb264afd65789026\trefs/heads/master");
        assertCommandReturns("ca23c1bc865fbd60b4d0c5dcdb264afd65789026");
    }

    public void testCorrectlyExtractsCommitHashFromResultWithATrailingNewline() throws Exception {
        mockReturnValue("ca23c1bc865fbd60b4d0c5dcdb264afd65789026\trefs/heads/master\n");
        assertCommandReturns("ca23c1bc865fbd60b4d0c5dcdb264afd65789026");
    }

    public void testCorrectlyExtractsCommitHashFromResultWithSpaces() throws Exception {
        mockReturnValue("1cc17a8203f7c5c82e89ae5f687d12b7be65951e     refs/heads/master");
        assertCommandReturns("1cc17a8203f7c5c82e89ae5f687d12b7be65951e");
    }

    public void testThrowsAnIOExceptionWhenGitReturnsErrorMessage() throws Exception {
        mockReturnValue("fatal: Unable to look up github.com");
        try {
            new ExecutorGitListRemoteCommand(GIT_EXE, commandExecutor).getLastCommit(REPO, BRANCH);
            fail("Should throw IOException");
        } catch (IOException e) {
            assertEquals("Could not determine latest commit for branch 'branch' @ 'repo'. git-ls-remote: fatal: Unable to look up github.com", e.getMessage());
        }
    }

    public void testThrowsUpIOExceptionFromGit() throws Exception {
        final IOException ioException = new IOException();

        checking(new Expectations() {{
            one(commandExecutor).execute(new String[]{GIT_EXE, "ls-remote", REPO, BRANCH}, tmpDir); will(throwException(ioException));
        }});

        try {
            new ExecutorGitListRemoteCommand(GIT_EXE, commandExecutor).getLastCommit(REPO, BRANCH);
            fail("Should throw IOException");
        } catch (IOException e) {
            assertSame(ioException, e);
        }
    }

    private void assertCommandReturns(String expected) throws IOException {
        GitListRemoteCommand command = new ExecutorGitListRemoteCommand(GIT_EXE, commandExecutor);
        assertEquals(expected, command.getLastCommit(REPO, BRANCH));
    }

    private void mockReturnValue(final String value) throws IOException
    {
        checking(new Expectations() {{
            one(commandExecutor).execute(new String[]{GIT_EXE, "ls-remote", REPO, BRANCH}, tmpDir);
            will(returnValue(value));
        }});
    }
}