package sg.gowild.sademo;

import com.google.firebase.database.DataSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

//@IgnoreExtraProperties
public class Usera {

    public String Name;
    public String State;
    public Map DateLog;
    public List<Log> ReminderLogList;

    public Usera() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)

        ReminderLogList = new Vector<Log>();
    }

    public Usera(String username, String state) {
        this.Name = username;
        this.State = state;
        ReminderLogList = new Vector<Log>();
    }

    public void SetNewUsera(DataSnapshot userInfo)
    {
        this.Name = (String) userInfo.child("Name").getValue();
        this.State = (String) userInfo.child("State").getValue();
        for (DataSnapshot newLog : userInfo.child("Reminder Log").getChildren())
        {
            this.ReminderLogList.add(newLog.getValue(Log.class));
        }
        //this.ReminderLogList = (HashMap<String, Log>)userInfo.child("Reminder Log").getValue();
    }

    public void AddReminderLog(String newInfo)
    {
        Log temp = new Log();
        temp.Information = newInfo;
        this.ReminderLogList.add(temp);
    }

}