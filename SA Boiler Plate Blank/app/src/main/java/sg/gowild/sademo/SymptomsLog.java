package sg.gowild.sademo;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymptomsLog{

    public List<String> symptoms;
    public List<String> possibleConditions;
    String date;

    public SymptomsLog()
    {

    }

    public SymptomsLog(List<String> in_symptoms, List<String> in_conditions)
    {
        symptoms = in_symptoms;
        possibleConditions = in_conditions;

        date = new Date().toString();
    }

    public Map<String, Object> toMap(){
        HashMap<String,Object> result = new HashMap<>();
        String symptomString = "";
        for (String symtom:symptoms) {
            symptomString += symtom + " ";
        }
        result.put("Symtoms",symptomString);

        String conditionString = "";
        for (String condition:possibleConditions) {
            conditionString+= condition+ " ";
        }
        result.put("PossibleCondition",conditionString);

        result.put("Date",date);
        return result;
    }
}
