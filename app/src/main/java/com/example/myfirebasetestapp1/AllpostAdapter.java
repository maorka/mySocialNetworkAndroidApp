package com.example.myfirebasetestapp1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class AllpostAdapter extends ArrayAdapter<Post> {

    Context context;
    List<Post> objects;
    private DatabaseReference mDatabase;

    public AllpostAdapter(Context context, int resource, int textViewResourceId, List<Post> objects) {
        super(context, resource, textViewResourceId, objects);
        this.context = context;
        this.objects = objects;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater layoutInflater = ((Activity) context).getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.custom_post, parent, false);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvAuthor = view.findViewById(R.id.tvAuthor);
        TextView tvBody = view.findViewById(R.id.tvBody);
        TextView tvTimestamp = view.findViewById(R.id.tvTimestamp);
        ImageView ivPostImage = view.findViewById(R.id.ivPostImage);
        ImageButton btnPostOptions = view.findViewById(R.id.btnPostOptions);
        ImageButton btnLike = view.findViewById(R.id.btnLike);
        ImageButton btnFavorite = view.findViewById(R.id.btnFavorite);
        TextView tvLikesCount = view.findViewById(R.id.tvLikesCount);

        LinearLayout commentsSection = view.findViewById(R.id.commentsSection);
        LinearLayout llCommentsContainer = view.findViewById(R.id.llCommentsContainer);
        TextView tvLoginToComment = view.findViewById(R.id.tvLoginToComment);
        LinearLayout addCommentLayout = view.findViewById(R.id.addCommentLayout);

        Post temp = objects.get(position);

        tvTitle.setText(temp.title);
        tvAuthor.setText("by " + temp.authorFirstName);
        tvLikesCount.setText(String.valueOf(temp.likes));

        if (temp.getTimestamp() instanceof Long) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            tvTimestamp.setText(sdf.format(new Date((Long) temp.getTimestamp())));
        }

        if (temp.getPostImage() != null) {
            byte[] imageBytes = Base64.decode(temp.getPostImage(), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            ivPostImage.setImageBitmap(bitmap);
            ivPostImage.setVisibility(View.VISIBLE);
        } else {
            ivPostImage.setVisibility(View.GONE);
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (temp.isExpanded) {
            tvBody.setText(temp.body);
            tvBody.setVisibility(View.VISIBLE);
            commentsSection.setVisibility(View.VISIBLE);
            setupComments(llCommentsContainer, temp.key, currentUser, tvLoginToComment, addCommentLayout);
        } else {
            tvBody.setVisibility(View.GONE);
            commentsSection.setVisibility(View.GONE);
        }

        tvTitle.setOnClickListener(v -> {
            temp.isExpanded = !temp.isExpanded;
            notifyDataSetChanged();
        });

        if (currentUser != null && temp.uid.equals(currentUser.getUid())) {
            btnPostOptions.setVisibility(View.VISIBLE);
            btnPostOptions.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(context, btnPostOptions);
                popup.getMenuInflater().inflate(R.menu.post_options_menu, popup.getMenu());
                
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.menu_edit) {
                        Intent intent = new Intent(context, EditPostActivity.class);
                        intent.putExtra("POST_KEY", temp.key);
                        context.startActivity(intent);
                        return true;
                    } else if (item.getItemId() == R.id.menu_delete) {
                        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("Posts").child(temp.key);
                        postRef.removeValue().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "Failed to delete post", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        } else {
            btnPostOptions.setVisibility(View.GONE);
        }

        if (currentUser != null) {
            final String userId = currentUser.getUid();
            final DatabaseReference postRef = mDatabase.child("Posts").child(temp.key);

            if (temp.likers != null && temp.likers.containsKey(userId)) {
                btnLike.setImageResource(R.drawable.ic_heart_filled);
            } else {
                btnLike.setImageResource(R.drawable.ic_heart_outline);
            }

            if (temp.favoriters != null && temp.favoriters.containsKey(userId)) {
                btnFavorite.setImageResource(R.drawable.ic_star_filled);
            } else {
                btnFavorite.setImageResource(R.drawable.ic_star_outline);
            }

            btnLike.setOnClickListener(v -> postRef.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                    Post p = mutableData.getValue(Post.class);
                    if (p == null) return Transaction.success(mutableData);
                    if (p.likers == null) p.likers = new HashMap<>();

                    if (p.likers.containsKey(userId)) {
                        p.likes--;
                        p.likers.remove(userId);
                        Log.d("Likes", "Like removed from post: " + p.title);
                    } else {
                        p.likes++;
                        p.likers.put(userId, true);
                        Log.d("Likes", "Like added to post: " + p.title);
                    }
                    mutableData.setValue(p);
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                    if (error != null) {
                        Log.e("TAG", "likeTransaction:onComplete:", error.toException());
                    }
                }
            }));

            btnFavorite.setOnClickListener(v -> postRef.runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                    Post p = mutableData.getValue(Post.class);
                    if (p == null) return Transaction.success(mutableData);
                    if (p.favoriters == null) p.favoriters = new HashMap<>();

                    if (p.favoriters.containsKey(userId)) {
                        p.favoriters.remove(userId);
                        Log.d("Favorites", "Post removed from favorites: " + p.title);
                    } else {
                        p.favoriters.put(userId, true);
                        Log.d("Favorites", "Post added to favorites: " + p.title);
                    }
                    mutableData.setValue(p);//create favorite field in database FB
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                    if (error != null) {
                        Log.e("TAG", "favoriteTransaction:onComplete:", error.toException());
                    }
                }
            }));
        } else {
            btnFavorite.setVisibility(View.GONE);
            btnLike.setVisibility(View.GONE);
        }

        return view;
    }

    private void setupComments(LinearLayout llCommentsContainer, String postKey, @Nullable FirebaseUser currentUser, TextView tvLoginToComment, LinearLayout addCommentLayout) {
        DatabaseReference commentsRef = FirebaseDatabase.getInstance().getReference("Comments").child(postKey);
        
        commentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                llCommentsContainer.removeAllViews();
                LayoutInflater inflater = LayoutInflater.from(context);
                
                for (DataSnapshot commentSnapshot : snapshot.getChildren()) {
                    Comment comment = commentSnapshot.getValue(Comment.class);
                    if (comment != null) {
                        View commentView = inflater.inflate(R.layout.custom_comment_item, llCommentsContainer, false);
                        
                        TextView tvCommentAuthor = commentView.findViewById(R.id.tvCommentAuthor);
                        TextView tvCommentText = commentView.findViewById(R.id.tvCommentText);
                        TextView tvCommentTimestamp = commentView.findViewById(R.id.tvCommentTimestamp);

                        tvCommentText.setText(comment.getText());
                        if (comment.getTimestamp() > 0) {
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                            tvCommentTimestamp.setText(sdf.format(new Date(comment.getTimestamp())));
                        }

                        // Fetch user's first name
                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(comment.getUid());
                        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                if (userSnapshot.exists()) {
                                    String firstname = userSnapshot.child("firstname").getValue(String.class);
                                    tvCommentAuthor.setText(firstname != null ? firstname : "Unknown User");
                                } else {
                                    tvCommentAuthor.setText("Unknown User");
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });

                        llCommentsContainer.addView(commentView);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Failed to load comments.", Toast.LENGTH_SHORT).show();
            }
        });

        if (currentUser == null) {
            addCommentLayout.setVisibility(View.GONE);
            tvLoginToComment.setVisibility(View.VISIBLE);
        } else {
            addCommentLayout.setVisibility(View.VISIBLE);
            tvLoginToComment.setVisibility(View.GONE);

            EditText etComment = addCommentLayout.findViewById(R.id.etComment);
            Button btnSendComment = addCommentLayout.findViewById(R.id.btnSendComment);

            btnSendComment.setOnClickListener(v -> {
                String commentText = etComment.getText().toString().trim();
                if (commentText.isEmpty()) {
                    Toast.makeText(context, "Cannot send an empty comment.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String commentId = commentsRef.push().getKey();
                Comment newComment = new Comment(currentUser.getUid(), commentText, System.currentTimeMillis());

                if (commentId != null) {
                    commentsRef.child(commentId).setValue(newComment)
                            .addOnSuccessListener(aVoid -> etComment.setText(""))
                            .addOnFailureListener(e -> Toast.makeText(context, "Failed to send comment.", Toast.LENGTH_SHORT).show());
                }
            });
        }
    }
}
