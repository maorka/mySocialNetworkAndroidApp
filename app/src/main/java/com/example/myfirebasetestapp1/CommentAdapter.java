package com.example.myfirebasetestapp1;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends ArrayAdapter<Comment> {

    private Context context;
    private List<Comment> comments;

    public CommentAdapter(@NonNull Context context, int resource, @NonNull List<Comment> objects) {
        super(context, resource, objects);
        this.context = context;
        this.comments = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater layoutInflater = ((Activity) context).getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.custom_comment_item, parent, false);

        TextView tvCommentAuthor = view.findViewById(R.id.tvCommentAuthor);
        TextView tvCommentText = view.findViewById(R.id.tvCommentText);
        TextView tvCommentTimestamp = view.findViewById(R.id.tvCommentTimestamp);

        Comment temp = comments.get(position);
        tvCommentText.setText(temp.getText());

        if (temp.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvCommentTimestamp.setText(sdf.format(new Date(temp.getTimestamp())));
        }

        // Fetch user's first name from UID
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(temp.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String firstname = snapshot.child("firstname").getValue(String.class);
                    tvCommentAuthor.setText(firstname != null ? firstname : "Unknown User");
                } else {
                    tvCommentAuthor.setText("Unknown User");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvCommentAuthor.setText("Unknown User");
            }
        });

        return view;
    }
}
