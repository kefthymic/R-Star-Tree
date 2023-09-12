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


    public RecordId getRecordId(){
        return recordId;
    }
}
