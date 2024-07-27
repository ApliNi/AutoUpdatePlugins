package io.github.aplini.autoupdateplugins.beans;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateItem {
    private String file = "";
    private String url = "";
    private String tempPath;
    private String updatePath;
    private String filePath;
    private String path;
    private String get;
    private String fileNamePattern;
    private boolean getPreRelease = false;
    private boolean zipFileCheck = true;
    private boolean ignoreDuplicates = true;
}
