package uk.co.pols.bamboo.gitplugin;

import static uk.co.pols.bamboo.gitplugin.SampleCommitFactory.commitFile;
import static uk.co.pols.bamboo.gitplugin.SampleCommitFactory.commitWithFile;
import com.atlassian.bamboo.build.BuildLoggerManager;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.commit.Commit;
import com.atlassian.bamboo.commit.CommitImpl;
import com.atlassian.bamboo.repository.AbstractRepository;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.repository.cvsimpl.CVSRepository;
import com.atlassian.bamboo.v2.build.BuildChanges;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;
import uk.co.pols.bamboo.gitplugin.client.commands.GitCommandDiscoverer;
import uk.co.pols.bamboo.gitplugin.client.GitClient;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GitRepositoryTest extends MockObjectTestCase {
    private static final String PLAN_KEY = "plan-key";
    private static final File SRC_CODE_DIR = new File("test/src/code/directory");
    private static final String RESPOSITORY_URL = "RepositoryUrl";
    private static final String REPOSITORY_BRANCH = "branch";

    private GitRepositoryConfig gitRepositoryConfig = new GitRepositoryConfig();
    private GitClient gitClient = mock(GitClient.class);
    private BuildLoggerManager buildLoggerManager = mock(BuildLoggerManager.class);
    private BuildLogger buildLogger = mock(BuildLogger.class);
    private GitCommandDiscoverer commandDiscoverer = mock(GitCommandDiscoverer.class);
    private GitRepository gitRepository = gitRepository(false);

    public void testProvidesANameToAppearInTheGuiRepositoryDrownDown() {
        assertEquals("Git", gitRepository.getName());
    }

    public void testProvidesALinkToTheGitHubGuidesPage() {
        assertEquals("http://github.com/guides/home", gitRepository.getUrl());
    }

    public void testClassesARepositoryOfADifferentTypeAsDifferent() {
        assertTrue(gitRepository.isRepositoryDifferent(new TestRepository()));
    }

    public void testClassesANullRepositoryAsDifferent() {
        assertTrue(gitRepository.isRepositoryDifferent(null));
    }

    public void testClassesAGitRepositoryWithADifferentUrlAsDifferent() {
        GitRepository repositoryToCompare = new GitRepository();
        repositoryToCompare.setRepositoryUrl("repositoryToCompareURL");

        gitRepository.setRepositoryUrl("gitRepositoryURL");

        assertTrue(gitRepository.isRepositoryDifferent(repositoryToCompare));
    }

    public void testClassesAGitRepositoryWithTheSameUrlAsBeingTheSame() {
        GitRepository repositoryToCompare = new GitRepository();
        repositoryToCompare.setRepositoryUrl("repositoryUrl");

        gitRepository.setRepositoryUrl("repositoryUrl");

        assertFalse(gitRepository.isRepositoryDifferent(repositoryToCompare));
    }

    public void testReturnsEmptyBuildChangesIfLatestRevisionHasntChanged() throws RepositoryException {
        checking(new Expectations() {{
            one(buildLoggerManager).getBuildLogger(PLAN_KEY); will(returnValue(buildLogger));
            one(gitClient).getLatestRevision(RESPOSITORY_URL, REPOSITORY_BRANCH, PLAN_KEY); will(returnValue("commit"));
        }});

        BuildChanges buildChanges = gitRepository.collectChangesSinceLastBuild(PLAN_KEY, "commit");
        assertNull(buildChanges.getChanges());
        assertEquals("commit", buildChanges.getVcsRevisionKey());
    }

    public void testUsesAGitClientToDetectTheChangesSinceTheLastBuild() throws RepositoryException {
        final List<CommitImpl> commits = Collections.singletonList(new CommitImpl("author"));
        checking(new Expectations() {{
            one(buildLoggerManager).getBuildLogger(PLAN_KEY); will(returnValue(buildLogger));
            one(gitClient).getLatestRevision(RESPOSITORY_URL, REPOSITORY_BRANCH, PLAN_KEY); will(returnValue("latest commit"));
            one(gitClient).getChangesSince(buildLogger, RESPOSITORY_URL, REPOSITORY_BRANCH, PLAN_KEY, "previous commit"); will(returnValue(commits));
        }});

        BuildChanges buildChanges = gitRepository.collectChangesSinceLastBuild(PLAN_KEY, "previous commit");
        assertEquals("latest commit", buildChanges.getVcsRevisionKey());
        final List<Commit> changes = buildChanges.getChanges();
        assertNotNull(changes);
        assertEquals(1, changes.size());
        assertEquals("author", changes.get(0).getAuthor().getFullName());
    }

    public void testInitialisesTheRepositoryIfTheWorkspaceIsEmpty() throws RepositoryException {
        checking(new Expectations() {{
            one(buildLoggerManager).getBuildLogger(PLAN_KEY); will(returnValue(buildLogger));
            one(gitClient).initialiseRepository(buildLogger, RESPOSITORY_URL, REPOSITORY_BRANCH, SRC_CODE_DIR);
            one(gitClient).pullFromRemote(buildLogger, RESPOSITORY_URL, REPOSITORY_BRANCH, PLAN_KEY, SRC_CODE_DIR); will(returnValue("last commit"));
        }});

        assertEquals("last commit", gitRepository(true).retrieveSourceCode(PLAN_KEY, "a commit"));
    }

    public void testChecksOutTheSourceCodeIfTheIfTheWorkspaceIsNotEmpty() throws RepositoryException {
        checking(new Expectations() {{
            one(buildLoggerManager).getBuildLogger(PLAN_KEY); will(returnValue(buildLogger));
            one(gitClient).pullFromRemote(buildLogger, RESPOSITORY_URL, REPOSITORY_BRANCH, PLAN_KEY, SRC_CODE_DIR); will(returnValue("last commit"));
        }});

        assertEquals("last commit", gitRepository(false).retrieveSourceCode(PLAN_KEY, "a commit"));
    }

    public void testARepositoryThatIsNotAGitRepositoryIsClearlyDifferent() {
        assertTrue(gitRepository(false).isRepositoryDifferent(new CVSRepository()));
    }

    public void testARepositoryWithADifferentRepositoryIsDifferent() {
        GitRepository gitRepository = gitRepository(false);
        gitRepository.setRepositoryUrl("one/url");

        GitRepository differentRepository = new GitRepository();
        differentRepository.setRepositoryUrl("other/url");

        assertTrue(gitRepository.isRepositoryDifferent(differentRepository));
    }

    public void testProvidesBambooWithWebUrlAllowingTheCodeChangePageLinkBackToGitHub() {
        GitRepository gitRepository = gitRepository(false);
        gitRepository.setWebRepositoryUrl("https://github.com/andypols/git-bamboo-plugin");

        assertTrue(gitRepository.hasWebBasedRepositoryAccess());
        assertEquals("https://github.com/andypols/git-bamboo-plugin/commit/71b2bf41fb82a12ca3d4d34bd62568d9167dc6d6", gitRepository.getWebRepositoryUrlForCommit(commitWithFile("71b2bf41fb82a12ca3d4d34bd62568d9167dc6d6")));
        assertEquals("https://github.com/andypols/git-bamboo-plugin/blob/71b2bf41fb82a12ca3d4d34bd62568d9167dc6d6/src/main/java/uk/co/pols/bamboo/gitplugin/GitRepository.java", gitRepository.getWebRepositoryUrlForFile(commitFile("71b2bf41fb82a12ca3d4d34bd62568d9167dc6d6")));
    }

    private GitRepository gitRepository(final boolean isWorkspaceEmpty) {
        gitRepository = new GitRepository() {
            protected GitClient gitClient() {
                return gitClient;
            }

            public File getSourceCodeDirectory(String projectKey) {
                return SRC_CODE_DIR;
            }

            protected boolean isWorkspaceEmpty(File file) {
                return isWorkspaceEmpty;
            }

            protected GitRepositoryConfig gitRepositoryConfig() {
                return gitRepositoryConfig;
            }

            protected GitCommandDiscoverer gitCommandDiscoverer() {
                return commandDiscoverer;
            }
        };

        gitRepository.setBuildLoggerManager(buildLoggerManager);
        gitRepository.setRepositoryUrl(RESPOSITORY_URL);
        gitRepository.setBranch(REPOSITORY_BRANCH);
        return gitRepository;
    }

    class TestRepository extends AbstractRepository {

        public String getName() {
            return null;
        }

        public String getHost() {
            return null;
        }

        public boolean isRepositoryDifferent(Repository repository) {
            return false;
        }

        public BuildChanges collectChangesSinceLastBuild(String string, String string1) throws RepositoryException {
            return null;
        }

        public String retrieveSourceCode(String string, String string1) throws RepositoryException {
            return null;
        }

        public void prepareConfigObject(BuildConfiguration buildConfiguration) {
        }
    }
}
