import java.io.Serializable;
import java.util.ArrayList;
public class BoundingBox implements Serializable {
    private ArrayList<Double> bounds;  //έχει τη μορφή (x1,x2,y1,y2,...)
    private int dimensions;
    private ArrayList<Double> center;
    private Double area;
    private Double perimeter;
    public BoundingBox(ArrayList<Double>bounds,int dimensions){
        this.dimensions = dimensions;
        for(int i=0;i<this.dimensions;i++){
            if(bounds.get(2*i+1)<bounds.get(2*i)){
                throw new IllegalArgumentException("high bound should be larger than low bound");
            }
        }
        this.bounds = bounds;
        this.center = new ArrayList<>();
        center = getCenter();
        this.area = getArea();
        this.perimeter = getPerimeter();
    }
    public ArrayList<Double> getBounds() {
        return bounds;
    }
    public void setBounds(ArrayList<Double> bounds) {
        this.bounds = bounds;
    }
    public int getDimensions() {
        return dimensions;
    }
    //επιστρέφει μόνο μία συντεταγμένη
    //dimension = 0 =>x      highBound = false =>x1, highBound = true =>x2
    //dimension = 1 =>y      highBound = false =>y1, highBound = true =>y2
    //etc
    public Double getBound(int dimension, boolean highBound){
        int pos = 2*dimension;
        if(highBound){
            pos = pos+1;
        }
        return bounds.get(pos);
    }
    public ArrayList<Double> getCenter() {
        center = new ArrayList<Double>();
        for (int i = 0; i < dimensions; i++) {
            center.add((getBound(i, true) + getBound(i, false)) / 2);
        }
        return center;
    }
    public Double getArea() {
        double tempArea = 1;
        for(int i=0;i<dimensions;i++){
            tempArea = tempArea * (getBound(i,true)-getBound(i,false));
        }
        return tempArea;
    }
    public Double getPerimeter() {
        double sum = 0;
        for(int i=0;i<dimensions;i++){
            sum = sum + (getBound(i,true)-getBound(i,false));
        }
        perimeter = 2 * sum;
        return perimeter;
    }
    /*μέθοδος που επιστρέφει true αν ένα bounding box αποτελεί σημείο, δλδ αν οι συντεταγμένες σε όλες τις διαστάσεις του πάνω και του
    κάτω ορίου συμπίπτουν
     */
    public boolean representsAPoint(){
        for(int i=0;i<dimensions;i++){
            if(!(this.getBound(i,false).equals(this.getBound(i,true)))){
                return false;
            }
        }
        return true;
    }
    /* μέθοδος που υπολογίζει το overlap μεταξύ 2 bounding boxes*/
    public double findOverlapWithOtherBox(BoundingBox boundingBox) {
        double overlap = 1;
        for(int i=0;i<dimensions;i++){
            if(boundingBox.getBound(i,false)>=this.getBound(i,true)){
                return 0;
            }
            else if(boundingBox.getBound(i,true)<this.getBound(i,false)){
                return 0;
            }
            else{
                double high = min(this.getBound(i,true),boundingBox.getBound(i,true));
                double low = max(this.getBound(i,false), boundingBox.getBound(i,false));
                overlap = overlap * (high-low);
            }
        }
        return overlap;
    }
    private static double min(double a, double b){
        if(a<=b){
            return a;
        }
        else{
            return b;
        }
    }
    private static double max(double a, double b){
        if(a>=b){
            return a;
        }
        else{
            return b;
        }
    }
    /* μέθοδος που ελέγχει αν ένα bounding box χρειάζεται να διευρυνθεί για να χωρέσει ένα νέο Entry*/
    public boolean needsEnlargementToInsertEntry(Entry entry){
        BoundingBox boundingBoxOfEntry = entry.getBoundingBox();
        for(int i=0;i<dimensions;i++){
            if(boundingBoxOfEntry.getBound(i,false)<this.getBound(i,false)){
                return true;
            }
            if(boundingBoxOfEntry.getBound(i,true)>this.getBound(i,true)){
                return true;
            }
        }
        return false;
    }
    /* μέθοδος που διευρύνει τα όρια ενός bounding Box για να προστεθεί ένα ακόμη entry*/
    public void enlargeBoundingBoxToInsertNewEntry(Entry entry){
        BoundingBox boundingBoxOfEntry = entry.getBoundingBox();
        ArrayList<Double> newBounds = new ArrayList<>();
        for(int i=0;i<dimensions;i++){
            newBounds.add(min(this.getBound(i,false),boundingBoxOfEntry.getBound(i,false)));
            newBounds.add(max(this.getBound(i,true),boundingBoxOfEntry.getBound(i,true)));
        }
        this.setBounds(newBounds);
    }


    /**Mέθοδος που επιστρέφει true αν το BoundingBox που δίνεται σαν παράμετρος βρίσκεται ολόκληρο
     * μέσα στο Bounding Box, αλλιώς επιστρέφει false */
    public boolean seeIfTheGivenBoundingIsAllInsideToThisBoundingBox(BoundingBox givenBoundingBox){
        for(int i=0;i<this.getDimensions();i++){
            if(this.getBound(i,false)> givenBoundingBox.getBound(i,false)){
                return false;
            }
            if(this.getBound(i,true)< givenBoundingBox.getBound(i,true)){
                return false;
            }
        }
        return true;
    }

    /** Μέθοδος που επιστρέφει true αν το BoundingBox βρίσκεται ολόκληρο μέσα στο Bounding Box που δίνεται σαν παράμετρος,
     * αλλιώς επιστρέφει false */
    public boolean seeIfThisBoundingBoxIsAllInsideToTheGivenBoundingBox(BoundingBox givenBoundingBox){

        for(int i=0;i<this.getDimensions();i++){
            if(this.getBound(i,false)< givenBoundingBox.getBound(i,false)){
                return false;
            }
            if(this.getBound(i,true)> givenBoundingBox.getBound(i,true)){
                return false;
            }
        }
        return true;
    }

    /** Μέθοδος που επιστρέφει true αν το BoundingBox έχει έστω ένα κοινό σημείο με το BoundingBox
     * που δίνεται ως παράμετρος. Αν δεν έχουν κανένα κοινό σημείο επιστρέφει false. */
    public boolean seeIfThereIsAtLeastACommonPoint(BoundingBox givenBoundingBox){
        for(int i=0;i<dimensions;i++) {
            if (givenBoundingBox.getBound(i, false) > this.getBound(i, true)) {
                return false;
            } else if (givenBoundingBox.getBound(i, true) < this.getBound(i, false)) {
                return false;
            }
        }
        return true;
    }

    /**Μέθοδος που επιστρέφει την απόσταση μεταξύ ενός σημείου και του BoundingBox.
     * Στις δύο διαστάσεις υπολογίζει την ελάχιστη απόσταση μεταξύ του ορθογωνίου και του σημείου,
     * ενώ σε παραπάνω διαστάσεις επιστρέφει την απόστση μεταξύ του σημείου και του κέντρου του BoundingBox*/
    public double findMinDistFromPoint(ArrayList<Double> pointCoordinates){
        double distance = 0;
        //σημείο με συντεταγμένες (x,y) και BoundingBox με συντεταγμένες (x1,x2,y1,y2)
        double x,y,x1,x2,y1,y2;
        x = pointCoordinates.get(0);
        y = pointCoordinates.get(1);
        x1 = this.getBound(0,false);
        x2 = this.getBound(0,true);
        y1 = this.getBound(1,false);
        y2 = this.getBound(1,true);
        if(this.getDimensions()==2){  //για δύο διαστάσεις
            if(x<x1){
                if(y<y1){
                    distance = Math.sqrt((x1-x)*(x1-x) + (y1-y)*(y1-y));
                }
                else if(y<=y2){
                    distance = x1-x;
                }
                else{
                    distance = Math.sqrt((x1-x)*(x1-x) + (y2-y)*(y2-y));
                }
            }
            else if(x<=x2){
                if(y<y1){
                    distance = y1-y;
                }
                else if(y<=y2){
                    distance = 0;
                }
                else{
                    distance = y-y2;
                }
            }
            else{
                if(y<y1){
                    distance = Math.sqrt((x2-x)*(x2-x) + (y1-y)*(y1-y));
                }
                else if(y<=y2){
                    distance = x-x2;
                }
                else{
                    distance = Math.sqrt((x2-x)*(x2-x) + (y2-y)*(y2-y));
                }
            }
            return distance;
        }
        else{  // για παραπάνω από δύο διαστσεις: υπολογίζεται η απόσταση από το κέντρο
            ArrayList<Double> centerCoordinates = this.getCenter();
            for(int i=0;i<this.getDimensions();i++){
                distance = distance + (pointCoordinates.get(i)-centerCoordinates.get(i))*(pointCoordinates.get(i)-centerCoordinates.get(i));
            }
            return Math.sqrt(distance);
        }
    }
    /**Μέθοδος που επιστρέφει την απόσταση Μανχάταν μεταξύ της κάτω δεξιάς γωνίας του BoundingBox και της αρχής των αξόνων Ο(0,...,0) */
    public double findMinDist(){
        double minDist = 0;
        for(int i=0;i<this.dimensions;i++){
            minDist = minDist + this.getBound(i,false);
        }
        return minDist;
    }


    public void printBounds(){
        for(int i=0;i<dimensions;i++){
            System.out.println(this.getBound(i,false)+","+this.getBound(i,true));
        }
    }
    /*μέθοδος που δέχεται ένα Arraylist με entries και επιστρέφει ένα boundingBox του οποίου τα
     * όρια περιέχουν όλα τα όρια των boundingBoxes των entries */
    public static BoundingBox findBoundingBoxToFitAllEntries(ArrayList<Entry>group){
        ArrayList<Double> boundsForNewBoundingBox = new ArrayList<>();
        //for(int i=0;i<group.get(0).getBoundingBox().getDimensions();i++){
        for(int i=0;i<IndexFile.getDimensions();i++){
            double lowerBound = Double.POSITIVE_INFINITY;
            double upperBound = Double.NEGATIVE_INFINITY;
            for(int j=0;j<group.size();j++){
                if(group.get(j).getBoundingBox().getBound(i,false)<lowerBound){
                    lowerBound = group.get(j).getBoundingBox().getBound(i,false);
                }
                if(group.get(j).getBoundingBox().getBound(i,true)>upperBound){
                    upperBound = group.get(j).getBoundingBox().getBound(i,true);
                }
            }
            boundsForNewBoundingBox.add(lowerBound);
            boundsForNewBoundingBox.add(upperBound);
        }
        BoundingBox boundingBoxToFitAllEntriesOfGroup = new BoundingBox(boundsForNewBoundingBox,IndexFile.getDimensions() );
        return boundingBoxToFitAllEntriesOfGroup;
    }
}
