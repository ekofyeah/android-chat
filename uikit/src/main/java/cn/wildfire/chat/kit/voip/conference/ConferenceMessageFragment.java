/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfirechat.remote.ChatManager;

public class ConferenceMessageFragment extends Fragment {
    @BindView(R2.id.messageRecyclerView)
    RecyclerView messageRecyclerView;

    private MessageViewModel messageViewModel;
    private List<UiMessage> messages = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private Handler handler;

    private final static long TIMEOUT = 30 * 1000;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_conference_message, container, false);
        ButterKnife.bind(this, view);
        messageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);
        init();
        return view;
    }

    private void init() {
        messageViewModel.messageLiveData().observe(getViewLifecycleOwner(), new Observer<UiMessage>() {
            @Override
            public void onChanged(UiMessage uiMessage) {
                messages.add(uiMessage);
                messageAdapter.notifyItemInserted(messages.size() - 1);
            }
        });
        messageAdapter = new MessageAdapter();
        messageRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        messageRecyclerView.setAdapter(messageAdapter);

        handler = new Handler();
        handler.postDelayed(this::checkMessageTimeout, TIMEOUT);
    }

    private void checkMessageTimeout() {
        long now = System.currentTimeMillis();
        int j = -1;
        for (int i = 0; i < messages.size(); i++) {
            if (now - messages.get(i).message.serverTime > TIMEOUT) {
                j = i;
            }
        }
        if (j > -1) {
            if (j + 1 > messages.size()) {
                messages.clear();
            } else {
                messages = messages.subList(j + 1, messages.size());
            }
        }
        messageAdapter.notifyItemRangeRemoved(0, j + 1);

        handler.postDelayed(this::checkMessageTimeout, TIMEOUT);
    }

    @OnClick(R2.id.inputTextView)
    void showMessageInputFragment() {
        ((ConferenceActivity) getActivity()).showKeyboardDialogFragment();
    }

    static abstract class MessageViewHolder extends RecyclerView.ViewHolder {

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        abstract public void onBind(UiMessage message);
    }

    static class TextMessageViewHolder extends MessageViewHolder {
        @BindView(R2.id.senderTextView)
        TextView senderTextView;
        @BindView(R2.id.messageContentTextView)
        TextView messageContentTextView;

        public TextMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        @Override
        public void onBind(UiMessage message) {
            senderTextView.setText(ChatManager.Instance().getUserDisplayName(message.message.sender) + ":");
            messageContentTextView.setText(message.message.digest());
        }
    }

    class MessageAdapter extends RecyclerView.Adapter<MessageViewHolder> {

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View itemView = inflater.inflate(R.layout.av_conference_message_item, parent, false);
            return new TextMessageViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            holder.onBind(messages.get(position));
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }
    }

}