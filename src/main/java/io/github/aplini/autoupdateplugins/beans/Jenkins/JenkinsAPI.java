package io.github.aplini.autoupdateplugins.beans.Jenkins;

import lombok.Getter;

import java.util.List;

@Getter
public class JenkinsAPI {
    private List<JenkinsArtifact> artifacts;
}
