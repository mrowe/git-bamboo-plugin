package uk.co.pols.bamboo.gitplugin;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.commit.Commit;
import com.atlassian.bamboo.repository.RepositoryException;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;

import java.io.File;
import java.io.IOException;
import java.util.List;

import uk.co.pols.bamboo.gitplugin.client.CmdLineGitClient;
import uk.co.pols.bamboo.gitplugin.client.commands.*;

public class CmdLineGitClientTest extends MockObjectTestCase {
    private static final String LAST_REVISION_CHECKED = "1cc17a8203f7c5c82e89ae5f687d12b7be65951e";
    private static final String REPOSITORY_URL = "repository.url";
    private static final String REPOSITORY_BRANCH = "master";
    private static final String PLAN_KEY = "plankey";
    private static final File SOURCE_CODE_DIRECTORY = new File("src");

    private BuildLogger buildLogger = mock(BuildLogger.class);
    private GitPullCommand gitPullCommand = mock(GitPullCommand.class);
    private GitLogCommand gitLogCommand = mock(GitLogCommand.class);
    private GitInitCommand gitInitCommand = mock(GitInitCommand.class);
    private GitRemoteCommand gitRemoteCommand = mock(GitRemoteCommand.class);
    private GitListRemoteCommand gitListRemoteCommand = mock(GitListRemoteCommand.class);
    private CmdLineGitClient gitClient = gitClient();

    /*
    String getLatestRevision(BuildLogger buildLogger, String repositoryUrl, String branch, String planKey) throws RepositoryException;
     */

    public void testGetLatestRevisionReturnsTheMostRecentRemoteCommit() throws RepositoryException, IOException {
        checking(new Expectations() {{
            one(gitListRemoteCommand).getLastCommit(REPOSITORY_URL, REPOSITORY_BRANCH); will(returnValue(LAST_REVISION_CHECKED));
        }});

        String latestUpdate = gitClient.getLatestRevision(REPOSITORY_URL, REPOSITORY_BRANCH, PLAN_KEY);

        assertEquals(LAST_REVISION_CHECKED, latestUpdate);
    }

    public void testGetLatestRevisionWrapsExceptionGettingLastCommit() throws RepositoryException, IOException {
        final IOException ioException = new IOException("EXPECTED EXCEPTION");

        checking(new Expectations() {{
            one(gitListRemoteCommand).getLastCommit(REPOSITORY_URL, REPOSITORY_BRANCH); will(throwException(ioException));
        }});

        try {
            gitClient.getLatestRevision(REPOSITORY_URL, REPOSITORY_BRANCH, PLAN_KEY);
            fail("Should throw RepositoryException");
        } catch (RepositoryException e) {
            assertEquals("Could not get latest revision from remote repository 'repository.url'", e.getMessage());
            assertSame(ioException, e.getCause());
        }
    }

    public void testPullFromRemoteCallsGitCommandAndReturnsResult() throws RepositoryException, IOException {
        checking(new Expectations() {{
            one(buildLogger).addBuildLogEntry("Pulling changes on 'plankey' from 'master' @ 'repository.url");
            one(gitPullCommand).pullUpdatesFromRemoteRepository(buildLogger, REPOSITORY_URL, REPOSITORY_BRANCH);
            one(gitLogCommand).getHeadRevision(REPOSITORY_BRANCH); will(returnValue(LAST_REVISION_CHECKED));
        }});

        final String result = gitClient.pullFromRemote(buildLogger, REPOSITORY_URL, REPOSITORY_BRANCH, PLAN_KEY, SOURCE_CODE_DIRECTORY);
        assertEquals(LAST_REVISION_CHECKED, result);
    }

    public void testPullFromRemoteWrapsExceptionPullingUpdates() throws RepositoryException, IOException {
        final IOException ioException = new IOException("EXPECTED EXCEPTION");

        checking(new Expectations() {{
            one(buildLogger).addBuildLogEntry("Pulling changes on 'plankey' from 'master' @ 'repository.url");
            one(gitPullCommand).pullUpdatesFromRemoteRepository(buildLogger, REPOSITORY_URL, REPOSITORY_BRANCH); will(throwException(ioException));
        }});

        try {
            gitClient.pullFromRemote(buildLogger, REPOSITORY_URL, REPOSITORY_BRANCH, PLAN_KEY, SOURCE_CODE_DIRECTORY);
            fail("Should throw RepositoryException");
        } catch (RepositoryException e) {
            assertEquals("Could not update working dir '/Users/mrowe/src/java_crap/git-bamboo-plugin/src' from remote repository 'repository.url'", e.getMessage());
            assertSame(ioException, e.getCause());
        }
    }

    public void testPullFromRemoteWrapsGetHeadRevisionException() throws RepositoryException, IOException {
        final IOException ioException = new IOException("EXPECTED EXCEPTION");

        checking(new Expectations() {{
            one(buildLogger).addBuildLogEntry("Pulling changes on 'plankey' from 'master' @ 'repository.url");
            one(gitPullCommand).pullUpdatesFromRemoteRepository(buildLogger, REPOSITORY_URL, REPOSITORY_BRANCH);
            one(gitLogCommand).getHeadRevision(REPOSITORY_BRANCH); will(throwException(ioException));
        }});

        try {
            gitClient.pullFromRemote(buildLogger, REPOSITORY_URL, REPOSITORY_BRANCH, PLAN_KEY, SOURCE_CODE_DIRECTORY);
            fail("Should throw RepositoryException");
        } catch (RepositoryException e) {
            assertEquals("Could not update working dir '/Users/mrowe/src/java_crap/git-bamboo-plugin/src' from remote repository 'repository.url'", e.getMessage());
            assertSame(ioException, e.getCause());
        }
    }

   public void testGetLatestChangesMakesChangeSetIdAvailable() throws RepositoryException, IOException {
       checking(new Expectations() {{
           one(buildLogger).addBuildLogEntry("Getting changes on 'plankey' at 'master' @ 'repository.url' since commit 'last revision'");
           one(gitListRemoteCommand).getLastCommit(REPOSITORY_URL, REPOSITORY_BRANCH); will(returnValue(LAST_REVISION_CHECKED));
       }});

       final List<Commit> commits = gitClient.getChangesSince(buildLogger, REPOSITORY_URL, REPOSITORY_BRANCH, PLAN_KEY, "last revision");
       assertEquals(1, commits.size());
       Commit commit = commits.get(0);
       assertNotNull(commit);
       assertEquals(LAST_REVISION_CHECKED, commit.guessChangeSetId());
   }

    public void testInitialiseRepositoryCreatesANewLocalRepository() throws RepositoryException, IOException {
        checking(new Expectations() {{
            one(buildLogger).addBuildLogEntry(SOURCE_CODE_DIRECTORY.getAbsolutePath() + " is empty. Creating new git repository.");
            one(gitInitCommand).init(buildLogger);
            one(gitRemoteCommand).add_origin(REPOSITORY_URL, REPOSITORY_BRANCH, buildLogger);
        }});

        gitClient.initialiseRepository(buildLogger, REPOSITORY_URL, REPOSITORY_BRANCH, SOURCE_CODE_DIRECTORY);
    }

    public void testInitialiseRepositoryWrapsInitException() throws RepositoryException, IOException {
        final IOException ioException = new IOException("EXPECTED EXCEPTION");

        checking(new Expectations() {{
            one(buildLogger).addBuildLogEntry(SOURCE_CODE_DIRECTORY.getAbsolutePath() + " is empty. Creating new git repository.");
            one(gitInitCommand).init(buildLogger); will(throwException(ioException));
        }});

        try {
            gitClient.initialiseRepository(buildLogger, REPOSITORY_URL, REPOSITORY_BRANCH, SOURCE_CODE_DIRECTORY);
            fail("Should throw RepositoryException");
        } catch (RepositoryException e) {
            assertEquals("Failed to initialise repository", e.getMessage());
            assertSame(ioException, e.getCause());
        }
    }

    public void testInitialiseRepositoryWrapsAddOriginException() throws RepositoryException, IOException {
        final IOException ioException = new IOException("EXPECTED EXCEPTION");

        checking(new Expectations() {{
            one(buildLogger).addBuildLogEntry(SOURCE_CODE_DIRECTORY.getAbsolutePath() + " is empty. Creating new git repository.");
            one(gitInitCommand).init(buildLogger);
            one(gitRemoteCommand).add_origin(REPOSITORY_URL, REPOSITORY_BRANCH, buildLogger); will(throwException(ioException));
        }});

        try {
            gitClient.initialiseRepository(buildLogger, REPOSITORY_URL, REPOSITORY_BRANCH, SOURCE_CODE_DIRECTORY);
            fail("Should throw RepositoryException");
        } catch (RepositoryException e) {
            assertEquals("Failed to initialise repository", e.getMessage());
            assertSame(ioException, e.getCause());
        }
    }

    private CmdLineGitClient gitClient() {
        return new CmdLineGitClient() {
            protected GitPullCommand pullCommand(File sourceCodeDirectory) {
                return gitPullCommand;
            }

            protected GitLogCommand logCommand(File sourceCodeDirectory, String lastRevisionChecked) {
                return gitLogCommand;
            }

            protected GitInitCommand initCommand(File sourceCodeDirectory) {
                return gitInitCommand;
            }

            protected GitRemoteCommand remoteCommand(File sourceCodeDirectory) {
                return gitRemoteCommand;
            }

            protected GitListRemoteCommand listRemoteCommand() {
                return gitListRemoteCommand;
            }
        };
    }
}