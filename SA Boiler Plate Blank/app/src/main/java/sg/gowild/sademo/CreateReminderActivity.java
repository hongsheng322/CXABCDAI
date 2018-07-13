package sg.gowild.sademo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class CreateReminderActivity extends AppCompatActivity {

    private Button cancel;
    private Button create;
    private Button hours;
    private Button mins;
    TextView display;
    CalendarView cv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_createreminder);

        Setupviews();

    }

    private void Setupviews() {

        cancel = findViewById(R.id.cancel);
        create = findViewById(R.id.create);
        hours = findViewById(R.id.hour);
        mins = findViewById(R.id.mins);
        display = findViewById(R.id.dislay);
        cv = (CalendarView) findViewById(R.id.calendar);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CreateReminderActivity.this, ReminderActivity.class));
            }
        });

        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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

                date1.setTime(longtime);
                String finaldate = new SimpleDateFormat("E, MMM d, yyyy, hh:mm aa").format(date1);
                display.setText(finaldate);
            }
        });
    }
}
