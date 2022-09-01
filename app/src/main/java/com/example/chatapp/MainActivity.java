package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
public static final int RC_SIGN_IN=1;
    RecyclerView recyclerView;
    messageAdapter adapter;
    ArrayList<FriendlyMessage> friendlyMessagesArray = new ArrayList<>();
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    ChildEventListener childEventListener;
    FirebaseAuth mfirebaseAuth;
    FirebaseAuth.AuthStateListener authStateListener;
   Button button;
   TextInputEditText editText;
   String username="anyonmous";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        firebaseDatabase = FirebaseDatabase.getInstance();
        mfirebaseAuth =FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("message");
        button=findViewById(R.id.send_btn);
        editText=findViewById(R.id.edittext_message);
        recyclerView = findViewById(R.id.message_list);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        // Send button sends a message and clears the EditText
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                FriendlyMessage friendlyMessage = new FriendlyMessage(editText.getText().toString(), username, null);
                databaseReference.push().setValue(friendlyMessage);

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
   attachDatabaseListener();
}
private  void attachDatabaseListener(){
        if(childEventListener==null) {
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
        }
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out:
                AuthUI.getInstance().signOut(this);
                return  true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
