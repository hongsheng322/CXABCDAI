package sg.gowild.sademo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class CreateReminderActivity extends AppCompatActivity {

    private Button cancel;
    private Button create;
    private Button hours;
    private Button mins;
    TextView display;
    CalendarView cv;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference databaseRef = database.getReference();

    Usera currentuser;
    int usercount;
    int userid;
    int logid;
    EditText info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_createreminder);

        logid = 0;
        usercount = 0;
        userid = 0;

        Query UsersQuery = databaseRef.child("users").orderByKey();
        UsersQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Usera> UserList = new ArrayList<Usera>();
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren())
                {
                    Usera newUser = new Usera();
                    newUser.SetNewUsera(postSnapshot);
                    UserList.add(newUser);
                    usercount++;

                    //add the reminder to the list
                    if (newUser.Name.equalsIgnoreCase("Patient1"))
                    {
                        userid = usercount;

                        for (sg.gowild.sademo.Log tempLog : newUser.ReminderLogList)
                        {
                            logid++;
                        }

                        currentuser = newUser;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        Setupviews();
    }

    private void Setupviews() {

        cancel = findViewById(R.id.cancel);
        create = findViewById(R.id.create);
        hours = findViewById(R.id.hour);
        mins = findViewById(R.id.mins);
        display = findViewById(R.id.dislay);
        display.setText("Sun, Jul 15, 2018, 12 00 am");
        cv = (CalendarView) findViewById(R.id.calendar);
        info = findViewById(R.id.info);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CreateReminderActivity.this, ReminderActivity.class));
            }
        });

        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log log = new Log(display.getText().toString(), info.getText().toString(),false);
                currentuser.AddReminderLog(log.toString());

                HashMap<String, Object> res = new HashMap<>();
                res.put(String.valueOf(logid), log);
                databaseRef.child("users").child(String.valueOf(userid)).child("Reminder Log").push();
                databaseRef.child("users").child(String.valueOf(userid)).child("Reminder Log").updateChildren(res);

                startActivity(new Intent(CreateReminderActivity.this, ReminderActivity.class));
            }
        });

        hours.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popmenu = new PopupMenu(CreateReminderActivity.this, hours);
                popmenu.getMenuInflater().inflate(R.menu.hoursmenu, popmenu.getMenu());

                popmenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        hours.setText(item.getTitle());
                        return true;
                    }
                });

                popmenu.show();
            }
        });

        mins.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popmenu = new PopupMenu(CreateReminderActivity.this, mins);
                popmenu.getMenuInflater().inflate(R.menu.minutesmenu, popmenu.getMenu());

                popmenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        mins.setText(item.getTitle());
                        return true;
                    }
                });

                popmenu.show();
            }
        });


        cv.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {

                String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                Date date1 = new Date();
                long longtime = ((Integer.valueOf(hours.getText().toString()) - 7) * 60 + (Integer.valueOf(mins.getText().toString()) - 30)) * 60000;

                try {
                    date1 = new SimpleDateFormat("dd/MM/yyyy").parse(date);
            } catch (ParseException e) {
                e.printStackTrace();
            };

                String finaldate = new SimpleDateFormat("E, MMM d, yyyy").format(date1);
                date1.setTime(longtime);
                finaldate += " " + new SimpleDateFormat("hh:mm aa").format(date1);
                display.setText(finaldate);
            }
        });
    }
}
