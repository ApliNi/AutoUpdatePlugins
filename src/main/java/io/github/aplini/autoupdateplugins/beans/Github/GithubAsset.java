package io.github.aplini.autoupdateplugins.beans.Github;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class GithubAsset {
    @SerializedName("browser_download_url")
    private String url;
    private String name;
}
