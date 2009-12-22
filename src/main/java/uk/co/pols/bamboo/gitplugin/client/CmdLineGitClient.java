package uk.co.pols.bamboo.gitplugin.client;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.commit.*;
import com.atlassian.bamboo.repository.RepositoryException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.co.pols.bamboo.gitplugin.client.commands.*;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        // TODO actually get a list of commits from the repo
        /*
         * This is pretty dodgy, but it's going to be messy getting the actual change set, since we have to
         * do it from the remote repo. This is just a place-holder so Bamboo has enough information to send
         * out build-complete emails without blowing up (see http://jira.atlassian.com/browse/BAM-3770).
         */
        final String latestRevision = getLatestRevision(repositoryUrl, branch, planKey);
        final Commit commit = new CommitImpl("git committers");
        final CommitFileImpl file = new CommitFileImpl("files");
        file.setRevision(latestRevision);
        commit.setFiles(Collections.<CommitFile>singletonList(file));
        return Collections.singletonList(commit);
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