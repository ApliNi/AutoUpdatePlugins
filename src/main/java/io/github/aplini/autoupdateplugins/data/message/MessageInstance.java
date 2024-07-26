package io.github.aplini.autoupdateplugins.data.message;

import lombok.Getter;

@Getter
public class MessageInstance {
    private final String NoPermission = "§cYou don't have permission to use this command.§r";
    private final String NoSuchCommand = "§cNo such command.§r";
    private final String SuccessMessage = "§aSuccess!";
    private final String FailedMessage = "§cSomething went wrong. Please Check console.";
    private final SubCommands commands = new SubCommands();
    private final Update update = new Update();
    @Getter
    public class SubCommands {
        private final Description description = new Description();
        private final Usage usage = new Usage();

        @Getter
        public class Description {
            private final String RELOAD = "§6Reload Config & Message§r";
            private final String UPDATE = "§aStart Plugin Update§r";
            private final String LOG = "§bShow Plugin Log§r";
            private final String STOP = "§cStop Current Update Task§r";
        }

        @Getter
        public class Usage {
            private final String RELOAD = "§a/aup reload§r";
            private final String UPDATE = "§a/aup update§r";
            private final String LOG = "§a/aup log§r";
            private final String STOP = "§a/aup stop§r";
        }
    }
    @Getter
    public class Update {
        private final String checking = "§aChecking for updates...§r";
        private final String succeedGetType = "§aTarget type: {type}§r";
        private final String errStartRepeatedly = "§cUpdater start repeatedly.§r";
        private final String errParsingDirectUrl = "§cError parsing file direct link, skipping this update.§r";
        private final String tempAlreadyLatest = "§a[Cache] File is already the latest version.§r";
        private final String errDownload = "§cError downloading file, skipping this update.§r";
        private final String zipFileCheck = "§a[Zip integrity check] File is not complete, skipping this update.§r";
        private final Github Github = new Github();
        @Getter
        public class Github {
            private final String repoNotFound = "§cRepository '{owner}/{repo}' not found.§r";
        }
    }
}
