package io.github.aplini.autoupdateplugins.beans;

import lombok.Getter;

@Getter
public class UpdateItem {
    private String file;
    private String url;
    private String tempPath;
    private String updatePath;
    private String filePath;
    private String path;
    private String get;
    private String fileNamePattern;
    private boolean getPreRelease;
    private boolean zipFileCheck;
    private boolean ignoreDuplicates;
}
