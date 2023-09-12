import java.io.File;
import java.util.ArrayList;

public class RStarTree {
    IndexFile indexFile;
    ReadDataFile readDataFile;
    int coordinates;
    private int totalRecords;
    private int maxEntriesInOneNode;

    public RStarTree(boolean seeIfTheFileIsAlreadyBeenCreated){
        if(seeIfTheFileIsAlreadyBeenCreated){
            this.indexFile= new IndexFile("indexFile.txt");
        }
        else{
            this.indexFile= new IndexFile();
        }
        this.readDataFile= new ReadDataFile();

        this.totalRecords=readDataFile.getTotalNumberOfRecords();
        this.coordinates= indexFile.getCoordinates();
        this.maxEntriesInOneNode= indexFile.getMaxEntriesInNode();
    }
    private long zOrdering(Record record){
        int numOfBits= 25;

        ArrayList<Double> coordinates= record.getCoordinates();

        long zOrderValue = 0;

        for (int i = 0; i < numOfBits; i++) {
            for (int dim = 0; dim < this.coordinates; dim++) {
                // The temp is being used to transmit the double number into a long
                double temp = coordinates.get(dim) * Math.pow(2, numOfBits);

                // The transmitted long number
                long transmitLong = (long) temp;

                long bit = (transmitLong >> i) & 1;

                // Interleave the bit into the Z-order value.
                zOrderValue |= (bit << (this.coordinates * i + dim));
            }
        }

        return zOrderValue;
    }
    private void putItInTheCorrectPosition(RStarTree.HelpForOrder[] helpForOrders, RStarTree.HelpForOrder tempHelpForOrder, int i){
        boolean flag=false;

        for(int j=0;j<i && !flag;j++){
            if(tempHelpForOrder.zOrderingNumber<helpForOrders[j].zOrderingNumber){
                for(int k=i;k>j;k--){
                    helpForOrders[k]=helpForOrders[k-1];
                }
                helpForOrders[j]=tempHelpForOrder;
                flag=true;
            }
        }

        if(!flag){
            helpForOrders[i]=tempHelpForOrder;
        }
    }
    private HelpForOrder[] getAllTheRecordsInOrder(){
        RStarTree.HelpForOrder[] helpForOrders= new RStarTree.HelpForOrder[this.totalRecords];
        RStarTree.HelpForOrder tempHelpForOrder;
        Record tempRecord;

        for(int i=0;i<this.totalRecords;i++){
            tempRecord = this.readDataFile.getTheData(i);
            tempHelpForOrder= new RStarTree.HelpForOrder();

            tempHelpForOrder.zOrderingNumber=zOrdering(tempRecord);
            tempHelpForOrder.record=tempRecord;
            tempHelpForOrder.positionInTheDataFile=i;

            putItInTheCorrectPosition(helpForOrders, tempHelpForOrder, i);
        }

//        ArrayList<Record> orderedRecords= new ArrayList<>();
//        for(int i=0;i<this.totalRecords;i++){
//            orderedRecords.add(helpForOrders[i].record);
//        }

        return helpForOrders;

    }

    /**
     * We will fill the nodes up to 80% of their space
     */
    public void bottomUp(){
        HelpForOrder[] allTheRecordsInOrder= getAllTheRecordsInOrder();
        int limitOfEntriesInANode= this.maxEntriesInOneNode*80/100;
        int counterOfEntriesInTheNode=limitOfEntriesInANode;


        short currentNodeId=1;
        Record tempRecord;
        RecordId tempRecordId;
        int positionInTheDataFile;
        String tempString;
        short blockFromRecordId;
        int slotOfRecordId;
        int help;
        BoundingBox tempBoundingBox;
        Entry entry;
        int level;


        level=0;
        //In this loop, all the leaves of the R*-Tree are created
        for(int i=0;i<this.totalRecords;i++){
            if(counterOfEntriesInTheNode==limitOfEntriesInANode){
                currentNodeId= indexFile.createNewNode(level);
                counterOfEntriesInTheNode=0;
            }

            tempRecord= allTheRecordsInOrder[i].record;
            positionInTheDataFile= allTheRecordsInOrder[i].positionInTheDataFile;

            tempString= readDataFile.getTheTargetBlockAndTheSlotOffset(positionInTheDataFile);
            help = tempString.indexOf("&");

            blockFromRecordId= (short) Integer.parseInt(tempString.substring(0,help));
            slotOfRecordId= Integer.parseInt(tempString.substring(help+1));

            tempRecordId= new RecordId(blockFromRecordId, slotOfRecordId);


            ArrayList<Double> boundsForBoundingBox= new ArrayList<>();

            //in order to create the boundingBox
            for(int k=0;k<this.coordinates;k++){
                for(int l=0;l<this.coordinates;l++){
                    boundsForBoundingBox.add(tempRecord.getCoordinates().get(k));
                }
            }

            tempBoundingBox= new BoundingBox(boundsForBoundingBox, this.coordinates);

            entry= new EntryOfLeaf(tempRecordId, tempBoundingBox, -1);

            indexFile.addNewEntryInNode(currentNodeId, entry);
            counterOfEntriesInTheNode++;
        }




        int numberOfNodesInThePreviousLevel=0;
        int numberOfEntriesThatAllTheNodesInTheCurrentLevelHave;

        Node tempNode;
        int childId;
        int targetNode;
        int lastNodeIdOfThePreviousLevel=0;



        numberOfNodesInThePreviousLevel= currentNodeId - lastNodeIdOfThePreviousLevel;
        numberOfEntriesThatAllTheNodesInTheCurrentLevelHave=0;



        targetNode=1;
        //Now all the nonLeaves nodes are created
        while(numberOfNodesInThePreviousLevel!=1){
            lastNodeIdOfThePreviousLevel=currentNodeId;
            level+=1;
            currentNodeId=indexFile.createNewNode(level);

            //In this loop the nodes of the same level are being created
            for(;numberOfEntriesThatAllTheNodesInTheCurrentLevelHave<numberOfNodesInThePreviousLevel;targetNode++){
                tempNode= indexFile.getNodeFromTheFile(targetNode);
                tempBoundingBox= BoundingBox.findBoundingBoxToFitAllEntries(tempNode.getEntries());
                childId=targetNode;
                entry= new Entry(tempBoundingBox, childId);

                if(!(indexFile.addNewEntryInNode(currentNodeId, entry))){
                    currentNodeId= indexFile.createNewNode(level);
                    indexFile.addNewEntryInNode(currentNodeId,entry);
                }

                numberOfEntriesThatAllTheNodesInTheCurrentLevelHave++;
            }



            numberOfNodesInThePreviousLevel= currentNodeId - lastNodeIdOfThePreviousLevel;
            numberOfEntriesThatAllTheNodesInTheCurrentLevelHave=0;
        }

        //The last currentNodeId is the root of the R*-Tree
        indexFile.setTheRoot(currentNodeId);

    }

    public RecordId search(ArrayList<Double> coordinatesOfAPoint){
        ArrayList<Double> boundsForBoundingBox= new ArrayList<>();
        BoundingBox tempBoundingBox;
        Node root;


        //in order to create the boundingBox
        for(int k=0;k<this.coordinates;k++){
            for(int l=0;l<this.coordinates;l++){
                boundsForBoundingBox.add(coordinatesOfAPoint.get(k));
            }
        }
        tempBoundingBox= new BoundingBox(boundsForBoundingBox, this.coordinates);

        root= indexFile.getTheRoot();

        return mySearch(root, tempBoundingBox);
    }

    private RecordId mySearch(Node root, BoundingBox boundingBoxOfThePointWeWantToFind){
        ArrayList<Entry> tempArrayListOfEntriesInANode;
        boolean sameBoundingBox;
        tempArrayListOfEntriesInANode= root.getEntries();

        if(root.getLevel()==0){ //This means that the node is a leaf
            EntryOfLeaf tempEntryOfLeaf;
            BoundingBox tempBoundingBox;

            for(int i=0;i<tempArrayListOfEntriesInANode.size();i++){
                tempEntryOfLeaf= (EntryOfLeaf) tempArrayListOfEntriesInANode.get(i);
                tempBoundingBox= tempEntryOfLeaf.getBoundingBox();
                sameBoundingBox=true;

                //See if the boundingBoxes are the same. If they are, then the point has been found
                for(int j=0;j<tempBoundingBox.getBounds().size() && sameBoundingBox;j++){
                    if( Double.compare( tempBoundingBox.getBounds().get(j) , boundingBoxOfThePointWeWantToFind.getBounds().get(j) ) !=0 ){
                        sameBoundingBox=false;
                    }
                }

                if(sameBoundingBox){
                    //nothingToDoHere toDo toDelete
                    return tempEntryOfLeaf.getRecordId();
                }
            }
        }
        else{
            Entry tempEntry;
            Node help;

            for(int i=0;i<tempArrayListOfEntriesInANode.size();i++){
                tempEntry= tempArrayListOfEntriesInANode.get(i);
                if(tempEntry.getBoundingBox().findOverlapWithOtherBox(boundingBoxOfThePointWeWantToFind)==0){ //Checks if the boundingBox surrounds the pointWeWantToFind. If no, the entry will not been searched //toDO
                    help= indexFile.getNodeFromTheFile((int) tempEntry.getChildId());

                    RecordId tempMySearch= mySearch(help, boundingBoxOfThePointWeWantToFind);

                    if( tempMySearch != null){
                        return tempMySearch;
                    }
                }
            }
        }

        return null;

    }

    public static void main(String[] args){
        ReadDataFile readDataFile1= new ReadDataFile();
        Record record;
        RecordId recordId;

        RStarTree rStarTree= new RStarTree(true);
//        rStarTree.bottomUp();
//        Node node= rStarTree.indexFile.getTheRoot();


//        ArrayList<Double> temp= new ArrayList<>();
//
//        temp.add(41.4988902);
//        temp.add(26.5321304);


        for(int i=0;i<readDataFile1.getTotalNumberOfRecords();i++){
            record= readDataFile1.getTheData(i);

            recordId= rStarTree.search(record.getCoordinates());

            if(recordId==null){
                System.out.println("Not found");
            }
            else {
                System.out.println("Block: " + recordId.getBlock() + ", Slot: " + recordId.getSlot());
//                System.out.println(readDataFile1.getTheData(recordId.getBlock(), recordId.getSlot()));
            }

        }
    }




    private class HelpForOrder{
        long zOrderingNumber;
        Record record;
        int positionInTheDataFile;
    }

    //    public static void main(String[] args){
//        RStarTree createIndexFile= new RStarTree();
//        createIndexFile.getAllTheRecordsInOrder();
//    }

}


