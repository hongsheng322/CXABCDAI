package sg.gowild.sademo;

import java.util.Date;

public class Log {

    public String DateTime_s;
    public Date DateTime;
    public String Information;
    public Boolean repeat;

    public Log()
    {
        DateTime = new Date();
        DateTime_s = DateTime.toString();
        Information = "No information";
        repeat = false;
    }

    public Log(String newDateTime, String newInformation, Boolean newRepeat)
    {
        DateTime_s = newDateTime;
        DateTime = new Date(DateTime_s);
        Information = newInformation;
        repeat = newRepeat;
    }
}
