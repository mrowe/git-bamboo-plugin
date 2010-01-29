package uk.co.pols.bamboo.gitplugin.client.commands;

import java.io.File;
import java.util.List;

import com.atlassian.bamboo.commit.Commit;

import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

public class ExecutorRemoteGitLogCommandTest extends MockObjectTestCase {

    private final CommandExecutor commandExecutor = mock(CommandExecutor.class);
    private static final String REPO_URL = "ssh://git@foo.com/srv/git/bar.git";
    private static final String BRANCH = "my-branch";
    private static final String LAST_REVISION = "deadcafe";

    public void testExtractCommitsForSshRepo() throws Exception {
        GitLogCommand gitLogCommand = new ExecutorRemoteGitLogCommand(REPO_URL, BRANCH, LAST_REVISION, commandExecutor);

        checking(new Expectations() {{
            one(commandExecutor).execute(new String[] {"ssh", "git@foo.com", "git", "--git-dir=/srv/git/bar.git", "log", LAST_REVISION + ".." + BRANCH, "--numstat", "--date=iso8601"}, new File(System.getProperty("java.io.tmpdir")));
            will(returnValue(mostRecentCommitLog));
        }});

        List<Commit> commits = gitLogCommand.extractCommits();
        assertEquals(1, commits.size());
    }

    public void testExtractCommitsForNonSshRepo() throws Exception {
        GitLogCommand gitLogCommand = new ExecutorRemoteGitLogCommand("git://foo.com/home/user/myrepo", BRANCH, LAST_REVISION, commandExecutor);
        List<Commit> commits = gitLogCommand.extractCommits();
        assertEquals(1, commits.size());
        Commit commit = commits.get(0);
        assertEquals("git committers", commit.getAuthor().getFullName());
        assertEquals(1, commit.getFiles().size());
        assertEquals("files", commit.getFiles().get(0).getName());
    }

    public void testGetLastRevisionChecked() throws Exception {
        try {
            new ExecutorRemoteGitLogCommand(REPO_URL, BRANCH, LAST_REVISION, commandExecutor).getLastRevisionChecked();
            fail("Should throw illegal state exeception");
        } catch (IllegalStateException e) {
            assertEquals("Last revision checked not available for remote log command.", e.getMessage());
        }
    }

    public void testGetHeadRevision() throws Exception {
        try {
            new ExecutorRemoteGitLogCommand(REPO_URL, BRANCH, LAST_REVISION, commandExecutor).getHeadRevision(BRANCH);
            fail("Should throw illegal state exeception");
        } catch (IllegalStateException e) {
            assertEquals("Head revision checked not available for remote log command.", e.getMessage());
        }
    }

    private final String mostRecentCommitLog =
        "commit 60f6a6cabe727b14897b4d98bca91ce646a07d3d\n" +
            "Author: Andy Pols <andy@pols.co.uk>\n" +
            "Date:   2008-03-13 01:27:52 +0000\n" +
            "\n" +
            "    Initial plugin - just Adds Git to the repository dropdown... does not actually do anything just yet!\n" +
            "\n";

}
