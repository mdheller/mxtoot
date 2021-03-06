/*
 * Copyright sablintolya@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ma1uta.mxtoot.matrix.command;

import com.google.gson.Gson;
import com.sys1yagi.mastodon4j.MastodonClient;
import com.sys1yagi.mastodon4j.api.Scope;
import com.sys1yagi.mastodon4j.api.entity.auth.AppRegistration;
import com.sys1yagi.mastodon4j.api.exception.Mastodon4jRequestException;
import com.sys1yagi.mastodon4j.api.method.Apps;
import io.github.ma1uta.matrix.Event;
import io.github.ma1uta.matrix.bot.Context;
import io.github.ma1uta.matrix.bot.command.OwnerCommand;
import io.github.ma1uta.matrix.client.methods.EventMethods;
import io.github.ma1uta.mxtoot.mastodon.MxMastodonClient;
import io.github.ma1uta.mxtoot.matrix.MxTootConfig;
import io.github.ma1uta.mxtoot.matrix.MxTootDao;
import io.github.ma1uta.mxtoot.matrix.MxTootPersistentService;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Register a new toot client.
 */
public class RegisterMastodonClient extends OwnerCommand<MxTootConfig, MxTootDao, MxTootPersistentService<MxTootDao>, MxMastodonClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterMastodonClient.class);

    @Override
    public String name() {
        return "reg";
    }

    @Override
    public boolean ownerInvoke(Context<MxTootConfig, MxTootDao, MxTootPersistentService<MxTootDao>, MxMastodonClient> holder,
                               String roomId, Event event, String arguments) {
        MxTootConfig config = holder.getConfig();
        EventMethods eventMethods = holder.getMatrixClient().event();

        if (arguments == null || arguments.trim().isEmpty()) {
            eventMethods.sendNotice(roomId, "Usage: " + usage());
            return true;
        }

        config.setMastodonServer(arguments);

        Apps apps = new Apps(new MastodonClient.Builder(config.getMastodonServer(), new OkHttpClient.Builder(), new Gson()).build());

        try {
            AppRegistration appRegistration = apps
                .createApp(config.getMastodonClient(), "urn:ietf:wg:oauth:2.0:oob", new Scope(Scope.Name.ALL)).execute();

            config.setMastodonClientId(appRegistration.getClientId());
            config.setMastodonClientSecret(appRegistration.getClientSecret());

            String authUrl = apps.getOAuthUrl(appRegistration.getClientId(), new Scope(Scope.Name.ALL), "urn:ietf:wg:oauth:2.0:oob");

            eventMethods.sendNotice(roomId, authUrl.replaceAll("\\s", "+"));
            eventMethods.sendNotice(roomId, "Please open url, login, get auth code and invoke command: !auth <auth code>");
        } catch (Mastodon4jRequestException e) {
            String msg = "Cannot start registration of the mastodon client: ";
            LOGGER.error(msg, e);
            eventMethods.sendNotice(roomId, msg + e.getMessage());
        }
        return true;
    }

    @Override
    public String help() {
        return "start registration of the new mastodon client (can invoke only owner).";
    }

    @Override
    public String usage() {
        return "reg <mastodon server>";
    }
}
