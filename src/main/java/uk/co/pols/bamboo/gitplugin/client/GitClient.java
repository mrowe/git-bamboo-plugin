package uk.co.pols.bamboo.gitplugin.client;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.commit.Commit;

import java.io.File;
import java.util.List;

import uk.co.pols.bamboo.gitplugin.GitRepositoryConfig;

public interface GitClient {
    void initialiseRepository(BuildLogger buildLogger, String gitRepositoryConfig, String sourceCodeDirectory, File planKey) throws RepositoryException;

    String getLatestRevision(BuildLogger buildLogger, String repositoryUrl, String branch, String planKey) throws RepositoryException;

    String pullFromRemote(BuildLogger buildLogger, String repositoryUrl, String branch, String planKey, String lastRevisionChecked, File sourceCodeDirectory) throws RepositoryException;
}
