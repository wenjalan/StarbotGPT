package wenjalan.starbot.gpt;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;

public class EventListener extends ListenerAdapter {

    public static final long WENTON_ID = 478706068223164416L;

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        // if the message wasn't from me, return
        if (event.getAuthor().getIdLong() != WENTON_ID) return;

        // if the message started with !gpt
        TextChannel channel = event.getChannel();
        Message msg = event.getMessage();
        String content = msg.getContentRaw();
        if (content.startsWith("!gpt")) {
            // if the arg was download
            String query = content.substring("!gpt".length()).trim();
            if (query.startsWith("download")) {
                // send that we're starting the download
                channel.sendMessage("Stealing all your data...").queue();
                StarbotGPT.downloadChannel(channel);
            }
        }
    }

}
