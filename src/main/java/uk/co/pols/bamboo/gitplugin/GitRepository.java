package uk.co.pols.bamboo.gitplugin;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.commit.Commit;
import com.atlassian.bamboo.commit.CommitFile;
import com.atlassian.bamboo.repository.*;
import com.atlassian.bamboo.security.EncryptionException;
import com.atlassian.bamboo.security.StringEncrypter;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.BuildChanges;
import com.atlassian.bamboo.v2.build.BuildChangesImpl;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.repository.RepositoryEventAware;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.*;

/**
 * Provides GIT and GITHUB support for the Bamboo Build Server
 * <p/>
 * TODO Let user define the location of the git exe
 * TODO run a which git command to guess the location of git
 * TODO Add hook for github callback triggering the build
 * TODO don't include all historical commits on first build
 * TODO work out if the repository url has changed...
 * <p/>
 * This is what capistarno does....
 * git reset -q --hard 10e162370493a984c279ffc7ca59e18d7850e844;
 * git checkout -q -b deploy 10e162370493a984c279ffc7ca59e18d7850e844;
 * <p/>
 * So if I can do a remote history I'm laughing...
 */
public class GitRepository extends AbstractRepository implements /*SelectableAuthenticationRepository,*/ WebRepositoryEnabledRepository, InitialBuildAwareRepository, RepositoryEventAware {
    private static final Log log = LogFactory.getLog(GitRepository.class);

    public static final String NAME = "Git";
    public static final String KEY = "git";

    private static final String GIT_HOME = "/opt/local/bin";
    private static final String GIT_EXE = GIT_HOME + "/git";

    private static final String REPO_PREFIX = "repository.git.";
    public static final String GIT_REPO_URL = REPO_PREFIX + "repositoryUrl";
    public static final String GIT_BRANCH = REPO_PREFIX + "branch";
    public static final String SVN_USERNAME = REPO_PREFIX + "username";
    public static final String SVN_AUTH_TYPE = REPO_PREFIX + "authType";
    public static final String SVN_PASSWORD = REPO_PREFIX + "userPassword";
    public static final String SVN_KEYFILE = REPO_PREFIX + "keyFile";
    public static final String SVN_PASSPHRASE = REPO_PREFIX + "passphrase";

    private String repositoryUrl;
    private String branch;
    private String webRepositoryUrl;
    private String username;
    private String password;
    private String passphrase;
    private String keyFile;
    private String webRepositoryUrlRepoName;
    private String authType;

    public synchronized BuildChanges collectChangesSinceLastBuild(String planKey, String lastVcsRevisionKey) throws RepositoryException {
        List<Commit> commits = new ArrayList<Commit>();

        String latestCommitTime = gitClient().getLatestUpdate(
                buildLoggerManager.getBuildLogger(planKey),
                repositoryUrl,
                planKey,
                lastVcsRevisionKey,
                commits,
                getSourceCodeDirectory(planKey)
        );

        return new BuildChangesImpl(String.valueOf(latestCommitTime), commits);
    }

    public String retrieveSourceCode(String planKey, String vcsRevisionKey) throws RepositoryException {
        File sourceDirectory = getSourceCodeDirectory(planKey);
        BuildLogger buildLogger = buildLoggerManager.getBuildLogger(planKey);
        GitClient gitClient = gitClient();

        if (isWorkspaceEmpty(sourceDirectory)) {
            gitClient.initialiseRemoteRepository(sourceDirectory, repositoryUrl, buildLogger);
        }
        return gitClient.getLatestUpdate(buildLogger, repositoryUrl, planKey, vcsRevisionKey, new ArrayList<Commit>(), sourceDirectory);
    }

    public void preRetrieveSourceCode(final BuildContext buildContext) {
        // what to do if the repository has changed...
    }

    public void postRetrieveSourceCode(final BuildContext buildContext) {
    }

    @Override
    public ErrorCollection validate(BuildConfiguration buildConfiguration) {
        ErrorCollection errorCollection = super.validate(buildConfiguration);

        validateMandatoryField(buildConfiguration, errorCollection, GIT_REPO_URL, "Please specify where the repository is located");
        validateMandatoryField(buildConfiguration, errorCollection, GIT_BRANCH, "Please specify which branch you want to build");

        return errorCollection;
    }

    public boolean isRepositoryDifferent(Repository repository) {
        if (repository instanceof GitRepository) {
            GitRepository gitRepository = (GitRepository) repository;
            return !new EqualsBuilder()
                    .append(this.getName(), gitRepository.getName())
                    .append(getRepositoryUrl(), gitRepository.getRepositoryUrl())
                    .isEquals();
        } else {
            return true;
        }
    }

    public void prepareConfigObject(BuildConfiguration buildConfiguration) {
    }

    @Override
    public void populateFromConfig(HierarchicalConfiguration config) {
        super.populateFromConfig(config);

        setRepositoryUrl(config.getString(GIT_REPO_URL));
        setBranch(config.getString(GIT_BRANCH));
    }

    @Override
    public HierarchicalConfiguration toConfiguration() {
        HierarchicalConfiguration configuration = super.toConfiguration();
        configuration.setProperty(GIT_REPO_URL, getRepositoryUrl());
        configuration.setProperty(GIT_BRANCH, getBranch());

        return configuration;
    }

    public void onInitialBuild(BuildContext buildContext) {
    }

    public String getName() {
        return NAME;
    }

    public String getPassphrase() {
        try {
            StringEncrypter stringEncrypter = new StringEncrypter();
            return stringEncrypter.decrypt(passphrase);
        }
        catch (Exception e) {
            return null;
        }
    }

    public void setPassphrase(String passphrase) {
        try {
            if (StringUtils.isNotEmpty(passphrase)) {
                StringEncrypter stringEncrypter = new StringEncrypter();
                this.passphrase = stringEncrypter.encrypt(passphrase);
            } else {
                this.passphrase = passphrase;
            }
        }
        catch (EncryptionException e) {
            log.error("Failed to encrypt password", e);
            this.passphrase = null;
        }
    }

    public String getEncryptedPassphrase() {
        return passphrase;
    }

    public void setEncryptedPassphrase(String encryptedPassphrase) {
        passphrase = encryptedPassphrase;
    }

    public String getKeyFile() {
        return keyFile;
    }

    public String getSubstitutedKeyFile() {
        return variableSubstitutionBean.substituteBambooVariables(keyFile);
    }

    public void setKeyFile(String myKeyFile) {
        this.keyFile = myKeyFile;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getUrl() {
        return "http://github.com/guides/home";
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = StringUtils.trim(repositoryUrl);
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public void setUsername(String username) {
        this.username = StringUtils.trim(username);
    }

    public String getUsername() {
        return username;
    }

    public void setUserPassword(String password) {
        try {
            if (StringUtils.isNotEmpty(password)) {
                StringEncrypter stringEncrypter = new StringEncrypter();
                this.password = stringEncrypter.encrypt(password);
            } else {
                this.password = password;
            }
        }
        catch (EncryptionException e) {
            log.error("Failed to encrypt password", e);
            this.password = null;
        }
    }

    public String getUserPassword() {
        try {
            StringEncrypter stringEncrypter = new StringEncrypter();
            return stringEncrypter.decrypt(password);
        }
        catch (Exception e) {
            return null;
        }
    }


    public String getEncryptedPassword() {
        return password;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        password = encryptedPassword;
    }

    public boolean hasWebBasedRepositoryAccess() {
        return StringUtils.isNotBlank(webRepositoryUrl);
    }

    public String getWebRepositoryUrl() {
        return webRepositoryUrl;
    }

    /**
     * Return web repository URL with extrapolated Bamboo variables
     *
     * @return Web repository URL with extrapolated Bamboo variables
     */
    public String getSubstitutedWebRepositoryUrl() {
        return variableSubstitutionBean.substituteBambooVariables(webRepositoryUrl);
    }

    public void setWebRepositoryUrl(String url) {
        webRepositoryUrl = StringUtils.trim(url);
    }

    public String getWebRepositoryUrlRepoName() {
        return webRepositoryUrlRepoName;
    }

    public void setWebRepositoryUrlRepoName(String repoName) {
        webRepositoryUrlRepoName = StringUtils.trim(repoName);
    }

    public String getWebRepositoryUrlForFile(CommitFile file) {
        ViewCvsFileLinkGenerator fileLinkGenerator = new ViewCvsFileLinkGenerator(getSubstitutedWebRepositoryUrl());
        return fileLinkGenerator.getWebRepositoryUrlForFile(file, webRepositoryUrlRepoName, ViewCvsFileLinkGenerator.SVN_REPO_TYPE);
    }

    public String getWebRepositoryUrlForDiff(CommitFile file) {
        ViewCvsFileLinkGenerator fileLinkGenerator = new ViewCvsFileLinkGenerator(getSubstitutedWebRepositoryUrl());
        return fileLinkGenerator.getWebRepositoryUrlForDiff(file, webRepositoryUrlRepoName, ViewCvsFileLinkGenerator.SVN_REPO_TYPE);
    }

    public String getWebRepositoryUrlForRevision(CommitFile file) {
        ViewCvsFileLinkGenerator fileLinkGenerator = new ViewCvsFileLinkGenerator(getSubstitutedWebRepositoryUrl());
        return fileLinkGenerator.getWebRepositoryUrlForRevision(file, webRepositoryUrlRepoName, ViewCvsFileLinkGenerator.SVN_REPO_TYPE);
    }

    @Override
    public String getWebRepositoryUrlForCommit(Commit commit) {
        ViewCvsFileLinkGenerator fileLinkGenerator = new ViewCvsFileLinkGenerator(getSubstitutedWebRepositoryUrl());
        return fileLinkGenerator.getWebRepositoryUrlForCommit(commit, webRepositoryUrlRepoName, ViewCvsFileLinkGenerator.SVN_REPO_TYPE);
    }

    public String getHost() {
        if (repositoryUrl == null) {
            return UNKNOWN_HOST;
        }

        // TODO work out how to extract the host form the various forms of git url
        return "github.com";
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(101, 11)
                .append(getKey())
                .append(getRepositoryUrl())
                .append(getUsername())
                .append(getEncryptedPassword())
                .append(getWebRepositoryUrl())
                .append(getWebRepositoryUrlRepoName())
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
                .append(getUsername(), rhs.getUsername())
                .append(getEncryptedPassword(), rhs.getEncryptedPassword())
                .append(getWebRepositoryUrl(), rhs.getWebRepositoryUrl())
                .append(getWebRepositoryUrlRepoName(), rhs.getWebRepositoryUrlRepoName())
                .append(getTriggerIpAddress(), rhs.getTriggerIpAddress())
                .isEquals();
    }

    public int compareTo(Object obj) {
        GitRepository o = (GitRepository) obj;
        return new CompareToBuilder()
                .append(getRepositoryUrl(), o.getRepositoryUrl())
                .append(getUsername(), o.getUsername())
                .append(getEncryptedPassword(), o.getEncryptedPassword())
                .append(getWebRepositoryUrl(), o.getWebRepositoryUrl())
                .append(getWebRepositoryUrlRepoName(), o.getWebRepositoryUrlRepoName())
                .append(getTriggerIpAddress(), o.getTriggerIpAddress())
                .toComparison();
    }

//    public List<NameValuePair> getAuthenticationTypes() {
//        List<NameValuePair> types = new ArrayList<NameValuePair>();
//        types.add(AuthenticationType.PASSWORD.getNameValue());
//        types.add(AuthenticationType.SSH.getNameValue());
//        return types;
//    }

    private void validateMandatoryField(BuildConfiguration buildConfiguration, ErrorCollection errorCollection, String fieldKey, String errorMessage) {
        if (StringUtils.isEmpty(buildConfiguration.getString(fieldKey))) {
            errorCollection.addError(fieldKey, errorMessage);
        }
    }

    protected GitClient gitClient() {
        return new CmdLineGitClient(GIT_EXE);
    }
}