package uk.co.pols.bamboo.gitplugin.client.commands;

import com.atlassian.bamboo.build.logger.BuildLogger;

import java.io.IOException;

public interface GitInitCommand {
    void init(BuildLogger buildLogger) throws IOException;
}
