package sg.gowild.sademo;

import android.content.Intent;
import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PatientLogActivity extends AppCompatActivity {


    private FirebaseDatabase database;
    private DatabaseReference databaseRef;
    private ChildEventListener childEventListener;

    LinkedList<String> symptoms;
    LinkedList<String> conditions;
    Usera currUser = new Usera();
    int UserCount;

    List<Date> list_reminder;
    List<String> list_reminder_info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patientlog);

        //Add button logic
        Setupviews();

        //hardcode values
        symptoms = new LinkedList<>();
        conditions = new LinkedList<>();

        symptoms.add("Cough");
        conditions.add("Sick");

        database = FirebaseDatabase.getInstance();
        databaseRef = database.getReference().child("users");

        //firebase
        Query UsersQuery = databaseRef.orderByKey();
        UsersQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Usera> UserList = new ArrayList<Usera>();
                UserCount = 0;
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren())
                {
                    Usera newUser = new Usera();
                    newUser.SetNewUsera(postSnapshot);
                    newUser.Key = String.valueOf(UserCount+1);
                    UserList.add(newUser);
                    UserCount++;

                        //SortReminder();
                        //t.start();
                }
                currUser = UserList.get(UserList.size()-1);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

//        //Setup Database
//        database = FirebaseDatabase.getInstance();
//        databaseRef = database.getReference().child("users");
//        childEventListener = new ChildEventListener() {
//            @Override
//            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//                System.out.println("ChildAdded");
//            }
//
//            @Override
//            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//                System.out.println("ChildChanged");
//            }
//
//            @Override
//            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
//                System.out.println("ChildRemoved");
//
//            }
//
//            @Override
//            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//                System.out.println("ChildMoved");
//
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                System.out.println("ChildCanceled");
//
//            }
//        };

    }

//    private void LogSymptoms(String key, List<String> symptoms, List<String> conditions)
//    {
//        SymptomsLog log = new SymptomsLog(symptoms,conditions);
//        Map<String,Object> dataValues = log.toMap();
//
//        Map<String,Object> childUpdates = new HashMap<>();
//
////        Usera newUser = new Usera(user,"healthy");
////        newUser.AddReminderLog("Please Take XYZ medicine");
////        childUpdates.put("Name", newUser.Name);
////        childUpdates.put("State", newUser.State);
////        childUpdates.put("Reminder Log", newUser.ReminderLogList);
//
//        childUpdates.put("/users/" + key, dataValues);
//
//        databaseRef.updateChildren(childUpdates);
//    }

    private void Setupviews() {
        Button back = findViewById(R.id.backp);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PatientLogActivity.this, MainActivity.class));
            }
        });


        Button logButton = findViewById(R.id.logbutton);
        logButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currUser.LogSymptoms(databaseRef,symptoms,conditions);            }
        });
    }

}

