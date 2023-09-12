public class Main {

    public static void main(String[] args){
        //new ConvertOsm("map.osm"); //It transmits the osm file to csv
        //new CreateDataFile("dataFile.csv", 2); //creates the dataFile

        ReadDataFile a= new ReadDataFile();


        System.out.println(a.getTheData(3510));//a test, in order to see if everything works ok

    }
}