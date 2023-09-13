import java.util.ArrayList;

public class EntryOfLeaf extends Entry{

    private RecordId recordId;


    /*Επειδή οι αλγόριθμοι για το split χρησιμοποιούν τετράγωνα κι όχι σημεία και τα entries στα φύλλα θα
    * έχουν ένα bounding box, απλώς οι συντεταγμένες των ορίων θα συμπίπτουν
    * δλδ το bounding box ενός leaf entry θα έχει bounds ένα Arraylist της μορφής (x1,x1,y1,y1,...) */
    public EntryOfLeaf(RecordId recordId, ArrayList<Double> bounds, int dimensions){
        super(new BoundingBox(bounds,dimensions));
        this.recordId = recordId;
    }

    public EntryOfLeaf(RecordId recordId, BoundingBox boundingBox, long childId){
        super(boundingBox, childId);
        this.recordId= recordId;
    }

    /** Μέθοδος που επιστρέφει true αν η EntryOfLeaf κυριαρχεί επί μιας άλλης EntryOfLeaf που δέχεται σαν παράμετρο (gia skyline) */
    public boolean dominatesAnotherEntryOfLeaf(EntryOfLeaf anotherEntryOfLeaf){
        ArrayList<Double> coordinates = new ArrayList<>();
        ArrayList<Double> anotherEntryOfLeafCoordinates = new ArrayList<>();
        for(int i=0;i<this.getBoundingBox().getDimensions();i++){
            coordinates.add(this.getBoundingBox().getBound(i,false));
            anotherEntryOfLeafCoordinates.add(anotherEntryOfLeaf.getBoundingBox().getBound(i,false));
        }
        boolean entryOfLeafIsBetterThanAnotherEntryOfLeafInAtLeastOneAttribute = false;

        for(int i=0;i<coordinates.size();i++){
            if(anotherEntryOfLeafCoordinates.get(i)<coordinates.get(i)){
                return false;
            }
            if(coordinates.get(i)<anotherEntryOfLeafCoordinates.get(i)){
                entryOfLeafIsBetterThanAnotherEntryOfLeafInAtLeastOneAttribute = true;
            }
        }
        if(entryOfLeafIsBetterThanAnotherEntryOfLeafInAtLeastOneAttribute){
            return true;
        }
        else {
            return false;
        }
    }
    /** Μέθοδος που επιστρέφει true αν η EntryOfLeaf κυριαρχεί επί μιας άλλης Entry που δέχεται σαν παράμετρο (gia skyline) */
    public boolean dominatesAnotherEntry(Entry anotherEntry){
        ArrayList<Double> coordinates = new ArrayList<>();
        ArrayList<Double> anotherEntryCoordinates = new ArrayList<>();
        for(int i=0;i<this.getBoundingBox().getDimensions();i++){
            coordinates.add(this.getBoundingBox().getBound(i,false));
            anotherEntryCoordinates.add(anotherEntry.getBoundingBox().getBound(i,false));
        }
        boolean entryOfLeafIsBetterThanAnotherEntryOfLeafInAtLeastOneAttribute = false;

        for(int i=0;i<coordinates.size();i++){
            if(anotherEntryCoordinates.get(i)<coordinates.get(i)){
                return false;
            }
            if(coordinates.get(i)<anotherEntryCoordinates.get(i)){
                entryOfLeafIsBetterThanAnotherEntryOfLeafInAtLeastOneAttribute = true;
            }
        }
        if(entryOfLeafIsBetterThanAnotherEntryOfLeafInAtLeastOneAttribute){
            return true;
        }
        else {
            return false;
        }
    }
    public RecordId getRecordId(){
        return recordId;
    }
}
