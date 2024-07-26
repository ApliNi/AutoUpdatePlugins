package io.github.aplini.autoupdateplugins.data.message;

import lombok.Getter;

@Getter
public class MessageInstance {
    private String NoPermission = "§cYou don't have permission to use this command.§r";
    private String NoSuchCommand = "§cNo such command§r";
    private String SuccessMessage = "§aSuccess!";
    private String FailedMessage = "§cSomething went wrong. Please Check console.";
    private SubCommands commands = new SubCommands();
    @Getter
    public class SubCommands {
        private Description description = new Description();
        private Usage usage = new Usage();
        @Getter
        public class Description {
            private String RELOAD = "§6Reload Config & Message§r";
            private String UPDATE = "§aStart Plugin Update§r";
            private String LOG = "§bShow Plugin Log§r";
            private String STOP = "§cStop Current Update Task§r";
        }
        @Getter
        public class Usage {
            private String RELOAD = "§a/aup reload§r";
            private String UPDATE = "§a/aup update§r";
            private String LOG = "§a/aup log§r";
            private String STOP = "§a/aup stop§r";
        }
    }
}
