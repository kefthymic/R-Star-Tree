import java.io.File;
import java.util.ArrayList;
import java.util.Stack;
import java.util.PriorityQueue;

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

    /**
     *
     * This function search for a specific, record in the R-tree
     * It returns the RecordId that shows, in which block and slot this record is
     * If there is not any record with these coordinates, then the function returns null
     */
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

    /**
     * Help function to implement the search function. It uses recursion
     */
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
                    return tempEntryOfLeaf.getRecordId();
                }
            }
        }
        else{
            Entry tempEntry;
            Node help;

            for(int i=0;i<tempArrayListOfEntriesInANode.size();i++){
                tempEntry= tempArrayListOfEntriesInANode.get(i);
                if(tempEntry.getBoundingBox().seeIfThereIsAtLeastACommonPoint(boundingBoxOfThePointWeWantToFind)){ //Checks if the boundingBox surrounds the pointWeWantToFind. If no, the entry will not been searched
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


    /**
     * This search is been used in the delete function
     * It returns a string that shows all the node and the entryOfLeaf that contain the record with these coordinates
     */
    private String searchForTheDelete(ArrayList<Double> coordinatesOfAPoint){
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

        return mySearchForTheDelete(root, tempBoundingBox);
    }

    /**
     * The help function to implement the searchForDelete. It uses recursion
     */
    private String mySearchForTheDelete(Node root, BoundingBox boundingBoxOfThePointWeWantToFind){
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
                    return root.getNodeId()+"&"+i;
                }
            }
        }
        else{
            Entry tempEntry;
            Node help;

            for(int i=0;i<tempArrayListOfEntriesInANode.size();i++){
                tempEntry= tempArrayListOfEntriesInANode.get(i);
                if(tempEntry.getBoundingBox().seeIfThereIsAtLeastACommonPoint(boundingBoxOfThePointWeWantToFind)){ //Checks if the boundingBox surrounds the pointWeWantToFind. If no, the entry will not been searched
                    help= indexFile.getNodeFromTheFile((int) tempEntry.getChildId());

                    String tempMySearchForTheDelete= mySearchForTheDelete(help, boundingBoxOfThePointWeWantToFind);

                    if( tempMySearchForTheDelete != null){
                        return tempMySearchForTheDelete;
                    }
                }
            }
        }

        return null;

    }

    /**
     * This function fixes the recordId of the record that changed its position to take the empty space that the deleted record created
     * The helpForDelete has all the necessary information
     */
    private void correctTheRecordIdOfTheEntryThatChangedPositionInTheDataFile(HelpForDelete helpForDelete){
        if(helpForDelete.coordinatesOfTheLastRecordThatChangedPlace==null){
            return;
        }

        String temp= searchForTheDelete(helpForDelete.coordinatesOfTheLastRecordThatChangedPlace);

        if(temp==null){
            return;
        }

        int blockId;
        int slotInTheBlock;
        int help;

        help= temp.indexOf("&");
        blockId= Integer.parseInt(temp.substring(0, help));
        slotInTheBlock= Integer.parseInt(temp.substring(help+1));

        indexFile.changeTheRecordIdOfAnEntryOfLeaf((short) blockId, slotInTheBlock, helpForDelete.recordIdOfTheRecordThatBeenDeleted);
    }


    /**
     * This function delete the record with these coordinates from the R-tree and from the dataFile
     * First it takes the information that needs to delete it, with the myDelete function
     * Then, with the fixRtree function it proceed to the deletion and the last record in the same node takes the empty space
     * Finally the correctTheRecordIdOfTheEntryThatChangedPositionInTheDataFile is called to update the recordId, because the empty space from the deleted record in the dataFile is taken by the last element from the datFile
     * @param coordinatesOfAPoint
     */
    public void delete(ArrayList<Double> coordinatesOfAPoint){
        ArrayList<Double> boundsForBoundingBox= new ArrayList<>();
        BoundingBox tempBoundingBox;
        Node root;
        HelpForDelete helpForDelete;


        //in order to create the boundingBox
        for(int k=0;k<this.coordinates;k++){
            for(int l=0;l<this.coordinates;l++){
                boundsForBoundingBox.add(coordinatesOfAPoint.get(k));
            }
        }
        tempBoundingBox= new BoundingBox(boundsForBoundingBox, this.coordinates);

        root= indexFile.getTheRoot();

        helpForDelete= myDelete(root, tempBoundingBox);
        if(helpForDelete!=null){
            fixRtree(helpForDelete);
            correctTheRecordIdOfTheEntryThatChangedPositionInTheDataFile(helpForDelete);
        }
        else {
            System.out.println("Not in the file");
        }
    }

    private HelpForDelete myDelete(Node root, BoundingBox boundingBoxOfThePointWeWantToDelete){
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
                    if( Double.compare( tempBoundingBox.getBounds().get(j) , boundingBoxOfThePointWeWantToDelete.getBounds().get(j) ) !=0 ){
                        sameBoundingBox=false;
                    }
                }

                if(sameBoundingBox){
                    return new HelpForDelete(readDataFile.deleteARecordFromTheFile(tempEntryOfLeaf.getRecordId()), tempEntryOfLeaf.getRecordId(), root.getNodeId()+"&"+i+"\n");
                }
            }
        }
        else{
            Entry tempEntry;
            Node help;

            for(int i=0;i<tempArrayListOfEntriesInANode.size();i++){
                tempEntry= tempArrayListOfEntriesInANode.get(i);
                if(tempEntry.getBoundingBox().seeIfThereIsAtLeastACommonPoint(boundingBoxOfThePointWeWantToDelete)){ //Checks if the boundingBox surrounds the pointWeWantToFind. If no, the entry will not been searched
                    help= indexFile.getNodeFromTheFile((int) tempEntry.getChildId());

                    HelpForDelete tempMyDelete= myDelete(help, boundingBoxOfThePointWeWantToDelete);

                    if( tempMyDelete != null){
                        String helpString= root.getNodeId()+"&"+i+"\n";
                        tempMyDelete.whereIsTheDeletedData= helpString+tempMyDelete.whereIsTheDeletedData;
                        return tempMyDelete;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Always the first element is the leaf node
     */
    private void fixRtree(HelpForDelete dataFromThePointToDelete){
        Stack<HelpForFixing> needsModify;
        String whereIsTheDeletedData= dataFromThePointToDelete.whereIsTheDeletedData;
        int tempNodeId;
        int tempTheEntryThatModified;
        int help1;
        int help2;
        Node tempNode;
        ArrayList<Entry> allTheEntriesOfANode;
        HelpForFixing tempHelpForFixing;
        boolean helpBoolean;

        needsModify= new Stack<>();
        //The stack is filling with the data of the dataFromThePointToDelete. In the end the first element will be a leaf node
        while(!whereIsTheDeletedData.equals("")){
            help1= whereIsTheDeletedData.indexOf("&");
            help2= whereIsTheDeletedData.indexOf("\n");
            tempNodeId= Integer.parseInt(whereIsTheDeletedData.substring(0, help1));
            tempTheEntryThatModified= Integer.parseInt(whereIsTheDeletedData.substring(help1+1, help2));
            whereIsTheDeletedData= whereIsTheDeletedData.substring(help2+1);

            needsModify.add(new HelpForFixing(tempNodeId, tempTheEntryThatModified));
        }

        //first the record in the R-tree that should have been deleted, will take all the data of the last record in this node and the numberOfRecords in this node will decrease
        tempHelpForFixing= needsModify.pop();
        if(indexFile.deleteAnEntryAndFillItWithTheLastEntryOfTheNode((short) tempHelpForFixing.nodeIdToFix, tempHelpForFixing.theEntryThatModified)){
            tempNode= indexFile.getNodeFromTheFile((short) tempHelpForFixing.nodeIdToFix);
            allTheEntriesOfANode= tempNode.getEntries();
        }
        else{
            allTheEntriesOfANode= null;
        }

        //Now the boundingBox of all the above nodes are updated
        while(!needsModify.empty()){
            helpBoolean=true;

            tempHelpForFixing= needsModify.pop();

            if(allTheEntriesOfANode==null){
                helpBoolean= indexFile.deleteAnEntryAndFillItWithTheLastEntryOfTheNode((short) tempHelpForFixing.nodeIdToFix, tempHelpForFixing.theEntryThatModified);
            }
            else{
                BoundingBox boundingBox= BoundingBox.findBoundingBoxToFitAllEntries(allTheEntriesOfANode);
                indexFile.updateTheBoundingBoxOfAnEntryOfANode((short) tempHelpForFixing.nodeIdToFix, tempHelpForFixing.theEntryThatModified, boundingBox);
            }

            if(!helpBoolean){
                allTheEntriesOfANode= null;
            }
            else{
                tempNode= indexFile.getNodeFromTheFile((short) tempHelpForFixing.nodeIdToFix);
                allTheEntriesOfANode= tempNode.getEntries();
            }
        }
    }

    /**
     * Function that returns an arrayList with all the records that are inside the rangeQuery
     *
     */
    public ArrayList<RecordId> rangeQuery(BoundingBox queryBoundingBox){
        Node root= indexFile.getTheRoot();


        return myRangeQuery(root, queryBoundingBox);
    }

    private ArrayList<RecordId> myRangeQuery(Node root, BoundingBox queryBoundingBox){

        ArrayList<RecordId> solution= new ArrayList<>();
        ArrayList<RecordId> tempArrayList;

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
                    if( ! tempEntryOfLeaf.getBoundingBox().seeIfThereIsAtLeastACommonPoint(queryBoundingBox) ){
                        sameBoundingBox=false;
                    }
                }

                if(sameBoundingBox){
                    solution.add(tempEntryOfLeaf.getRecordId());
                }
            }
        }
        else{
            Entry tempEntry;
            Node help;

            for(int i=0;i<tempArrayListOfEntriesInANode.size();i++){
                tempEntry= tempArrayListOfEntriesInANode.get(i);
                if(tempEntry.getBoundingBox().seeIfThereIsAtLeastACommonPoint(queryBoundingBox)){ //Checks if the boundingBox surrounds the pointWeWantToFind. If no, the entry will not been searched
                    help= indexFile.getNodeFromTheFile((int) tempEntry.getChildId());

                    tempArrayList= myRangeQuery(help, queryBoundingBox);


                    //There all the solutions from the previous recursive calls are been added to one
                    for(int j=0;j<tempArrayList.size();j++){
                        solution.add(tempArrayList.get(j));
                    }
                }
            }
        }

        return solution;
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
    /** Βοηθητική μέθοδος για το skyline. Επιστρέφει true αν τα στοιχεία που ήδη βρίσκονται στο skyline
     * κυριαρχούν επί ενός entry που δέχεται ως παράμετρο (δλδ αν έστω ένα στοιχείο του skyline κυριαρχεί επί του entry) */
    private boolean skylineDominatesEntry(ArrayList<EntryOfLeaf> skyline, Entry entry){
        ArrayList<Double> coordinatesOfBottomLeftCorner = new ArrayList<>();
        for(int i=0;i<entry.getBoundingBox().getDimensions();i++){
            coordinatesOfBottomLeftCorner.add(entry.getBoundingBox().getBound(i,false));
        }
        for(int i=0;i<skyline.size();i++){
            EntryOfLeaf skylineEntry = skyline.get(i);
            if(skylineEntry.dominatesAnotherEntry(entry)){
                return true;
            }
        }
        return false;
    }
    /** Μέθοδος που επιστρέφει σε ένα Arraylist όλες τις εγγραφές που ανήκουν στο skyline*/
    public ArrayList<Record> skyline(){
        ArrayList<EntryOfLeaf> skyline = new ArrayList<>();
        Node root= indexFile.getTheRoot();
        ArrayList<Entry> entriesOfRoot = root.getEntries();

        PriorityQueue<Entry> priorityQueue = new PriorityQueue<>();
        for(int i=0;i<entriesOfRoot.size();i++){
            priorityQueue.add(entriesOfRoot.get(i));
        }


        while(!priorityQueue.isEmpty()) {
            Entry nextEntry = priorityQueue.poll(); //το πρώτο entry στην priority queue
            int childId = (int) nextEntry.getChildId();
            if (childId == -1) { //είναι σε φύλλο
                EntryOfLeaf entryOfLeaf = (EntryOfLeaf) nextEntry;
                boolean dominatesAll = true;
                for(int i=0;i< skyline.size();i++){
                    if(skyline.get(i).dominatesAnotherEntryOfLeaf(entryOfLeaf)){
                        dominatesAll = false;
                        break;
                    }
                }
                if(dominatesAll==true){ //μπαίνει στο skyline
                    skyline.add(entryOfLeaf);
                }

            } else {
                if(!skylineDominatesEntry(skyline,nextEntry)) { //expand : το πρώτο entry δεν κυριαρχείται εξ ολοκλήρου από την κορυφογραμμή
                    Node nextNode = indexFile.getNodeFromTheFile(childId); //οπότε προσθέτουμε όλα τα entries που βρίσκονται στον κόμβο-παιδί του στο priority queue
                    ArrayList<Entry> entries = nextNode.getEntries();
                    for (int i = 0; i < entries.size(); i++) {
                        priorityQueue.add(entries.get(i));
                    }
                }

            }

        }
        //Βρίσκουμε τις εγγραφές που αντιστοιχούν στα EntryOfLeaf του skyline
        ArrayList<Record> skylineWithRecords = new ArrayList<>();
        for(int i=0;i<skyline.size();i++){
            RecordId recordId = skyline.get(i).getRecordId();
            int slot = recordId.getSlot();
            short block = recordId.getBlock();
            Record record = readDataFile.getTheData(block,slot);
            skylineWithRecords.add(record);
        }


        return skylineWithRecords;
    }

    /**
     * Helpful class that is needed to do the bottomUp. Every record is saved there, with its zOrderingNumber and its position in the dataFile
     */
    private class HelpForOrder{
        long zOrderingNumber;
        Record record;
        int positionInTheDataFile;
    }

    /**
     * Helpful function for the deletions
     */
    private class HelpForDelete{
        ArrayList<Double> coordinatesOfTheLastRecordThatChangedPlace;
        RecordId recordIdOfTheRecordThatBeenDeleted;
        String whereIsTheDeletedData;

        HelpForDelete(ArrayList<Double> coordinatesOfTheLastRecordThatChangedPlace, RecordId recordIdOfTheRecordThatBeenDeleted, String whereIsTheDeletedData){
            this.coordinatesOfTheLastRecordThatChangedPlace= coordinatesOfTheLastRecordThatChangedPlace;
            this.recordIdOfTheRecordThatBeenDeleted= recordIdOfTheRecordThatBeenDeleted;
            this.whereIsTheDeletedData= whereIsTheDeletedData;
        }
    }

    /**
     * Helpful function for the fixing after a deletion. It is a pair of the node to fix and which entry
     */
    private class HelpForFixing{
        int nodeIdToFix;
        int theEntryThatModified;

        HelpForFixing(int nodeIdToFix, int theEntryThatModified){
            this.nodeIdToFix= nodeIdToFix;
            this.theEntryThatModified= theEntryThatModified;
        }
    }

}


