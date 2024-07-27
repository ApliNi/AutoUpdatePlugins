package io.github.aplini.autoupdateplugins.data.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageInstance {
    private String NoPermission = "You don't have permission to use this command.";
    private String NoSuchCommand = "No such command.";
    private String SuccessMessage = "Success!";
    private String FailedMessage = "Something went wrong. Please Check console.";
    private String ReloadMessage = "Stopping Update Schedule & Reloading Data!";
    private SubCommands commands = new SubCommands();
    private Update update = new Update();
    @Getter
    @Setter
    public static class SubCommands {
        private Description description = new Description();
        private Usage usage = new Usage();

        @Getter
        @Setter
        public static class Description {
            private String RELOAD = "Reload Config & Message";
            private String UPDATE = "Start Plugin Update";
            private String LOG = "Show Plugin Log";
            private String STOP = "Stop Current Update Task";
        }

        @Getter
        @Setter
        public static class Usage {
            private String RELOAD = "/aup reload";
            private String UPDATE = "/aup update";
            private String LOG = "/aup log";
            private String STOP = "/aup stop";
        }
    }
    @Getter
    @Setter
    public static class Update {
        private String checking = "Checking for updates...";
        private String succeedGetType = "Target type: {type}";
        private String errStartRepeatedly = "Updater start repeatedly.";
        private String errParsingDirectUrl = "Error parsing file direct link, skipping this update.";
        private String tempAlreadyLatest = "[Cache] File is already the latest version.";
        private String errDownload = "Error downloading file, skipping this update.";
        private String zipFileCheck = "[Zip integrity check] File is not complete, skipping this update.";
        private String findDownloadUrl = "Find download url: {url}";
        private String noFileMatching = "No file matching.";
        private String resourceNotFound = "Resource not found.";
        private String listConfigErrMissing = "Update list configuration error or Missing basic configuration";
        private String fileSizeDifference = "File size difference: Old - {old}bytes, New - {new}bytes.";
        private Github Github = new Github();
        @Getter
        @Setter
        public static class Github {
            private String repoNotFound = "Repository '{owner}/{repo}' not found.";
        }
    }
}
