package sg.gowild.sademo;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

//@IgnoreExtraProperties
public class Usera {

    public String Key;
    public String Name;
    public String State;
    public Map DateLog;
    public List<Log> ReminderLogList;
    public LinkedList<SymptomsLog> SymptomsLogList;

    public Usera() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)

        ReminderLogList = new Vector<Log>();
        SymptomsLogList = new LinkedList<>();
    }

    public Usera(String key, String username, String state) {
        this.Key = key;
        this.Name = username;
        this.State = state;
        ReminderLogList = new Vector<Log>();
        SymptomsLogList = new LinkedList<>();
    }

    public void SetNewUsera(DataSnapshot userInfo)
    {
        this.Name = (String) userInfo.child("Name").getValue();
        this.State = (String) userInfo.child("State").getValue();
        for (DataSnapshot newLog : userInfo.child("Reminder Log").getChildren())
        {
            this.ReminderLogList.add(newLog.getValue(Log.class));
        }
//        for (DataSnapshot newLog : userInfo.child("Symptoms Log").getChildren())
//        {
//            this.SymptomsLogList.add(newLog.getValue(SymptomsLog.class));
//        }
        //this.ReminderLogList = (HashMap<String, Log>)userInfo.child("Reminder Log").getValue();
    }

    public void AddReminderLog(String newInfo)
    {
        Log temp = new Log();
        temp.Information = newInfo;
        this.ReminderLogList.add(temp);
    }

    public void LogSymptoms(DatabaseReference databaseRef, List<String> symptoms, List<String> condition)
    {
        SymptomsLog log = new SymptomsLog(symptoms,condition);
        this.SymptomsLogList.add(log);
        Map<String,Object> dataValues = log.toMap();

        Map<String,Object> childUpdates = new HashMap<>();
        childUpdates.put("/"+Key +"/Symptoms Log", dataValues);
        databaseRef.updateChildren(childUpdates);
    }

}