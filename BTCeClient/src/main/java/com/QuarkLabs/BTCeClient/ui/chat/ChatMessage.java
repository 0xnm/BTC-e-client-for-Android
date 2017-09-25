package com.QuarkLabs.BTCeClient.ui.chat;

import android.support.annotation.ColorInt;

public class ChatMessage {

    private String author = "";
    private String message = "";
    private String messageId = "";
    @ColorInt
    private int color = -1;

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @ColorInt
    public int getColor() {
        return color;
    }

    public void setColor(@ColorInt int color) {
        this.color = color;
    }
}
