import java.util.ArrayList;

public class Main {

    public static void main(String[] args){
//        new ConvertOsm("map.osm"); //It transmits the osm file to csv
        int numberOfCoordinatesThatPointsHave= 2;
        new CreateDataFile("dataFile.csv", numberOfCoordinatesThatPointsHave); //creates the dataFile
        LinearMethods linearMethods= new LinearMethods();

        RStarTree rStarTree= new RStarTree(false); //Αν το δέντρο είναι ήδη έτοιμο, τότε πρέπει η παράμετρος να είναι true. Αν είναι false, τότε το δέντρο χτίζεται από την αρχή



//        rStarTree.create(); //insertion function
        rStarTree.bottomUp();


        //delete of a record
        ArrayList<Double> coordinatesOfARecordToDelete= new ArrayList<>();
        coordinatesOfARecordToDelete.add(41.4908673);
        coordinatesOfARecordToDelete.add(26.5244910);
        rStarTree.delete(coordinatesOfARecordToDelete);

        //boundingBox for RangeQuery
        ArrayList<Double> bounds = new ArrayList<>();
        bounds.add(41.50); //x1
        bounds.add(41.52); //x2
        bounds.add(26.51); //y1
        bounds.add(26.53); //y2
        BoundingBox boundingBox = new BoundingBox(bounds,numberOfCoordinatesThatPointsHave);
        System.out.println("RangeQuery");
        //RangeQuery with rStarTree
        ArrayList<RecordId> arrayListOfRangeQuery= rStarTree.rangeQuery(boundingBox);
        for(RecordId recordId: arrayListOfRangeQuery){
            System.out.println(recordId);
        }
        //arrayListOfRangeQuery= linearMethods.rangeQuery(boundingBox); //Method for linear rangeQuery

        System.out.println("------------------------------");




        System.out.println("K-nn");
        //point for knn
        ArrayList<Double> point= new ArrayList<>();
        point.add(41.52);
        point.add(26.51);
        int knnParameter= 4;
        //knn with rStarTree
        ArrayList<Record> arrayListOfKnn= rStarTree.knn(knnParameter, point);
        for(Record record: arrayListOfKnn){
            System.out.print(record);
        }
        //arrayListOfKnn= linearMethods.knnLinear(knnParameter, point); //Method for linear k-nn

        System.out.println("------------------------------");




        System.out.println("Skyline");
        //Skyline with RStarTree
        ArrayList<Record> arrayListOfSkyline=rStarTree.skyline();
        for(Record record: arrayListOfSkyline){
            System.out.print(record);
        }
        //arrayListOfSkyline= linearMethods.skylineBruteForce(); //Method for bruteForce skyline

    }

}