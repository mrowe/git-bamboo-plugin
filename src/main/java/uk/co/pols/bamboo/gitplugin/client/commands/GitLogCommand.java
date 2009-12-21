package uk.co.pols.bamboo.gitplugin.client.commands;

import com.atlassian.bamboo.commit.Commit;

import java.util.List;
import java.io.IOException;

public interface GitLogCommand {
    List<Commit> extractCommits() throws IOException;

    String getLastRevisionChecked();

    String getHeadRevision(String branch) throws IOException;
}
