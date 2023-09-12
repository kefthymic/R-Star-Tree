import java.io.Serializable;
import java.util.ArrayList;

public class Entry implements Serializable {
    private BoundingBox boundingBox;
    private long childId;

    public Entry(BoundingBox boundingBox){
        this.boundingBox = boundingBox;
    }
    public Entry(BoundingBox boundingBox, long childId){
        this.boundingBox = boundingBox;
        this.childId = childId;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
    }

    public long getChildId() {
        return childId;
    }

    public void setChildId(long childId) {
        this.childId = childId;
    }

    /*μέθοδος που δέχεται ένα Arraylist entries και επιστρέφει ένα νέο Arraylist με τα entries ταξινομημένα σε αύξουσα
    * σειρά με βάση το πάνω όριο των BoundingBoxes σε κάποια διάσταση την οποία δέχεται σαν παράμετρο  */
    public static ArrayList<Entry> sortByUpperValues(ArrayList<Entry> entries,int dimension){
        ArrayList<Entry> tempEntries = new ArrayList<>() ;
        for(int i=0;i<entries.size();i++){
            tempEntries.add(entries.get(i));
        }

        ArrayList<Entry> sortedEntries = new ArrayList<>();
        while(tempEntries.size()>0){
            double minValue = tempEntries.get(0).getBoundingBox().getBound(dimension,true);
            int minPos = 0;
            for(int i=0;i<tempEntries.size();i++){
                if(tempEntries.get(i).getBoundingBox().getBound(dimension,true)<minValue){
                    minValue = tempEntries.get(i).getBoundingBox().getBound(dimension,true);
                    minPos = i;
                }
            }
            sortedEntries.add(tempEntries.get(minPos));
            tempEntries.remove(minPos);
        }
        return sortedEntries;
    }
    /*μέθοδος που δέχεται ένα Arraylist entries και επιστρέφει ένα νέο Arraylist με τα entries ταξινομημένα σε αύξουσα
     * σειρά με βάση το κάτω όριο των BoundingBoxes σε κάποια διάσταση την οποία δέχεται σαν παράμετρο  */

    public static ArrayList<Entry> sortByLowerValues(ArrayList<Entry> entries,int dimension){
        ArrayList<Entry> tempEntries = new ArrayList<>() ;
        for(int i=0;i<entries.size();i++){
            tempEntries.add(entries.get(i));
        }

        ArrayList<Entry> sortedEntries = new ArrayList<>();
        while(tempEntries.size()>0){
            double minValue = tempEntries.get(0).getBoundingBox().getBound(dimension,false);
            int minPos = 0;
            for(int i=0;i<tempEntries.size();i++){
                if(tempEntries.get(i).getBoundingBox().getBound(dimension,false)<minValue){
                    minValue = tempEntries.get(i).getBoundingBox().getBound(dimension,false);
                    minPos = i;
                }
            }
            sortedEntries.add(tempEntries.get(minPos));
            tempEntries.remove(minPos);
        }
        return sortedEntries;
    }







    public boolean seeIfTheGivenBoundingIsAllInsideToThisBoundingBox(BoundingBox givenBoundingBox){
        return true;
    }

    public boolean seeIfThisBoundingBoxIsAllInsideToTheGivenBoundingBox(BoundingBox givenBoundingBox){
        return true;
    }

    public boolean seeIfThereIsAtLeastACommonPoint(BoundingBox givenBoundingBox){
        return true;
    }
}
