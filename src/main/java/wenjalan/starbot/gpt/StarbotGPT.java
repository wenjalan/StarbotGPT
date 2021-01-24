package wenjalan.starbot.gpt;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class StarbotGPT {

    // directory to save channel history + annotations to
    public static final String SAVE_DIRECTORY = "./channels/";

    // the threshold (in seconds) to be considered a block
    public static final int BLOCK_THRESHOLD = 30;

    public static void main(String[] args) throws LoginException {
        System.out.println("Enter bot token:");
        String token = new Scanner(System.in).nextLine();
        new StarbotGPT(token);
    }

    public StarbotGPT(String token) throws LoginException {
        JDA jda = JDABuilder.createDefault(token)
                // add event listeners here
                .addEventListeners(new EventListener())
                .build();
        System.out.println("StarbotGPT Online!");
        System.out.println("Invite: " + jda.getInviteUrl());
    }

    // downloads a TextChannel to the disk, with annotations
    public static void downloadChannel(TextChannel channel) {
        // print
        System.out.println("Reading messages from channel id " + channel.getId());

        // get a list of messages representing the entire history of the channel
        System.out.println("Loading messages...");
        List<Message> history = new LinkedList<>();
        channel.getIterableHistory().forEach(msg -> {
            history.add(0, msg);
            if (history.size() % 1000 == 0) {
                System.out.println("Loaded " + history.size() + " messages so far...");
            }
        });
        System.out.println("Finished loading " + history.size() + " messages, beginning annotation...");

        // list of messages
        List<String> messages = new ArrayList<>();
        // total seen
        AtomicLong seen = new AtomicLong();
        // if we're in a block
        AtomicBoolean inBlock = new AtomicBoolean(false);
        // the OffsetDateTime of the last message
        AtomicReference<Message> lastMsg = new AtomicReference<>();

        // start reading the entire history, from the beginning
        history.forEach((msg) -> {
            // print our progress
            if (seen.get() % 1000 == 0) {
                System.out.println("Annotated " + messages.size() + " messages so far...");
            }

            // run content checks
            if (isValid(msg)) {
                // get message details
                String content = msg.getContentRaw();

                // if this message was written by the same author as the last one,
                // and we haven't set the start block tag
                if (isInBlock(lastMsg.get(), msg)) {
                    // if we're not in a block yet, declare one
                    if (!inBlock.get()) {
                        // set inBlock to true and add a starting tag to the previous message
                        inBlock.set(true);
                        String lastMessage = messages.remove(messages.size() - 1);
                        lastMessage = "<|start-block|>" + lastMessage;
                        messages.add(lastMessage);
                    }
                }
                // if the authors are different (and this isn't the very first message)
                else if (lastMsg.get() != null) {
                    // add an ending tag to the previous message
                    String lastMessage = messages.remove(messages.size() - 1);
                    lastMessage = lastMessage + "<|end-block|>";

                    // if we weren't in a block previously
                    // declare the previous message the start
                    if (!inBlock.get()) {
                        lastMessage = "<|start-block|>" + lastMessage;
                    }
                    messages.add(lastMessage);

                    // declare that we're no longer in a block
                    inBlock.set(false);
                }

                // set last date time
                lastMsg.set(msg);

                // add message to list
                messages.add(content);
            }

            // increment and continue
            seen.getAndIncrement();
        });

        // finish the last msg
        String lastMessage = messages.remove(messages.size() - 1);
        // if we weren't in a block make this the start too
        if (!inBlock.get()) {
            lastMessage = "<|start-block|>" + lastMessage;
        }
        // declare this message the end of the final block
        lastMessage += "<|end-block|>";
        messages.add(lastMessage);

        // sout
        System.out.println("Download complete, found " + messages.size() + " valid messages");
        channel.sendMessage("Downloaded " + messages.size() + " messages, time to sell to China").queue();

        // print to file
        try {
            String filename = SAVE_DIRECTORY + channel.getId() + ".txt";
            System.out.println("Writing to disk...");
            File f = new File(filename);
            if (!f.exists()) f.createNewFile();
            FileWriter writer = new FileWriter(f);
            for (String msg : messages) {
                writer.write(msg);
                writer.write("\n");
            }
            writer.flush();
            System.out.println("Finished! Wrote to " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // returns whether two messages are in a block together
    private static boolean isInBlock(Message lastMsg, Message msg) {
        // lastMsg is null
        if (lastMsg == null) return false;

        // same author
        if (lastMsg.getAuthor().getIdLong() != msg.getAuthor().getIdLong()) return false;

        // within BLOCK_THRESHOLD seconds
        long timeDifference = msg.getTimeCreated().toEpochSecond() - lastMsg.getTimeCreated().toEpochSecond();
        return timeDifference <= BLOCK_THRESHOLD;
    }

    // returns whether this message is valid for modeling
    private static boolean isValid(Message msg) {
        // get content
        String content = msg.getContentRaw().trim();

        // if it's sent by a bot
        if (msg.getAuthor().isBot()) return false;

        // if it's empty
        if (content.isEmpty()) return false;

        // if it's a command
        if (content.startsWith("!") || content.startsWith(".") || content.startsWith("-")) return false;

        // if it's a link
        if (content.startsWith("http")) return false;

        // return that it's valid
        return true;
    }

}
