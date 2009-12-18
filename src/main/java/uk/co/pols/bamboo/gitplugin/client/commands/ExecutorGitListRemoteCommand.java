package uk.co.pols.bamboo.gitplugin.client.commands;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExecutorGitListRemoteCommand implements GitListRemoteCommand {
    private static final Log log = LogFactory.getLog(ExecutorGitListRemoteCommand.class);

    private final String gitExe;
    private final CommandExecutor commandExecutor;

    private static final Pattern GIT_SHA1_PATTERN = Pattern.compile("([0-9a-f]+)\\s+(.*)");

    public ExecutorGitListRemoteCommand(String gitExe, CommandExecutor commandExecutor) {
        this.gitExe = gitExe;
        this.commandExecutor = commandExecutor;
    }

    public String getLastCommit(String repositoryUrl, String branch) throws IOException {
        log.info("Running '" + gitExe + " ls-remote '" + repositoryUrl + "' " + branch);
        final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        final String result = commandExecutor.execute(new String[] {gitExe, "ls-remote", repositoryUrl, branch}, tmpDir);
        final Matcher matcher = GIT_SHA1_PATTERN.matcher(result.trim());
        if (matcher.matches()) {
            final String sha1 = matcher.group(1);
            log.info("Latest commit for branch '" + branch + "' @ '" + repositoryUrl + "' is '" + sha1 + "'.");
            return sha1;
        }
        throw new IOException("Could not determine latest commit for branch '" + branch + "' @ '" + repositoryUrl + "'. git-ls-remote: " + result);
    }
}