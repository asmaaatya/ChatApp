package com.example.chatapp;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.data.model.User;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.nl.smartreply.SmartReply;
import com.google.mlkit.nl.smartreply.SmartReplyGenerator;
import com.google.mlkit.nl.smartreply.SmartReplySuggestion;
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult;
import com.google.mlkit.nl.smartreply.TextMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
public static final int RC_SIGN_IN=1;
    private static final int RC_PHOTO_PICKER =  2;
    RecyclerView recyclerView;
    messageAdapter adapter;
    ArrayList<FriendlyMessage> friendlyMessagesArray;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
     FirebaseStorage firebaseStorage;
     StorageReference storageReference;
    ChildEventListener childEventListener;
    FirebaseAuth mfirebaseAuth;
    FirebaseAuth.AuthStateListener authStateListener;
   Button button;
   ImageButton photo;
   TextInputEditText editText;
   TextView replytext;
   String username="anyonmous";
   List<TextMessage> conversation;
   SmartReplyGenerator smartReplyGenerator;
   String userUID="123456";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        firebaseDatabase = FirebaseDatabase.getInstance();
        mfirebaseAuth =FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("message");
        firebaseStorage=FirebaseStorage.getInstance();
        storageReference=FirebaseStorage.getInstance().getReference().child("chat_image");
        button=findViewById(R.id.send_btn);
        editText=findViewById(R.id.edittext_message);
        replytext=findViewById(R.id.reply_text);
conversation=new ArrayList();
smartReplyGenerator= SmartReply.getClient();
        recyclerView = findViewById(R.id.message_list);
photo=findViewById(R.id.chatImg);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        friendlyMessagesArray = new ArrayList<>();

photo.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
    }
});
        // Send button sends a message and clears the EditText
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FriendlyMessage friendlyMessage = new FriendlyMessage(editText.getText().toString(), username, null);
                databaseReference.push().setValue(friendlyMessage);
                 String message=editText.getText().toString().trim();
                conversation.add(TextMessage.createForRemoteUser(
                        message, System.currentTimeMillis(),userUID));
                SmartReplyGenerator smartReply = SmartReply.getClient();
                smartReply.suggestReplies(conversation).addOnSuccessListener(new OnSuccessListener<SmartReplySuggestionResult>() {
                    @Override
                    public void onSuccess(SmartReplySuggestionResult smartReplySuggestionResult) {
                        if (smartReplySuggestionResult.getStatus() == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
                            // The conversation's language isn't supported, so
                            // the result doesn't contain any suggestions.
                        } else if (smartReplySuggestionResult.getStatus() == SmartReplySuggestionResult.STATUS_SUCCESS) {
                            // Task completed successfully
                            for (SmartReplySuggestion suggestion : smartReplySuggestionResult.getSuggestions()) {
                                String myreplyText = suggestion.getText();
                                replytext.setText(myreplyText);
                            }
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
           replytext.setText("error"+e.getMessage());
                    }
                });


                // Clear input box
                editText.setText("");
            }

        });

authStateListener=new FirebaseAuth.AuthStateListener() {
    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        FirebaseUser user=firebaseAuth.getCurrentUser();
        if (user != null) {
            //signed in
            OnsignedInIntitialized(user.getDisplayName());
        }else{
            //signed out
            OnSignedOutClear();
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build()
                   );
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    RC_SIGN_IN
            );


        }
    }
};
    }


    @Override
    protected void onPause() {
        super.onPause();
        if(authStateListener!=null){
        mfirebaseAuth.removeAuthStateListener(authStateListener);}
        detatchReadLIstener();
        friendlyMessagesArray.clear();
            }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Sign-in succeeded, set up the UI
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }else if(requestCode==RC_PHOTO_PICKER && resultCode== RESULT_OK){

                Uri selectedImageUri=data.getData();
                final StorageReference photoReference = storageReference.child(selectedImageUri.getLastPathSegment());
                photoReference.putFile(selectedImageUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return photoReference.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            FriendlyMessage friendlyMessage = new FriendlyMessage(null, username, downloadUri.toString());
                            databaseReference.push().setValue(friendlyMessage);
                        } else {
                            Toast.makeText(MainActivity.this, "upload failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mfirebaseAuth.addAuthStateListener(authStateListener);
    }
private  void  OnsignedInIntitialized(String musername){
        username=musername;
        if(childEventListener==null){
   attachDatabaseListener();}
}

private  void attachDatabaseListener(){

            childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    FriendlyMessage friendlyMessage = snapshot.getValue(FriendlyMessage.class);
                    friendlyMessagesArray.add(friendlyMessage);
                    ;
                    adapter = new messageAdapter(MainActivity.this, friendlyMessagesArray);
                    recyclerView.setAdapter(adapter);
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }

            };

    databaseReference.addChildEventListener(childEventListener);
}
private  void OnSignedOutClear(){
username="anynmous";
friendlyMessagesArray.clear();
}
private  void detatchReadLIstener(){
        if(childEventListener!=null){
    databaseReference.removeEventListener(childEventListener);
        childEventListener=null;}
            }
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.topmenu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_outt:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
