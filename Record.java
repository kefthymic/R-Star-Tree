import java.util.ArrayList;

public class Record {
    private long osmNodeId;
    private ArrayList<Double> coordinates;
    private String name;

    public Record(long nodeId, ArrayList<Double> coordinates, String name){
        this.osmNodeId= nodeId;
        this.coordinates= coordinates;
        this.name= name;
    }

    public long getOsmNodeId() {
        return this.osmNodeId;
    }

    public ArrayList<Double> getCoordinates() {
        return this.coordinates;
    }

    public String getName() {
        return this.name;
    }

    /** Μέθοδος που επιστρέφει true αν η Record κυριαρχεί επί μιας άλλης Record που δέχεται σαν παράμετρο */
    public boolean dominatesAnotherRecord(Record anotherRecord){
        ArrayList<Double> anotherRecordCoordinates = anotherRecord.getCoordinates();
        boolean recordIsBetterThanAnotherRecordInAtLeastOneAttribute = false;

        for(int i=0;i<this.coordinates.size();i++){
            if(anotherRecordCoordinates.get(i)<this.coordinates.get(i)){
                return false;
            }
            if(this.coordinates.get(i)<anotherRecordCoordinates.get(i)){
                recordIsBetterThanAnotherRecordInAtLeastOneAttribute = true;
            }
        }
        if(recordIsBetterThanAnotherRecordInAtLeastOneAttribute){
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        String data="";

        data+="osmNode id: "+osmNodeId+"\n";

        data+="coordinates: ";
        data+=coordinates.get(0);
        for(int i=1;i<coordinates.size();i++){
            data+=" , "+coordinates.get(i);
        }
        data+="\n";

        if(name!=null){
            data+="name: "+name+"\n";
        }

        return data;
    }
}
