package io.github.aplini.autoupdateplugins.beans.Github;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.util.List;

@Getter
public class GithubAPI {
    @SerializedName("tag_name")
    private String tagName;
    private boolean prerelease;
    private List<GithubAsset> assets;
}
