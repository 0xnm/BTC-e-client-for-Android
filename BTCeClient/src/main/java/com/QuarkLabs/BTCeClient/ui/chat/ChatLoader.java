package com.QuarkLabs.BTCeClient.ui.chat;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.QuarkLabs.BTCeClient.AppPreferences;
import com.QuarkLabs.BTCeClient.BtcEApplication;
import com.QuarkLabs.BTCeClient.PageDownloader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatLoader extends AsyncTaskLoader<List<ChatMessage>> {

    private static final Pattern COLOR_PATTERN = Pattern.compile("#([a-zA-Z0-9]+);");

    @NonNull
    private List<ChatMessage> messages = new ArrayList<>();

    public ChatLoader(Context context) {
        super(context);
    }

    @Override
    public List<ChatMessage> loadInBackground() {
        PageDownloader pageDownloader = new PageDownloader();
        AppPreferences appPreferences = BtcEApplication.get(getContext())
                .getAppPreferences();
        String content = pageDownloader.download(
                appPreferences.getExchangeUrl(), null, appPreferences.getChatLocale());
        if (content == null) {
            messages = Collections.emptyList();
        } else {
            messages = parseToChatMessages(content);
        }
        return messages;
    }

    @NonNull
    private List<ChatMessage> parseToChatMessages(@NonNull String content) {
        Document document = Jsoup.parse(content);
        Element chatsDiv = document.getElementById("nChat");
        if (chatsDiv != null) {
            List<Element> chatElements = chatsDiv.getElementsByClass("chatmessage");
            List<ChatMessage> chatMessages = new ArrayList<>();
            for (Element msgElement : chatElements) {
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setMessageId(msgElement.id());

                List<Element> msgChildrenElements = msgElement.children();
                for (Element element : msgChildrenElements) {
                    if ("a".equals(element.tagName())) {

                        chatMessage.setColor(colorFromStyle(element.attr("style")));
                        chatMessage.setAuthor(element.text());
                    } else if ("span".equals(element.tagName())) {
                        chatMessage.setMessage(element.text());
                    }
                }
                chatMessages.add(chatMessage);
            }
            return chatMessages;
        }
        return Collections.emptyList();
    }

    @ColorInt
    private int colorFromStyle(@Nullable String style) {
        if (style == null) {
            return -1;
        }
        Matcher matcher = COLOR_PATTERN.matcher(style);
        if (matcher.find() && matcher.groupCount() == 1) {
            String color = matcher.group(1);
            if (color.length() == 6) {
                color = "FF" + color;
            }
            return Color.parseColor("#" + color.toUpperCase(Locale.US));
        }
        return -1;
    }

    @Override
    protected void onStartLoading() {

        if (!messages.isEmpty()) {
            deliverResult(messages);
        }

        if (takeContentChanged() || messages.isEmpty()) {
            forceLoad();
        }
    }
}
