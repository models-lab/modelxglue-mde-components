package lucene;

import java.util.ArrayList;

public class Utils {
	public static ArrayList<String> splitString(String inputString){
        ArrayList<String> resultsSplitted = new ArrayList<String>();
        String[] decomposed = inputString.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");

        for (String d : decomposed) {
            d=d.replace("/","");
            d=d.replace(":","");
            d=d.replace("AND","");
            d=d.replace("OR","");
            d=d.replace("NOT","");
            //if(d.length()<=4){continue;}
            if(d.toLowerCase().contains("and")||d.toLowerCase().contains("or")||d.toLowerCase().contains("not")){continue;}
            String[] decomposed1 = null;
            if(d.contains("-")){
                decomposed1 = d.split("-");
            }
            if(d.contains("_")){
                decomposed1 = d.split("_");
            }
            if(decomposed1!=null) {
                //System.out.println(d1);
                //ecoreElements.put(d1, categoryElements);
                for(String d1:decomposed1){
                    if(d1.length()<=2){continue;}
                    resultsSplitted.add(d1); }

            }
            else{
                //System.out.println(d);
                //ecoreElements.put(d, categoryElements);
                resultsSplitted.add(d);
            }
            //resultsSplitted.add(d);

        }
        return resultsSplitted;
    }
}
