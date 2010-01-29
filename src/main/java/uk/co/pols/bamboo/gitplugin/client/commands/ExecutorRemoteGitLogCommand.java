package uk.co.pols.bamboo.gitplugin.client.commands;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.atlassian.bamboo.commit.Commit;
import com.atlassian.bamboo.commit.CommitFile;
import com.atlassian.bamboo.commit.CommitFileImpl;
import com.atlassian.bamboo.commit.CommitImpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ExecutorRemoteGitLogCommand implements GitLogCommand {
    private static final Log log = LogFactory.getLog(ExecutorRemoteGitLogCommand.class);

    private final Pattern GIT_REPO_PATTERN = Pattern.compile("^ssh://(.*?)(/.*)$");

    private final String repositoryUrl;
    private final String branch;
    private String lastBuiltRevision;
    private final CommandExecutor commandExecutor;

    public ExecutorRemoteGitLogCommand(String repositoryUrl, String branch, String lastBuiltRevision, CommandExecutor commandExecutor) {
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
        this.lastBuiltRevision = lastBuiltRevision;
        this.commandExecutor = commandExecutor;
    }

    public List<Commit> extractCommits() throws IOException {
        /*
         * If our git repo uses the ssh protocol, we can remotely execute
         * git log commands to obtain the history. If not, we'll just send
         * back some placeholder info to keep Bamboo happy.
         */

        Matcher m = GIT_REPO_PATTERN.matcher(repositoryUrl);
        if (!m.matches()) {
            return makeDummyCommitHistory();
        }

        String sshUserAndHost = m.group(1);
        String repositoryPath = m.group(2);

        final String[] commandLine = getCommandLine(sshUserAndHost, repositoryPath);
        log.info(Arrays.toString(commandLine));
        String logText = commandExecutor.execute(commandLine, new File(System.getProperty("java.io.tmpdir")));

        GitLogParser logParser = new GitLogParser(logText);

        List<Commit> commits = logParser.extractCommits(null);
        lastBuiltRevision = logParser.getMostRecentCommitDate();

        return commits;
    }

    public String getLastRevisionChecked() {
        throw new IllegalStateException("Last revision checked not available for remote log command.");
    }

    public String getHeadRevision(String branch) throws IOException {
        throw new IllegalStateException("Head revision checked not available for remote log command.");
    }

    private List<Commit> makeDummyCommitHistory() {
        final Commit commit = new CommitImpl("git committers");
        final CommitFileImpl file = new CommitFileImpl("files");
        file.setRevision(lastBuiltRevision);
        commit.setFiles(Collections.<CommitFile>singletonList(file));
        return Collections.singletonList(commit);
    }

    private String[] getCommandLine(String sshTarget, String repoDir) {
        String revisionRange =  String.format("%s..%s", lastBuiltRevision == null ? branch + "~" : lastBuiltRevision, branch);
        return new String[] {"ssh", sshTarget, "git", "--git-dir=" + repoDir, "log", revisionRange, "--numstat", "--date=iso8601"};
    }
}