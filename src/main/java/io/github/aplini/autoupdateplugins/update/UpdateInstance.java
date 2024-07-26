package io.github.aplini.autoupdateplugins.update;

import io.github.aplini.autoupdateplugins.AutoUpdate;
import io.github.aplini.autoupdateplugins.bean.UpdateItem;
import io.github.aplini.autoupdateplugins.data.message.MessageManager;
import lombok.Getter;
import okhttp3.OkHttpClient;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Getter
public class UpdateInstance {
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final _scheduleTask task;

    public UpdateInstance(int delay, int interval, OkHttpClient client, List<UpdateItem> items, AutoUpdate plugin, int poolSize) {
        task = new _scheduleTask(items, client, plugin, poolSize);
        scheduledExecutorService.scheduleAtFixedRate(task, delay, interval, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void stop() {
        task.stop();
        scheduledExecutorService.shutdownNow();
    }
    private record _scheduleTask(List<UpdateItem> items, OkHttpClient client, AutoUpdate plugin, int poolSize) implements Runnable {
        static ExecutorService executor;

        private _scheduleTask(List<UpdateItem> items, OkHttpClient client, AutoUpdate plugin, int poolSize) {
            this.items = items;
            this.client = client;
            this.plugin = plugin;
            this.poolSize = poolSize;
            executor = Executors.newFixedThreadPool(poolSize);
        }

        @Override
        public void run() {
            for (UpdateItem item: items)
                executor.submit(new _updateTask(item, client.newBuilder().build(), plugin.messageManager));
        }
        private void stop() {
            executor.shutdownNow();
        }
        private record  _updateTask(UpdateItem item, OkHttpClient client, MessageManager messageManager) implements Runnable {
            @Override
            public void run() {

            }
        }
    }
}
