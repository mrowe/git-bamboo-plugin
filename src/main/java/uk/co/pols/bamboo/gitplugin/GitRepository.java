package uk.co.pols.bamboo.gitplugin;

import java.io.File;
import java.util.List;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.commit.Commit;
import com.atlassian.bamboo.commit.CommitFile;
import com.atlassian.bamboo.repository.AbstractRepository;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.repository.WebRepositoryEnabledRepository;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.BuildChanges;
import com.atlassian.bamboo.v2.build.BuildChangesImpl;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import uk.co.pols.bamboo.gitplugin.client.CmdLineGitClient;
import uk.co.pols.bamboo.gitplugin.client.GitClient;

public class GitRepository extends AbstractRepository implements WebRepositoryEnabledRepository {
    private final GitRepositoryConfig gitRepositoryConfig = gitRepositoryConfig();

    /*
     * This is called by bamboo when a build has been triggered to calculate the changes since the previous build.
     * It is executed on the server.  It does not get run on the intial build, so may have to handle an empty git repo 
     * when the sencond build is triggered
     */
    public synchronized BuildChanges collectChangesSinceLastBuild(final String planKey, final String lastBuiltRevisionKey) throws RepositoryException {
        final BuildLogger buildLogger = buildLoggerManager.getBuildLogger(planKey);
        final String latestRevision = gitClient().getLatestRevision(
                gitRepositoryConfig.getRepositoryUrl(),
                gitRepositoryConfig.getBranch(),
                planKey);

        if (latestRevision.equals(lastBuiltRevisionKey)) {
            return new BuildChangesImpl(latestRevision);
        }

        final List<Commit> commits = gitClient().getChangesSince(
                buildLogger,
                gitRepositoryConfig.getRepositoryUrl(),
                gitRepositoryConfig.getBranch(),
                planKey,
                lastBuiltRevisionKey);

        return new BuildChangesImpl(latestRevision, commits);
    }

    /**
     * This is called by the agent to get the latest code.
     */
    public String retrieveSourceCode(final String planKey, final String vcsRevisionKey) throws RepositoryException {
        final BuildLogger buildLogger = buildLoggerManager.getBuildLogger(planKey);
        final File sourceCodeDirectory = getSourceCodeDirectory(planKey);

        if (isWorkspaceEmpty(sourceCodeDirectory)) {
            gitClient().initialiseRepository(
                    buildLogger,
                    gitRepositoryConfig.getRepositoryUrl(),
                    gitRepositoryConfig.getBranch(),
                    sourceCodeDirectory);
        }

        return gitClient().pullFromRemote(
                buildLogger,
                gitRepositoryConfig.getRepositoryUrl(),
                gitRepositoryConfig.getBranch(),
                planKey,
                sourceCodeDirectory);
    }

    @Override
    public ErrorCollection validate(BuildConfiguration buildConfiguration) {
        return gitRepositoryConfig.validate(super.validate(buildConfiguration), buildConfiguration);
    }

    public boolean isRepositoryDifferent(Repository repository) {
        if (repository instanceof GitRepository) {
            GitRepository gitRepository = (GitRepository) repository;
            return !new EqualsBuilder()
                    .append(this.getName(), gitRepository.getName())
                    .append(getRepositoryUrl(), gitRepository.getRepositoryUrl())
                    .isEquals();
        }
        return true;
    }

    public void addDefaultValues(BuildConfiguration buildConfiguration) {
        super.addDefaultValues(buildConfiguration);
        gitRepositoryConfig.addDefaultValues(buildConfiguration);
    }

    public void prepareConfigObject(BuildConfiguration buildConfiguration) {
    }

    @Override
    public void populateFromConfig(HierarchicalConfiguration config) {
        super.populateFromConfig(config);
        gitRepositoryConfig.populateFromConfig(config);
    }

    @Override
    public HierarchicalConfiguration toConfiguration() {
        return gitRepositoryConfig.toConfiguration(super.toConfiguration());
    }

    public String getName() {
        return "Git";
    }

    public String getUrl() {
        return "http://github.com/guides/home";
    }


    public void setRepositoryUrl(String repositoryUrl) {
        gitRepositoryConfig.setRepositoryUrl(repositoryUrl);
    }

    public String getRepositoryUrl() {
        return gitRepositoryConfig.getRepositoryUrl();
    }

    public String getBranch() {
        return gitRepositoryConfig.getBranch();
    }

    public void setBranch(String branch) {
        gitRepositoryConfig.setBranch(branch);
    }

    public boolean hasWebBasedRepositoryAccess() {
        return gitRepositoryConfig.hasWebBasedRepositoryAccess();
    }

    public void setWebRepositoryUrl(String url) {
        gitRepositoryConfig.setWebRepositoryUrl(url);
    }

    public void setWebRepositoryUrlRepoName(String url) {
    }

    public String getWebRepositoryUrl() {
        return gitRepositoryConfig.getWebRepositoryUrl();
    }

    public String getWebRepositoryUrlRepoName() {
        return null;
    }

    public String getWebRepositoryUrlForFile(CommitFile commitFile) {
        return gitRepositoryConfig.getWebRepositoryUrlForFile(commitFile);
    }

    @Override
    public String getWebRepositoryUrlForCommit(Commit commit) {
        return gitRepositoryConfig.getWebRepositoryUrlForCommit(commit);
    }

    public String getWebRepositoryUrlForDiff(CommitFile file) {
        return gitRepositoryConfig.getWebRepositoryUrlForDiff(file);
    }

    public String getWebRepositoryUrlForRevision(CommitFile file) {
        return gitRepositoryConfig.getWebRepositoryUrlForRevision(file);
    }

    public String getHost() {
        return gitRepositoryConfig.getHost();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(101, 11)
                .append(getKey())
                .append(getRepositoryUrl())
                .append(getTriggerIpAddress())
                .toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GitRepository)) {
            return false;
        }
        GitRepository rhs = (GitRepository) o;
        return new EqualsBuilder()
                .append(getRepositoryUrl(), rhs.getRepositoryUrl())
                .append(getTriggerIpAddress(), rhs.getTriggerIpAddress())
                .isEquals();
    }

    public int compareTo(Object obj) {
        GitRepository o = (GitRepository) obj;
        return new CompareToBuilder()
                .append(getRepositoryUrl(), o.getRepositoryUrl())
                .append(getTriggerIpAddress(), o.getTriggerIpAddress())
                .toComparison();
    }

    protected GitClient gitClient() {
        return new CmdLineGitClient();
    }

    protected GitRepositoryConfig gitRepositoryConfig() {
        return new GitRepositoryConfig();
    }
}
