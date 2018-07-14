package sg.gowild.sademo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.*;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReminderActivity extends AppCompatActivity {

    private Button back;
    private Button set;
    //private ListView reminderlist;
    private  SwipeListView reminderlist;
    private ListAdapter listAdapter;
    private ArrayList<Info> listData = new ArrayList<Info>();

    List list = new ArrayList();
    ArrayAdapter adapter;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference databaseRef = database.getReference();
    List<Date> list_reminder;
    List<String> list_reminder_info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);

        list_reminder = new ArrayList<Date>();
        list_reminder_info = new ArrayList<String>();

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

                    //add the reminder to the list
                    if (newUser.Name.equalsIgnoreCase("Patient1"))
                    {
                        for (sg.gowild.sademo.Log tempLog : newUser.ReminderLogList)
                        {
                            Info info = new Info();
                            info.name = tempLog.DateTime;
                            info.desc = tempLog.Information;
                            listData.add(info);

                            list_reminder.add(tempLog.DateTime);
                            list_reminder_info.add(tempLog.Information);
                        }
                        SortReminder();
                    }
                }
                for (Usera temp : UserList) {
                    android.util.Log.v("User", temp.Name + " : " + temp.State);
                    //String note = ()
                    android.util.Log.v("Log", "" + temp.ReminderLogList.get(0).DateTime_s);
                }

                Updatetable();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        Setupviews();
    }

    private void Setupviews() {

        back = findViewById(R.id.back);
        set = findViewById(R.id.set);
        reminderlist = (SwipeListView)findViewById(R.id.reminderlist);

        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ReminderActivity.this, CreateReminderActivity.class));
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ReminderActivity.this, MainActivity.class));
            }
        });

        reminderlist.setListener(new OnSwipeListItemClickListener() {
            public void OnClick(View view, int index) {
                AlertDialog.Builder ab = new AlertDialog.Builder(ReminderActivity.this);
                ab.setTitle(list_reminder_info.get(index));
                ab.setMessage(list_reminder.get(index).toString());
                ab.create().show();
            }

            @Override
            public boolean OnLongClick(View view, int index) {
                return false;
            }

            @Override
            public void OnControlClick(int rid, View view, int index) {
                AlertDialog.Builder ab;
                switch (rid){
                    /*case R.id.modify:
                        ab = new AlertDialog.Builder(ReminderActivity.this);
                        ab.setTitle("Modify");
                        ab.setMessage("You will modify item "+index);
                        ab.create().show();
                        break;*/
                    case R.id.delete:
                        /*ab = new AlertDialog.Builder(ReminderActivity.this);
                        ab.setTitle("Deleted");
                        ab.setMessage("You will delete item "+index);
                        ab.create().show();*/
                        list_reminder.remove(index);
                        list_reminder_info.remove(index);
                        listAdapter.removeitem(index);
                        break;
                }
            }
        }, new int[]{R.id.delete});

        adapter = new ArrayAdapter(ReminderActivity.this, android.R.layout.simple_list_item_1, list);
        listAdapter = new ListAdapter(listData);
        reminderlist.setAdapter(listAdapter);
    }

    void Updatetable()
    {
        for(int i = 0; i < list_reminder.size(); i++)
        {
            list.add(list_reminder_info.get(i) + " \n " + new SimpleDateFormat("E, MMM d, yyyy, hh:mm aa").format(list_reminder.get(i)));
        }

        adapter = new ArrayAdapter(this, R.layout.list_item, R.id.item, list);
        listAdapter = new ListAdapter(listData);
        reminderlist.setAdapter(listAdapter);
    }

    private void  SortReminder()
    {
        int n = list_reminder.size();
        for (int i = 0; i < n-1; i++) {
            for (int j = 0; j < n - i - 1; j++){
                if (list_reminder.get(j).after(list_reminder.get(j + 1))) {
                    // swap temp and arr[i]
                    Date temp = list_reminder.get(j);
                    String temp_info = list_reminder_info.get(j);
                    list_reminder.set(j, list_reminder.get(j + 1));
                    list_reminder.set(j + 1, temp);

                    list_reminder_info.set(j, list_reminder_info.get(j + 1));
                    list_reminder_info.set(j + 1, temp_info);
                }
            }
        }
        for (Date tempDate : list_reminder)
        {
            android.util.Log.d("date",tempDate.toString());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    class Info{
        public Date name= new Date();
        public String desc="";
    }
    class ViewHolder{
        public TextView date;
        public TextView desc;
        public Button modify;
        public Button delete;
    }
    class ListAdapter extends SwipeListAdapter {
        private ArrayList<Info> listData;
        public ListAdapter(ArrayList<Info> listData){
            this.listData= (ArrayList<Info>) listData.clone();
        }
        @Override
        public int getCount() {
            return listData.size();
        }

        @Override
        public Object getItem(int position) {
            return listData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void removeitem(int id) { this.listData.remove(getItem(id)); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = new ViewHolder();
            if(convertView == null){
                convertView = View.inflate(getBaseContext(),R.layout.styles_list,null);
                viewHolder.date = (TextView) convertView.findViewById(R.id.date);
                viewHolder.desc = (TextView) convertView.findViewById(R.id.desc);
                //viewHolder.modify = (Button) convertView.findViewById(R.id.modify);
                viewHolder.delete = (Button) convertView.findViewById(R.id.delete);
                convertView.setTag(viewHolder);
            }
            else{
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.date.setText(listData.get(position).name.toString());
            viewHolder.desc.setText(listData.get(position).desc);
            return super.bindView(position, convertView);
        }
    }
}
