package com.example.chatapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class messageAdapter extends RecyclerView.Adapter<messageAdapter.MessageHolder> {
Context context;
ArrayList<FriendlyMessage> friendlyMessages=new ArrayList<>();



    public messageAdapter(Context context, ArrayList<FriendlyMessage> friendlyMessages) {
        this.context = context;
        this.friendlyMessages = friendlyMessages;
    }

    @NonNull
    @Override
    public MessageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v= LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem,null,false);
        return new MessageHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageHolder holder, int position) {
   FriendlyMessage message=friendlyMessages.get(position);
holder.message_sender.setText(message.getName());
holder.message_content.setText(message.getText());
    }

    @Override
    public int getItemCount() {
        return friendlyMessages.size();
    }

    public  class MessageHolder extends RecyclerView.ViewHolder{
TextView message_sender,message_content;
        public MessageHolder(@NonNull View itemView) {
            super(itemView);
            message_sender=itemView.findViewById(R.id.message_sender);
            message_content=itemView.findViewById(R.id.message_content);
        }
    }
}
