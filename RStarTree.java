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



    /**Μέθοδος που δέχεται έναν ακέραιο κ και τις συντεταγμένες ενός σημείου και επιστρέφει του κ πλησιέστερους γείτονες του σημείου */
    public ArrayList<Record> knn(int k, ArrayList<Double> pointCoordinates){
        Node root= indexFile.getTheRoot(); //ξεκινάει από ρίζα με στόχο να βρει το φύλλο που απέχει το λιγότερο από το σημείο
        ArrayList<Entry> entriesOfRoot = root.getEntries();
        if(entriesOfRoot.get(0).getChildId()==-1){ //αν η ρίζσ είναι φύλλο, σημαίνει ότι υπάρχει μόνος ένας κόμβος στο δέντρο, οπότε καλείται η εύρεση των κ γειτόνων σειριακά
            LinearMethods linearMethods = new LinearMethods();
            return linearMethods.knnLinear(k,pointCoordinates);
        }
        int posOfBestEntry = 0;  //λαμβάνεται ως entry αναφοράς το πρώτο entry του κόμβου
        double minDistance = entriesOfRoot.get(0).getBoundingBox().findMinDistFromPoint(pointCoordinates);
        for(int i=1;i<entriesOfRoot.size();i++){
            if(entriesOfRoot.get(i).getBoundingBox().findMinDistFromPoint(pointCoordinates)<minDistance){
                minDistance = entriesOfRoot.get(i).getBoundingBox().findMinDistFromPoint(pointCoordinates);
                posOfBestEntry = i;
            }
        }
        //έχει βρεθεί το entry του κόμβου που απέχει τη λιγότερη απόσταση από το σημείο
        int childId = (int)entriesOfRoot.get(posOfBestEntry).getChildId();
        Node nextNode = indexFile.getNodeFromTheFile(childId); //έπειτα πηγαίνει στον κόμβο που δείχνει το καλύτερο entry που βρέθηκε πριν
        while(nextNode.getLevel()>0){  //όσο δε βρισκόμαστε σε φύλλα η διαδικασία αυτή επαναλαμβάνεται
            ArrayList<Entry> entries = nextNode.getEntries();
            posOfBestEntry = 0;
            minDistance = entries.get(0).getBoundingBox().findMinDistFromPoint(pointCoordinates);
            for(int i=1;i<entries.size();i++){
                if(entries.get(i).getBoundingBox().findMinDistFromPoint(pointCoordinates)<minDistance){
                    minDistance = entries.get(i).getBoundingBox().findMinDistFromPoint(pointCoordinates);
                    posOfBestEntry = i;
                }
            }
            childId = (int)entries.get(posOfBestEntry).getChildId();
            nextNode = indexFile.getNodeFromTheFile(childId);
        }


        //μετά το τέλος του loop είμαστε σε φύλλο
        //οπότε σειριακά εξετάζονται μόνο τα στοιχεία του φύλλου και εντοπίζονται οι κ πλησιέστεροι γείτονες(που βρίσκονται μέσα στον κόμβο)
        double distance;
        ArrayList<EntryOfLeaf> knn = new ArrayList<>();
        ArrayList<EntryOfLeaf> entriesOfLeaf = new ArrayList<>();
        ArrayList<Entry> entries = nextNode.getEntries(); // τα entries που βρίσκονται στο φύλλο γίνονται EntryOfLeaf
        for(int i=0;i<entries.size();i++){
            EntryOfLeaf entryOfLeaf = (EntryOfLeaf) entries.get(i);
            entriesOfLeaf.add(entryOfLeaf);
        }
        EntryOfLeaf entryOfLeaf = entriesOfLeaf.get(0);
        knn.add(entryOfLeaf);

        for(int i=1;i<entriesOfLeaf.size();i++){
            entryOfLeaf = entriesOfLeaf.get(i);
            boolean inserted = false;
            for(int j=0;j<knn.size();j++){
                if(entryOfLeaf.getBoundingBox().findMinDistFromPoint(pointCoordinates)<knn.get(j).getBoundingBox().findMinDistFromPoint(pointCoordinates)){
                    knn.add(j,entryOfLeaf);
                    inserted = true;
                    break;
                }
            }
            if(knn.size()<k && !inserted){
                knn.add(entryOfLeaf);
            }
            if(knn.size()==k+1){
                knn.remove(k);
            }

        }
        //υπολογίζεται τώρα η ακτίνα (απόστση μεταξύ κ-οστου γείτονα και σημείου)
        double radius = knn.get(knn.size()-1).getBoundingBox().findMinDistFromPoint(pointCoordinates);

        //ξεκινάει πάλι από τη ρίζα
        root= indexFile.getTheRoot();
        entriesOfRoot = root.getEntries();
        ArrayList<Entry> entriesToCheck = new ArrayList<>();
        for(int i=0;i<entriesOfRoot.size();i++){ //τοποθετεί όλα τα στοιχεία της ρίζας σε ένα Arraylist entriesToCheck
            entriesToCheck.add(entriesOfRoot.get(i));
        }

        while(entriesToCheck.size()>0){  //όσο υπάρχουν ακόμα entries που πρέπει να ελεγχθούν
            Entry entry = entriesToCheck.get(0); // το entry που θα ελεγχθεί
            if(entry.getChildId()==-1){ //entry είναι σε φύλλο
                entryOfLeaf = (EntryOfLeaf) entry;
                double distanceFromPoint = entryOfLeaf.getBoundingBox().findMinDistFromPoint(pointCoordinates);
                for(int j=0;j<knn.size();j++){
                    if(distanceFromPoint==knn.get(j).getBoundingBox().findMinDistFromPoint(pointCoordinates)){ //εχει μπει ηδη
                        break;
                    }
                    if(distanceFromPoint<knn.get(j).getBoundingBox().findMinDistFromPoint(pointCoordinates)){ //ανήκει στους πλησιέστερους γείτονες
                        knn.add(j,entryOfLeaf);
                        break;
                    }
                }
                if(knn.size()<k){
                    knn.add(entryOfLeaf);
                }
                if(knn.size()==k+1){
                    knn.remove(k);
                }

            }
            else{ //entry είναι σε μη φύλλο
                if(entry.getBoundingBox().findMinDistFromPoint(pointCoordinates)<radius){ //η ελάχιστη απόσταση του entry και του σημείου είναι μικρότερη της ακτίνας, οπότε γίνεται expand
                    childId =(int) entry.getChildId();
                    nextNode = indexFile.getNodeFromTheFile(childId);
                    entries = nextNode.getEntries();
                    for(int i=0;i<entries.size();i++){ //expand, δλδ όλα τα entries που ανήκουν στον κόμβο που δείχνει το entry μπαίνουν στο Arraylist entriesToCheck για να ελεγχθούν στη συνέχεια
                        entriesToCheck.add(entries.get(i));
                    }

                }
                else{ // η ελάχιστη απόσταση του entry και του σημείου είναι μεγαλύτερη της ακτίνας οπότε δεν γίνεται expand και όλα τα entry που ανήκουν στον κόμβο που δείχνει το τωρινό entry δε θα ελεγχθούν καν

                }

            }
            entriesToCheck.remove(0);   //έχει ελεγχθεί και αφαιρείται το πρώτο στοιχείο του Arraylist
        }
        ArrayList<Record> knnWithRecords = new ArrayList<>(); //εντοπίζονται τα records του datafile που αντιστοιχούν στα entriesOfLeaf που βρίσκονται στο arraylist με τους κ πλησιέστερους γείτονες
        for(int i=0;i<knn.size();i++){
            RecordId recordId = knn.get(i).getRecordId();
            int slot = recordId.getSlot();
            short block = recordId.getBlock();
            Record record = readDataFile.getTheData(block,slot);
            knnWithRecords.add(record);

        }
        return knnWithRecords;
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
    /**Μέθοδος που κατασκευάζει το R*Tree παίρνοντας τις εγγραφές μία μία από το DataFile και για κάθε μία από αυτές
     * καλέι την insertEntry για να την εισάγει στο δέντρο */
    public void create(){
        short blockFromRecordId;
        int slotOfRecordId;
        String tempString;
        Record tempRecord;
        RecordId tempRecordId;
        int help;
        EntryOfLeaf entryOfLeaf;

        short currentNodeId= indexFile.createNewNode(0);

        indexFile.setTheRoot(currentNodeId);

        for(int i=0;i<readDataFile.getTotalNumberOfRecords();i++) {
            tempRecord = readDataFile.getTheData(i);
            tempString= readDataFile.getTheTargetBlockAndTheSlotOffset(i);
            help = tempString.indexOf("&");

            blockFromRecordId= (short) Integer.parseInt(tempString.substring(0,help));
            slotOfRecordId= Integer.parseInt(tempString.substring(help+1));
            tempRecordId= new RecordId(blockFromRecordId, slotOfRecordId);

            ArrayList<Double> bounds = new ArrayList<>();
            ArrayList<Double> coordinates = tempRecord.getCoordinates();
            for(int j=0;j<coordinates.size();j++){
                bounds.add(coordinates.get(j));
                bounds.add(coordinates.get(j));
            }
            BoundingBox boundingBox = new BoundingBox(bounds,coordinates.size());
            entryOfLeaf = new EntryOfLeaf(tempRecordId,boundingBox,-1);
            this.insertEntryOfLeaf(entryOfLeaf);

        }
    }
    /**Μέθοδος που εισάγει ένα entry στην κατάλληλη θέση στο δέντρο */
    public void insertEntryOfLeaf(EntryOfLeaf entryOfLeaf){
        ArrayList<Integer> NodesAndEntriesVisited = chooseSubtree(entryOfLeaf); //μονοπάτι από κορυφή μέχρι κατάλληλο φύλλο
        int nodeIdOfLeaf = NodesAndEntriesVisited.get(NodesAndEntriesVisited.size()-1); //id του φύλλου όπου θα εισαχθεί το entry
        if(indexFile.addNewEntryInNode(nodeIdOfLeaf,entryOfLeaf)){ //αν υπάρχει χώρος για εισαγωγή
            while(NodesAndEntriesVisited.size()>1){    // ενημέρωση του δέντρου(όλων των BoundingBoxes από τη ρίζα μέχρι το φύλλο) για να συμπεριλάβουν και το νέο entry
                int IdOfVisitedNode = NodesAndEntriesVisited.get(0);   //κόμβος στο μονοπάτι
                NodesAndEntriesVisited.remove(0);
                int posOfEntry = NodesAndEntriesVisited.get(0);   //θέση του entry στον κόμβο
                NodesAndEntriesVisited.remove(0);
                Node nodeVisited = indexFile.getNodeFromTheFile(IdOfVisitedNode);
                Entry entryVisited = nodeVisited.getEntries().get(posOfEntry);
                BoundingBox boundingBox = entryVisited.getBoundingBox(); //boundingbox πριν την εισαγωγή
                BoundingBox newBoundingBox = boundingBox.enlargeBoundingBoxToInsertNewEntry(entryOfLeaf);  //boundingbox μετά την εισαγωγή
                indexFile.updateTheBoundingBoxOfAnEntryOfANode((short) IdOfVisitedNode,posOfEntry,newBoundingBox);
            }
        }
        else{ //δεν υπάρχει χώρος, οπότε καλείται η overflowTreatment και μετά επιχειρείται ξανά η εισαγωγή
            overflowTreatment(NodesAndEntriesVisited);
            insertEntryOfLeaf(entryOfLeaf);
        }
    }/**Μέθοδος που υλοποιεί τον αλγόριθμο chooseSubTree και επιστρέφει το μονοπάτι των κόμβων και των entry που διέτρεξε
     το δέντρο ξεκινώντας από την ρίζα και φτάνοντας ως το κατάλληλο φύλλο.
     πχ αν ξεκινήσει από ρίζα με nodeId = 4, επιλέξει ως πιο κατάλληλο το entry που βρίσκεται στη θέση 3, έπειτα μεταβεί στον
     κόμβο με nodeId = 8,  επιλέξει ως πιο κατάλληλο το entry που βρίσκεται στη θέση 1 και τέλος καταλήξει στο φύλλο με nodeId = 17,
     η μέθοδος θα επιστρέψει ένα Arraylist της μορφής (4,3,8,1,17)
     */
    public ArrayList<Integer> chooseSubtree(EntryOfLeaf entryOfLeaf){
        Node root = indexFile.getTheRoot();
        ArrayList<Integer> NodesAndEntriesVisited = new ArrayList<>();
        Node tempNode = root;

        while (tempNode.getLevel()>0){
            NodesAndEntriesVisited.add((int)tempNode.getNodeId());
            ArrayList<Entry> entries = tempNode.getEntries();
            int posOfBestEntry=0;
            if(tempNode.getLevel()>1){
                posOfBestEntry = 0;
                BoundingBox boundingBox1 = entries.get(0).getBoundingBox();
                BoundingBox boundingBox2 = boundingBox1.enlargeBoundingBoxToInsertNewEntry(entryOfLeaf);
                double bestAreaEnlargement = boundingBox2.getArea() - boundingBox1.getArea();
                double smallestArea = boundingBox1.getArea();
                double areaEnlargement,area;
                for(int i=1;i<entries.size();i++){
                    boundingBox1 = entries.get(i).getBoundingBox();
                    boundingBox2 = boundingBox1.enlargeBoundingBoxToInsertNewEntry(entryOfLeaf);
                    areaEnlargement = boundingBox2.getArea()-boundingBox1.getArea();
                    if(areaEnlargement<bestAreaEnlargement){
                        bestAreaEnlargement = areaEnlargement;
                        smallestArea = boundingBox1.getArea();
                        posOfBestEntry = i;
                    }
                    else if(areaEnlargement==bestAreaEnlargement && boundingBox1.getArea()<smallestArea){
                        smallestArea = boundingBox1.getArea();
                        posOfBestEntry = i;
                    }

                }

            }
            else if(tempNode.getLevel()==1){
                posOfBestEntry = 0;
                BoundingBox boundingBox1 = entries.get(0).getBoundingBox();
                BoundingBox boundingBox2 = boundingBox1.enlargeBoundingBoxToInsertNewEntry(entryOfLeaf);
                double bestOverlap = boundingBox2.findOverlapWithOtherBox(boundingBox1);
                double bestAreaEnlargement = boundingBox2.getArea() - boundingBox1.getArea();
                //System.out.println("Best overlap before: "+bestOverlap+" Best areaEnlar before: "+bestAreaEnlargement);
                double overlap, areaEnlargement;
                for(int i=1;i<entries.size();i++){
                    boundingBox1 = entries.get(i).getBoundingBox();
                    boundingBox2 = boundingBox1.enlargeBoundingBoxToInsertNewEntry(entryOfLeaf);
                    overlap = boundingBox2.findOverlapWithOtherBox(boundingBox1);
                    areaEnlargement = boundingBox2.getArea()-boundingBox1.getArea();
                    // System.out.println("overlap: "+overlap+"areaEnlar : "+areaEnlargement);
                    if(overlap<bestOverlap){
                        bestOverlap = overlap;
                        bestAreaEnlargement = areaEnlargement;
                        posOfBestEntry = i;
                    }
                    else if(overlap==bestOverlap && areaEnlargement<bestAreaEnlargement){
                        bestAreaEnlargement = areaEnlargement;
                        posOfBestEntry = i;
                    }

                }
            }
            NodesAndEntriesVisited.add(posOfBestEntry);
            Entry bestEntry = entries.get(posOfBestEntry);
            int childId = (int)bestEntry.getChildId();
            tempNode = indexFile.getNodeFromTheFile(childId);
        }
        NodesAndEntriesVisited.add((int)tempNode.getNodeId());
        return NodesAndEntriesVisited;
    }
    /** Μέθοδος που καλείτα όταν δεν υπάρχει χώρος σε ένα φύλλο για την εισαγωγή ενός entry.
     * Αρχικά γίνεται reInsert του 30% των entries του φύλλου, κι έπειτα αν ακόμα δεν υπάρχει χώρος γίνεται split του φύλλου */
    public void overflowTreatment(ArrayList<Integer> NodesAndEntriesVisited){
        int IdOfLeaf = NodesAndEntriesVisited.get(NodesAndEntriesVisited.size()-1);
        Node leaf = indexFile.getNodeFromTheFile(IdOfLeaf);
        ArrayList<Entry> entries = leaf.getEntries();
        int numberOfEntriesToBeRemoved = (int) (entries.size()*0.3);
        int posOfLastEntry = entries.size()-1;
        for(int i=0 ;i<numberOfEntriesToBeRemoved;i++){
            EntryOfLeaf entryOfLeaf = (EntryOfLeaf)entries.get(posOfLastEntry);
            indexFile.deleteAnEntryAndFillItWithTheLastEntryOfTheNode((short) IdOfLeaf,posOfLastEntry);
            posOfLastEntry--;
            insertEntryOfLeaf(entryOfLeaf);

        }
        if(leaf.getEntries().size()==indexFile.getMaxEntriesInNode()){
            splitNode(NodesAndEntriesVisited);
        }

    }/**Μέθοδος που πραγματοποιεί το σπλιτ ενός κόμβου */
    public void splitNode(ArrayList<Integer> NodesAndEntriesVisited){

        int nodeIdToSplit = NodesAndEntriesVisited.get(NodesAndEntriesVisited.size()-1); //id του κόμβου που θα γίνει σπλιτ
        NodesAndEntriesVisited.remove(NodesAndEntriesVisited.size()-1);
        if(nodeIdToSplit==indexFile.getTheRoot().getNodeId()){   // αν πρόκειται για τη ρίζα υλοποιείται άλλος αλγόριθμος
            splitRoot();
        }
        else{
            int posOfEntry = NodesAndEntriesVisited.get(NodesAndEntriesVisited.size()-1); // θέση του entry στον γονέα κόμβο που δείχνει στν κόμβο που θα γίνει σπλιτ
            NodesAndEntriesVisited.remove(NodesAndEntriesVisited.size()-1);
            int idOfPreviousNode = NodesAndEntriesVisited.get(NodesAndEntriesVisited.size()-1); //id γονέα κόμβου


            Node nodeToSplit = indexFile.getNodeFromTheFile(nodeIdToSplit);
            ArrayList<Node> newNodes = nodeToSplit.split(); //οι δύο νέοι κόμβοι
            Node node1 = newNodes.get(0);
            Node node2 = newNodes.get(1);
            ArrayList<Entry> entries1 = node1.getEntries();
            ArrayList<Entry> entries2 = node2.getEntries();
            short nodeId1 = indexFile.createNewNode(nodeToSplit.getLevel());
            short nodeId2 = indexFile.createNewNode(nodeToSplit.getLevel());
            for (int j = 0; j < entries1.size(); j++) { //εισαγωγή των entries
                indexFile.addNewEntryInNode(nodeId1, entries1.get(j));
            }
            for (int j = 0; j < entries2.size(); j++) {
                indexFile.addNewEntryInNode(nodeId2, entries2.get(j));

            }
            //ορισμός των boundingBoxes των entries που θα δείχνουν στους νέους κόβους
            BoundingBox boundingBox1 = BoundingBox.findBoundingBoxToFitAllEntries(entries1);
            BoundingBox boundingBox2 = BoundingBox.findBoundingBoxToFitAllEntries(entries2);
            Entry entry1 = new Entry(boundingBox1,nodeId1); //τα entries που θα δείχνουν στους νέους κόμβους
            Entry entry2 = new Entry(boundingBox2,nodeId2);
            indexFile.deleteAnEntryAndFillItWithTheLastEntryOfTheNode((short)idOfPreviousNode,posOfEntry);
            indexFile.addNewEntryInNode(idOfPreviousNode,entry1); //εισαγωγή των entries στον κόμβο γονέα
            indexFile.addNewEntryInNode(idOfPreviousNode,entry2);
            Node previousNode = indexFile.getNodeFromTheFile(idOfPreviousNode);
            if(previousNode.getEntries().size()==indexFile.getMaxEntriesInNode()){ //έλεγχος για αν και ο γονέας χρειάζεται τώρα σπλιτ
                splitNode(NodesAndEntriesVisited); //αν ναι, κλήση του σπλιτ για τον γονέα παίρνοντας ως παράμετρο το μονοπάτι από ρίζα μέχρι τον γονέα
            }




        }
    }/** Mέθοδος που πραγματοποιεί το split της ρίζας */
    public void splitRoot(){
        Node root = indexFile.getTheRoot();
        ArrayList<Node> newNodes = root.split();
        Node node1 = newNodes.get(0);
        Node node2 = newNodes.get(1);
        ArrayList<Entry> entries1 = node1.getEntries();
        ArrayList<Entry> entries2 = node2.getEntries();
        short nodeId1 = indexFile.createNewNode(node2.getLevel());
        short nodeId2 = indexFile.createNewNode(node2.getLevel());
        short nodeId3 = indexFile.createNewNode(node2.getLevel()+1);
        for (int j = 0; j < entries1.size(); j++) {
            indexFile.addNewEntryInNode(nodeId1, entries1.get(j));
        }
        for (int j = 0; j < entries2.size(); j++) {
            indexFile.addNewEntryInNode(nodeId2, entries2.get(j));
        }
        BoundingBox boundingBox1 = BoundingBox.findBoundingBoxToFitAllEntries(entries1);
        BoundingBox boundingBox2 = BoundingBox.findBoundingBoxToFitAllEntries(entries2);

        Entry entry1 = new Entry(boundingBox1,nodeId1);
        Entry entry2 = new Entry(boundingBox2,nodeId2);
        indexFile.addNewEntryInNode(nodeId3,entry1);
        indexFile.addNewEntryInNode(nodeId3,entry2);
        indexFile.setTheRoot(nodeId3);


    }

    void print(){
        Node root = indexFile.getTheRoot();
        System.out.println("Root has ID: "+ root.getNodeId());
        ArrayList<Entry> entries = root.getEntries();
        System.out.println("Root has kids: "+entries.size());
        for(int i=0;i<entries.size();i++){
            System.out.println("Entry has childID: "+entries.get(i).getChildId());
        }
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


