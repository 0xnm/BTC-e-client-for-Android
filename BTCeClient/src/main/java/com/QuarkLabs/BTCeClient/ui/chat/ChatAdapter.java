package com.QuarkLabs.BTCeClient.ui.chat;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.QuarkLabs.BTCeClient.R;

import java.util.ArrayList;
import java.util.List;

class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessage> messages = new ArrayList<>();

    private boolean isLinkify;

    ChatAdapter() {
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return messages.get(position).getMessageId().hashCode();
    }

    @Override
    public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ChatViewHolder(
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_chat, parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.authorView.setText(message.getAuthor() + ":");
        if (message.getColor() != -1) {
            holder.authorView.setTextColor(message.getColor());
        } else {
            holder.authorView.setTextColor(ContextCompat.getColor(holder.authorView.getContext(),
                    R.color.chat_author_default));
        }
        holder.messageView.setText(message.getMessage());
        if (isLinkify) {
            Linkify.addLinks(holder.messageView, Linkify.WEB_URLS);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    void setMessages(@NonNull final List<ChatMessage> messages) {
        final List<ChatMessage> oldMessages = new ArrayList<>(this.messages);
        this.messages = messages;
        DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldMessages.size();
            }

            @Override
            public int getNewListSize() {
                return messages.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return oldMessages.get(oldItemPosition).getMessageId()
                        .equals(messages.get(newItemPosition).getMessageId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return true;
            }
        }).dispatchUpdatesTo(this);

    }

    // fine to set it once, since each time fragment is re-created anyway
    void setLinkify(boolean linkify) {
        isLinkify = linkify;
    }

    static final class ChatViewHolder extends RecyclerView.ViewHolder {

        private TextView authorView;
        private TextView messageView;

        ChatViewHolder(View itemView) {
            super(itemView);

            authorView = (TextView) itemView.findViewById(R.id.chat_author);
            messageView = (TextView) itemView.findViewById(R.id.chat_message);
        }
    }
}
