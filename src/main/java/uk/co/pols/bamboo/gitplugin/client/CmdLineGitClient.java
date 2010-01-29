package uk.co.pols.bamboo.gitplugin.client;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.commit.Commit;
import com.atlassian.bamboo.repository.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.co.pols.bamboo.gitplugin.client.commands.*;

public class CmdLineGitClient implements GitClient {
    private static final Log log = LogFactory.getLog(CmdLineGitClient.class);
    private GitCommandDiscoverer gitCommandDiscoverer = gitCommandDiscoverer();

    public String getLatestRevision(String repositoryUrl, String branch, String planKey) throws RepositoryException {
        log.info("Checking for changes on '" + planKey + "' at '" + branch + "' @ '" + repositoryUrl + "'");
        try {
            return listRemoteCommand().getLastCommit(repositoryUrl, branch);
        } catch (IOException e) {
            throw new RepositoryException("Could not get latest revision from remote repository '" + repositoryUrl + "'", e);
        }
    }

    public String pullFromRemote(BuildLogger buildLogger, String repositoryUrl, String branch, String planKey, File sourceCodeDirectory) throws RepositoryException {
        log.info(buildLogger.addBuildLogEntry("Pulling changes on '" + planKey + "' from '" + branch + "' @ '" + repositoryUrl));

        try {
            pullCommand(sourceCodeDirectory).pullUpdatesFromRemoteRepository(buildLogger, repositoryUrl, branch);
            final String headRevision = logCommand(sourceCodeDirectory, null /* FIXME */).getHeadRevision(branch);
            if (headRevision.length() < 1) {
                throw new RepositoryException("Could not determine revision for changes pulled into '" + sourceCodeDirectory.getAbsolutePath() + "' from '" + repositoryUrl + "'.");
            }
            return headRevision;
        } catch (IOException e) {
            throw new RepositoryException("Could not update working dir '" + sourceCodeDirectory.getAbsolutePath() + "' from remote repository '" + repositoryUrl + "'", e);
        }
    }

    public List<Commit> getChangesSince(BuildLogger buildLogger, String repositoryUrl, String branch, String planKey, String fromRevision) throws RepositoryException {
        log.info(buildLogger.addBuildLogEntry("Getting changes on '" + planKey + "' at '" + branch + "' @ '" + repositoryUrl + "' since commit '" + fromRevision + "'"));
        try
        {
            return remoteLogCommand(repositoryUrl, branch, fromRevision).extractCommits();
        } catch (IOException e) {
            throw new RepositoryException("Failed to get history from remote repo", e);
        }
    }

    public void initialiseRepository(BuildLogger buildLogger, String repositoryUrl, String branch, File sourceCodeDirectory) throws RepositoryException {
        log.info(buildLogger.addBuildLogEntry(sourceCodeDirectory.getAbsolutePath() + " is empty. Creating new git repository."));
        try {
            sourceCodeDirectory.mkdirs();
            initCommand(sourceCodeDirectory).init(buildLogger);
            remoteCommand(sourceCodeDirectory).add_origin(repositoryUrl, branch, buildLogger);
        } catch (IOException e) {
            throw new RepositoryException("Failed to initialise repository", e);
        }
    }

    protected GitPullCommand pullCommand(File sourceCodeDirectory) {
        return new ExecutorGitPullCommand(gitCommandDiscoverer.gitCommand(), sourceCodeDirectory, new AntCommandExecutor());
    }

    protected GitListRemoteCommand listRemoteCommand() {
        return new ExecutorGitListRemoteCommand(gitCommandDiscoverer.gitCommand(), new AntCommandExecutor());
    }

    protected GitLogCommand logCommand(File sourceCodeDirectory, String lastRevisionChecked) {
        return new ExecutorGitLogCommand(gitCommandDiscoverer.gitCommand(), sourceCodeDirectory, lastRevisionChecked, new AntCommandExecutor());
    }

    protected GitLogCommand remoteLogCommand(String repositoryUrl, String branch, String lastRevisionChecked)
    {
        return new ExecutorRemoteGitLogCommand(repositoryUrl, branch, lastRevisionChecked, new AntCommandExecutor());
    }

    protected GitInitCommand initCommand(File sourceCodeDirectory) {
        return new ExecutorGitInitCommand(gitCommandDiscoverer.gitCommand(), sourceCodeDirectory, new AntCommandExecutor());
    }

    protected GitRemoteCommand remoteCommand(File sourceCodeDirectory) {
        return new ExecutorGitRemoteCommand(gitCommandDiscoverer.gitCommand(), sourceCodeDirectory, new AntCommandExecutor());
    }

    protected GitCommandDiscoverer gitCommandDiscoverer() {
        return new BestGuessGitCommandDiscoverer(new AntCommandExecutor());
    }
}