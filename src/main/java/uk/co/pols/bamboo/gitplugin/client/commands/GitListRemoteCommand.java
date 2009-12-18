package uk.co.pols.bamboo.gitplugin.client.commands;

import java.io.IOException;

public interface GitListRemoteCommand
{
    String getLastCommit(String repositoryUrl, String branch) throws IOException;
}
