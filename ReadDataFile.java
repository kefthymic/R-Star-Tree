import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class ReadDataFile {
    private String dataFileBlocksName;
    private final int BLOCK_SIZE = 32 * 1024;
    private ArrayList<String> namesOfPlaces;

    public ReadDataFile() {
        this.dataFileBlocksName = "dataFileBlocks.txt";
        this.namesOfPlaces = new ArrayList<>();
        createTheArrayListOfNames();
    }

    private void createTheArrayListOfNames() {
        int sizeOfString;
        byte[] temp;
        byte[] block0;
        String tempString = null;
        short remainingSize;
        short numberOfNames;
        int help;

        block0 = getBlock((short) 0);
        remainingSize = ByteBuffer.wrap(block0, 2, 2).getShort();
        numberOfNames = ByteBuffer.wrap(block0, 13, 2).getShort();

        sizeOfString = BLOCK_SIZE - remainingSize - 13 - 2;

        temp = Arrays.copyOfRange(block0, 15, 15 + sizeOfString);
        try {
            tempString = new String(temp, "ISO-8859-7");
        } catch (UnsupportedEncodingException e) {
            System.out.println(e);
        }

        for (short i = 0; i < numberOfNames; i++) {
            help = tempString.indexOf("\n");
            this.namesOfPlaces.add(tempString.substring(0, help));
            tempString = tempString.substring(help + 1);
        }
    }

    /**
     * helpful private function that return the block with id==blockId
     */
    private byte[] getBlock(short blockId) {
        byte[] tempBlock = new byte[BLOCK_SIZE];
        int totalBlocks;
        try {
            RandomAccessFile file = new RandomAccessFile(this.dataFileBlocksName, "rw");

            file.read(tempBlock);//now the tempBlock is the block0

            totalBlocks = ByteBuffer.wrap(tempBlock, 4, 2).getShort();
            if (blockId < totalBlocks) {
                file.seek(blockId * BLOCK_SIZE);
                file.read(tempBlock); //now the tempBlock is block with id blockId
            }

            file.close();
        } catch (IOException e) {
            System.out.println(e);
        }

        return tempBlock;
    }

    private void updateTheFile(byte[] block){
        int targetBlockToWrite;
        try{
            RandomAccessFile file = new RandomAccessFile(this.dataFileBlocksName, "rw");

            targetBlockToWrite = ByteBuffer.wrap(block, 0, 2).getShort();
            file.seek((long) targetBlockToWrite * BLOCK_SIZE);
            file.write(block);

            file.close();
        }catch (IOException e){
            System.out.println(e);
        }
    }

    public int getCoordinates() {
        int temp;
        byte[] block0 = getBlock((short) 0);

        temp = (int) block0[10];

        return temp;

    }

    public Record getTheData(int blockId, int slotOffset) {

        byte[] block0;
        byte[] block;

        short totalBlocks;
        short sizeOfOneRecord;

        Record record;
        int position;
        short numberOfCoordinates;
        long nodeId;
        ArrayList<Double> coordinates;
        double temp;
        String name;
        int codeOfName;

        if (blockId < 1) {
            return null;
        }

        //Checks if the blockId is valid
        block0 = getBlock((short) 0);
        totalBlocks = ByteBuffer.wrap(block0, 4, 2).getShort();
        if (totalBlocks - 1 < blockId) {
            return null;
        }

        //Checks if the slotOffset is valid
        sizeOfOneRecord = ByteBuffer.wrap(block0, 8, 2).getShort();
        if ((BLOCK_SIZE - 2) * 1.0 / sizeOfOneRecord < slotOffset) {
            return null;
        }

        block = getBlock((short) (blockId));
        position = 2 + sizeOfOneRecord * slotOffset; //The first 2 bytes are used for the blockId

        nodeId = ByteBuffer.wrap(block, position, 8).getLong();
        position += 8;

        numberOfCoordinates = (short) block0[10];
        coordinates = new ArrayList<>();
        for (short i = 0; i < numberOfCoordinates; i++) {
            temp = ByteBuffer.wrap(block, position, 8).getDouble();
            coordinates.add(temp);
            position += 8;
        }

        codeOfName = ByteBuffer.wrap(block, position, 4).getInt();
        if (codeOfName == -1) {
            name = null;
        } else {
            name = this.namesOfPlaces.get(codeOfName);
        }

        record = new Record(nodeId, coordinates, name);

        return record;
    }


    public Record getTheData(int numberOfRecord){
        String tempString;
        int targetBlock;
        int slotOffset;
        int help;

        tempString= getTheTargetBlockAndTheSlotOffset(numberOfRecord);

        help = tempString.indexOf("&");

        targetBlock= Integer.parseInt(tempString.substring(0,help));
        slotOffset= Integer.parseInt(tempString.substring(help+1));

        return getTheData(targetBlock, slotOffset);
    }

    /**
     * It deletes a record from the file
     * The last element that was in the file, will take the empty position that will be created
     *
     * The function returns the coordinates of the lastElement that took the empty position
     * If the record that will be deleted is the last element, then the function returns null
     */
    public ArrayList<Double> deleteARecordFromTheFile(RecordId recordIdFromTheRecordToDelete){
        byte[] block0= getBlock((short) 0);
        byte[] temp;

        short totalRecords= getTotalNumberOfRecords();
        short numberOfRecordInTheLastBlock= getTheNumberOfRecordInTheLastBlock();

        totalRecords--;
        numberOfRecordInTheLastBlock--;

        temp = ByteBuffer.allocate(2).putShort(totalRecords).array(); //slotOfTotalRecords
        System.arraycopy(temp,0,block0,6,2);

        if(numberOfRecordInTheLastBlock==0){
            short numberOfBlocks= getTotalNumberOfBlocks();
            numberOfBlocks--;

            temp = ByteBuffer.allocate(2).putShort(numberOfBlocks).array(); //slotOfTotalBlocks
            System.arraycopy(temp,0,block0,4,2);

            numberOfRecordInTheLastBlock= (short) ((BLOCK_SIZE - 2)/getTheSizeOfOneRecord());
        }

        temp = ByteBuffer.allocate(2).putShort(numberOfRecordInTheLastBlock).array(); //slotOfTotalRecordsOfTheLastBlock
        System.arraycopy(temp,0,block0,11,2);

        Record lastRecordInTheDataFile= getTheData(totalRecords);
        Record recordToDelete= getTheData(recordIdFromTheRecordToDelete.getBlock(), recordIdFromTheRecordToDelete.getSlot());

        //see if it is the last element
        if(lastRecordInTheDataFile.toString().compareTo(recordToDelete.toString()) == 0){
            updateTheFile(block0);
            return null;
        }

        byte[] block= getBlock(recordIdFromTheRecordToDelete.getBlock());
        int offset= 2 + getTheSizeOfOneRecord()*recordIdFromTheRecordToDelete.getSlot();

        byte[] lastBlock = getBlock((short) (getTotalNumberOfBlocks()-1));

        String stringWithBlockAndSlotOfTheLastBlock= getTheTargetBlockAndTheSlotOffset(totalRecords);
        int help= stringWithBlockAndSlotOfTheLastBlock.indexOf("&");

        int blockOfTheLastRecordInTheDataFile= Integer.parseInt(stringWithBlockAndSlotOfTheLastBlock.substring(0, help));
        int tempSlot= Integer.parseInt(stringWithBlockAndSlotOfTheLastBlock.substring(help+1));
        int offsetOfTheLastRecordInTheDataFile = 2 + getTheSizeOfOneRecord()*tempSlot;


        //The nodeId is being copied
        temp = ByteBuffer.allocate(8).putLong(lastRecordInTheDataFile.getOsmNodeId()).array();
        System.arraycopy(temp,0,block,offset,8);
        offset+=8;

        //It deletes this data from the last block (It is putting zeros)
        temp = ByteBuffer.allocate(8).putLong(0).array();
        System.arraycopy(temp,0,lastBlock,offsetOfTheLastRecordInTheDataFile,8);
        offsetOfTheLastRecordInTheDataFile+=8;

        //The coordinates are being copied
        for(int i=0;i<getCoordinates();i++){//add the coordinates in the dataFile
            temp = ByteBuffer.allocate(8).putDouble(lastRecordInTheDataFile.getCoordinates().get(i)).array();
            System.arraycopy(temp,0,block,offset,8);
            offset+=8;

            //It deletes this data from the last block (It is putting zeros)
            temp = ByteBuffer.allocate(8).putLong(0).array();
            System.arraycopy(temp,0,lastBlock,offsetOfTheLastRecordInTheDataFile,8);
            offsetOfTheLastRecordInTheDataFile+=8;
        }


        //The position in the nameOfList, where the name of the record is being copied
        help= ByteBuffer.wrap(lastBlock, offsetOfTheLastRecordInTheDataFile,4).getInt();

        temp = ByteBuffer.allocate(4).putInt(help).array();//put the integer that shows the position of the name
        System.arraycopy(temp,0,block,offset,4);

        //It deletes this data from the last block (It is putting zeros)
        temp = ByteBuffer.allocate(4).putInt(0).array();
        System.arraycopy(temp,0,lastBlock,offsetOfTheLastRecordInTheDataFile,4);

        updateTheFile(block0);
        updateTheFile(block);
        updateTheFile(lastBlock);

        return lastRecordInTheDataFile.getCoordinates();
    }

    /**
     * It returns the targetBlock and the slotOffset of the numberOfRecord-th record
     *
     * @return a String with the above data. The form is the bellow:
     *              "targetBlock&slotOffset"
     */
    public String getTheTargetBlockAndTheSlotOffset(int numberOfRecord){
        byte[] block0= getBlock((short) 0);

        short totalBlocks;
        short totalRecords;
        short sizeOfOneRecord;
        int maxRecordsInOneBlock;
        int targetBlock=-1;
        int i;
        int slotOffset=-1;

        totalBlocks= ByteBuffer.wrap(block0, 4,2).getShort();
        totalRecords= ByteBuffer.wrap(block0, 6,2).getShort();
        sizeOfOneRecord= ByteBuffer.wrap(block0, 8,2).getShort();

        if(totalRecords-1<numberOfRecord){
            return null;
        }

        maxRecordsInOneBlock= (BLOCK_SIZE-2)/sizeOfOneRecord;

        if( (numberOfRecord+1)%maxRecordsInOneBlock==0 ){
            targetBlock= (numberOfRecord+1)/maxRecordsInOneBlock;
            slotOffset= maxRecordsInOneBlock-1;
        }
        else{
            for(i=1;i<totalBlocks && targetBlock==-1;i++){
                if(numberOfRecord-maxRecordsInOneBlock*i<0){
                    targetBlock=i;
                    slotOffset= numberOfRecord - maxRecordsInOneBlock*(i-1);
                }
            }
        }

        return targetBlock+"&"+slotOffset;
    }

    public short getTheSizeOfOneRecord() {
        byte[] block0 = getBlock((short) 0);
        short temp;

        temp = ByteBuffer.wrap(block0, 8, 2).getShort();

        return temp;
    }

    public short getTotalNumberOfBlocks() {
        short temp;
        byte[] block0 = getBlock((short) 0);

        return ByteBuffer.wrap(block0, 4, 2).getShort();
    }

    public short getTotalNumberOfRecords() {
        short temp;
        byte[] block0 = getBlock((short) 0);

        return ByteBuffer.wrap(block0, 6, 2).getShort();
    }

    public short getTheNumberOfRecordInTheLastBlock(){
        byte[] block0= getBlock((short) 0);

        return ByteBuffer.wrap(block0, 11, 2).getShort();
    }

}
