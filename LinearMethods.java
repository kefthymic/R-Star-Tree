import java.util.ArrayList;

public class LinearMethods {
    private ReadDataFile readDataFile;

    public LinearMethods(){
        this.readDataFile= new ReadDataFile();
    }


    public ArrayList<RecordId> rangeQuery(BoundingBox queryBoundingBox){
        Record tempRecord;
        BoundingBox tempBoundingBox;
        String tempString;
        int help;
        short tempBlockId;
        int tempSlot;
        ArrayList<RecordId> solution= new ArrayList<>();
        ArrayList<Double> forBounds= new ArrayList<>();
        RecordId tempRecordId;

        int totalRecords= this.readDataFile.getTotalNumberOfRecords();
        int totalCoordinates= this.readDataFile.getCoordinates();

        for(int i=0;i<totalRecords;i++){
            tempRecord= this.readDataFile.getTheData(i);

            for(int j=0;j<totalCoordinates;j++){
                for(int k=0;k<totalCoordinates;k++){
                    forBounds.add(tempRecord.getCoordinates().get(j));
                }
            }

            tempBoundingBox= new BoundingBox(forBounds, totalCoordinates);

            if (tempBoundingBox.seeIfThereIsAtLeastACommonPoint(queryBoundingBox)){
                tempString= this.readDataFile.getTheTargetBlockAndTheSlotOffset(i);
                help= tempString.indexOf("&");

                tempBlockId= (short) Integer.parseInt(tempString.substring(0, help));
                tempSlot= Integer.parseInt(tempString.substring(help+1));

                tempRecordId= new RecordId(tempBlockId, tempSlot);

                solution.add(tempRecordId);
            }

            forBounds.clear();
        }



        return solution;
    }
    /**Μέθοδος που υπολογίζει και επιστρέφει σε ένα Arraylist<Record> τους κ πλησιέστερους γείτονες σειριακά. */
    public ArrayList<Record> knnLinear(int k, ArrayList<Double> pointCoordinates){
        double distance;
        ArrayList<Record> knn = new ArrayList<>();

        Record record = readDataFile.getTheData(0);
        knn.add(record);

        for(int i=1;i<readDataFile.getTotalNumberOfRecords();i++){
            boolean inserted = false;
            record = readDataFile.getTheData(i);
            for(int j=0;j<knn.size();j++){
                if(record.distanceFromPoint(pointCoordinates)<knn.get(j).distanceFromPoint(pointCoordinates)){
                    knn.add(j,record);
                    inserted = true;
                    break;
                }
            }
            if(knn.size()<k && !inserted){
                knn.add(record);
            }
            if(knn.size()==k+1){
                knn.remove(k);
            }

        }
        return knn;
    }

    /** Μέθοδος που βρίσκει τα σημεία που βρίσκονται στο skyline σε χρόνο εκτέλεσης Ο(n^2)
     * Για κάθε εγγραφή ελέγχει αν κυριαρχείται από έστω και μία άλλη εγγραφή. Αν δεν κυριαρχείται από καμία
     * ανήκει στο skyline
     */
    public ArrayList<Record> skylineBruteForce(){
        ArrayList<Record> skyline = new ArrayList<>();
        Record recordI;
        Record recordJ;
        for(int i=0;i<readDataFile.getTotalNumberOfRecords();i++){
            System.out.println(i);
            boolean dominatesAll = true;
            recordI = readDataFile.getTheData(i);
            for(int j=0;j<readDataFile.getTotalNumberOfRecords();j++){
                if(i!=j){
                    recordJ = readDataFile.getTheData(j);
                    if(recordJ.dominatesAnotherRecord(recordI)){
                        dominatesAll = false;
                        break;
                    }
                }
            }
            if(dominatesAll){
                skyline.add(recordI);
            }
        }
        return skyline;
    }


}