import java.io.Serializable;
import java.util.ArrayList;

public class Node implements Serializable {

    private short nodeId;
    private int level;
    private ArrayList<Entry> entries;
    private IndexFile indexFile;
    private int MAX_ENTRIES;
    private int MIN_ENTRIES;
    private short nodeIdCounter;



    public Node(short nodeId, int level, ArrayList<Entry> entries){
        this.indexFile= new IndexFile("indexFile.txt");

        this.level = level;
        this.entries = entries;
        this.nodeId = nodeId;
        this.MAX_ENTRIES= indexFile.getMaxEntriesInNode();
        this.MIN_ENTRIES= (int) (0.4*MAX_ENTRIES);
        this.nodeIdCounter= indexFile.getTotalNodes();
    }

    public Node(int level,ArrayList<Entry> entries){
        this.indexFile= new IndexFile("indexFile.txt");

        this.level = level;
        this.entries = entries;

        this.nodeIdCounter= indexFile.getTotalNodes();
        this.nodeId = this.nodeIdCounter;
        this.nodeIdCounter++;
    }


    public long getNodeId() {
        return nodeId;
    }
    public ArrayList<Entry> getEntries() {
        return entries;
    }
    public int getLevel() {
        return level;
    }
    public int getMaxEntries() {
        return MAX_ENTRIES;
    }
    public int getMinEntries() {
        return MIN_ENTRIES;
    }

    public void setNodeId(short nodeId) {
        this.nodeId = nodeId;
    }

    public void setEntries(ArrayList<Entry> entries) {
        this.entries = entries;
    }

    public void setLevel(int level) {
        this.level = level;
    }


    public void insertEntryInNode(Entry entry){
        entries.add(entry);
    }

    /*Algorithm Split
    Sl Invoke ChooseSplitAxis to determine the axis, perpendicular to which the split is performed
    s2 Invoke ChooseSplitIndex to determine the best distribution into two groups along that axis
    s3 Distribute the entries into two groups */
    public ArrayList<Node> split(){ // (SOS)όταν γίνεται το σπλιτ το arraylist entries περιέχει MAX_ENTRIES+1
        int splitIndex = chooseSplitAxis();
        ArrayList<Node> twoNewNodesAfterSplit = chooseSplitIndex(splitIndex);



        return twoNewNodesAfterSplit;

    }
     /*The R*-tree uses the following method to find good splits. Along each axis, the entries are first sorted by the lower
    value, then sorted by the upper value of then rectangles. For each sort M-2m+2 distributions of the M+1 entries into two
    groups are determined. where the k-th distribution (k =1, ,(M-2m+2)) is described as follows: The first group
    contains the first (m-1)+k entries, the second group contains the remaining entries */


    /*Algorithm ChooseSplitAxis
    CSAl For each axis
            sort the entries by the lower, then by the upper value of their rectangles and determine all
            distributions as described above. Compute S, the sum of all margin-values of the different distributions
        end
    CSA2 Choose the axis with the minimum S as split axis*/

    public int chooseSplitAxis(){
        int minPos = 0; //pos for the axis with the minimum S = sum of all margin-values of the different distributions
        double minSValue = Double.POSITIVE_INFINITY;
        ArrayList<Entry> entriesToBeSorted = this.entries;

        ArrayList<Entry> sortedEntriesByLowerValue;
        ArrayList<Entry> sortedEntriesByUpperValue;
        for(int i=0;i<IndexFile.getDimensions();i++){ //for every dimension (every axis)
            double S =0;
            sortedEntriesByLowerValue = Entry.sortByLowerValues(entriesToBeSorted,i); //first sort(by lower value)
            int numberOfDistributions = MAX_ENTRIES-2*MIN_ENTRIES+2;  //num of distributions = M-2*m+2
            for(int k=1;k<=numberOfDistributions;k++){ //for every distribution
                int numberOfEntriesInFirstGroup = MIN_ENTRIES-1+k; //m-1+k
                int numberOfEntriesInSecondGroup = sortedEntriesByLowerValue.size()-numberOfEntriesInFirstGroup; //the remaining
                ArrayList<Entry> firstGroup = new ArrayList<>();
                ArrayList<Entry> secondGroup = new ArrayList<>();

                for(int j=0;j<MAX_ENTRIES+1;j++){
                    if(j<numberOfEntriesInFirstGroup){
                        firstGroup.add(sortedEntriesByLowerValue.get(j));
                    }
                    else{
                        secondGroup.add(sortedEntriesByLowerValue.get(j));
                    }
                }
                BoundingBox boundingBoxForFirstGroup = BoundingBox.findBoundingBoxToFitAllEntries(firstGroup); //Bounding box for the entries of the first group
                BoundingBox boundingBoxForSecondGroup = BoundingBox.findBoundingBoxToFitAllEntries(secondGroup); //Bounding box for the entries of the second group
                S = S + boundingBoxForFirstGroup.getPerimeter() + boundingBoxForSecondGroup.getPerimeter();

            }
            sortedEntriesByUpperValue = Entry.sortByUpperValues(entriesToBeSorted,i); //second sort(by upper value)
            for(int k=1;k<=numberOfDistributions;k++){
                int numberOfEntriesInFirstGroup = MIN_ENTRIES-1+k;
                int numberOfEntriesInSecondGroup = sortedEntriesByUpperValue.size()-numberOfEntriesInFirstGroup;
                ArrayList<Entry> firstGroup = new ArrayList<>();
                ArrayList<Entry> secondGroup = new ArrayList<>();
                for(int j=0;j<MAX_ENTRIES+1;j++){
                    if(j<numberOfEntriesInFirstGroup){
                        firstGroup.add(sortedEntriesByUpperValue.get(j));
                    }
                    else{
                        secondGroup.add(sortedEntriesByUpperValue.get(j));
                    }
                }
                BoundingBox boundingBoxForFirstGroup = BoundingBox.findBoundingBoxToFitAllEntries(firstGroup);
                BoundingBox boundingBoxForSecondGroup = BoundingBox.findBoundingBoxToFitAllEntries(secondGroup);
                S = S + boundingBoxForFirstGroup.getPerimeter() + boundingBoxForSecondGroup.getPerimeter();
            }

            if(S<minSValue){
                minSValue = S;
                minPos = i;
            }

        }

        return minPos;
    }
    /*Algorithm ChooseSplitIndex
    CSIl Along the chosen split axIs, choose the distribution with the minimum overlap-value
    Resolve ties by choosing the distribution with minimum area-value */

    public ArrayList<Node> chooseSplitIndex(int splitIndex){
        double minOverlapValue = Double.POSITIVE_INFINITY;
        double minAreaValue = Double.POSITIVE_INFINITY;
        int numberOfEntriesInFirstGroupForTheBestDistribution=0;
        int numberOfEntriesInSecondGroupForTheBestDistribution=0;
        boolean IsBestDistributionSortedByUpperValue=true;


        ArrayList<Entry> entriesToBeSorted = this.entries;
        ArrayList<Entry> sortedEntriesByLowerValue;
        ArrayList<Entry> sortedEntriesByUpperValue;
        double overlap;
        double area;
        int numberOfDistributions = MAX_ENTRIES-2*MIN_ENTRIES+2;  //num of distributions = M-2*m+2
        sortedEntriesByLowerValue = Entry.sortByLowerValues(entriesToBeSorted,splitIndex);
        for(int k=1;k<=numberOfDistributions;k++){ //for every distribution
            int numberOfEntriesInFirstGroup = MIN_ENTRIES-1+k; //m-1+k
            int numberOfEntriesInSecondGroup = sortedEntriesByLowerValue.size()-numberOfEntriesInFirstGroup; //the remaining
            ArrayList<Entry> firstGroup = new ArrayList<>();
            ArrayList<Entry> secondGroup = new ArrayList<>();

            for(int j=0;j<MAX_ENTRIES+1;j++){
                if(j<numberOfEntriesInFirstGroup){
                    firstGroup.add(sortedEntriesByLowerValue.get(j));
                }
                else{
                    secondGroup.add(sortedEntriesByLowerValue.get(j));
                }
            }
            BoundingBox boundingBoxForFirstGroup = BoundingBox.findBoundingBoxToFitAllEntries(firstGroup); //Bounding box for the entries of the first group
            BoundingBox boundingBoxForSecondGroup = BoundingBox.findBoundingBoxToFitAllEntries(secondGroup); //Bounding box for the entries of the second group
            overlap = boundingBoxForFirstGroup.findOverlapWithOtherBox(boundingBoxForSecondGroup);
            area = boundingBoxForFirstGroup.getArea()+boundingBoxForSecondGroup.getArea();

            if(overlap<minOverlapValue){
                minOverlapValue = overlap;
                minAreaValue = area;
                numberOfEntriesInFirstGroupForTheBestDistribution = firstGroup.size();
                numberOfEntriesInSecondGroupForTheBestDistribution = secondGroup.size();
                IsBestDistributionSortedByUpperValue = false;
            }
            else if(overlap==minOverlapValue && area<minAreaValue){
                minOverlapValue = overlap;
                minAreaValue = area;
                numberOfEntriesInFirstGroupForTheBestDistribution = firstGroup.size();
                numberOfEntriesInSecondGroupForTheBestDistribution = secondGroup.size();
                IsBestDistributionSortedByUpperValue = false;
            }

        }
        sortedEntriesByUpperValue = Entry.sortByUpperValues(entriesToBeSorted,splitIndex); //second sort(by upper value)
        for(int k=1;k<=numberOfDistributions;k++){
            int numberOfEntriesInFirstGroup = MIN_ENTRIES-1+k;
            int numberOfEntriesInSecondGroup = sortedEntriesByUpperValue.size()-numberOfEntriesInFirstGroup;
            ArrayList<Entry> firstGroup = new ArrayList<>();
            ArrayList<Entry> secondGroup = new ArrayList<>();
            for(int j=0;j<MAX_ENTRIES+1;j++){
                if(j<numberOfEntriesInFirstGroup){
                    firstGroup.add(sortedEntriesByUpperValue.get(j));
                }
                else{
                    secondGroup.add(sortedEntriesByUpperValue.get(j));
                }
            }
            BoundingBox boundingBoxForFirstGroup = BoundingBox.findBoundingBoxToFitAllEntries(firstGroup);
            BoundingBox boundingBoxForSecondGroup = BoundingBox.findBoundingBoxToFitAllEntries(secondGroup);
            overlap = boundingBoxForFirstGroup.findOverlapWithOtherBox(boundingBoxForSecondGroup);
            area = boundingBoxForFirstGroup.getArea()+boundingBoxForSecondGroup.getArea();
            if(overlap<minOverlapValue){
                minOverlapValue = overlap;
                minAreaValue = area;
                numberOfEntriesInFirstGroupForTheBestDistribution = firstGroup.size();
                numberOfEntriesInSecondGroupForTheBestDistribution = secondGroup.size();
                IsBestDistributionSortedByUpperValue = true;
            }
            else if(overlap==minOverlapValue && area<minAreaValue){
                minOverlapValue = overlap;
                minAreaValue = area;
                numberOfEntriesInFirstGroupForTheBestDistribution = firstGroup.size();
                numberOfEntriesInSecondGroupForTheBestDistribution = secondGroup.size();
                IsBestDistributionSortedByUpperValue = false;
            }

        }
        //for best distribution
        ArrayList<Entry> bestFirstGroup = new ArrayList<>();
        ArrayList<Entry> bestSecondGroup = new ArrayList<>();
        if(IsBestDistributionSortedByUpperValue){
            for(int i=0;i<MAX_ENTRIES+1;i++){
                if(i<numberOfEntriesInFirstGroupForTheBestDistribution){
                    bestFirstGroup.add(sortedEntriesByUpperValue.get(i));
                }
                else{
                    bestSecondGroup.add(sortedEntriesByUpperValue.get(i));
                }
            }
        }
        else{
            for(int i=0;i<MAX_ENTRIES+1;i++){
                if(i<numberOfEntriesInFirstGroupForTheBestDistribution){
                    bestFirstGroup.add(sortedEntriesByLowerValue.get(i));
                }
                else{
                    bestSecondGroup.add(sortedEntriesByLowerValue.get(i));
                }
            }
        }
        ArrayList<Node> newNodes = new ArrayList<>();
        Node node1 = new Node(this.getLevel(),bestFirstGroup);
        Node node2 = new Node(this.getLevel(),bestSecondGroup);
        newNodes.add(node1);
        newNodes.add(node2);
        return newNodes;


    }


}
