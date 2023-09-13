import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class CreateDataFile {
    private File csvFile;
    private final int BLOCK_SIZE= 32*1024;
    private int coordinates;
    private String dataFileBlocksName;

    public CreateDataFile(File csvFile, int coordinates){
        this.csvFile=csvFile;
        this.coordinates= coordinates;
        this.dataFileBlocksName="dataFileBlocks.txt";
        creation();
    }
    public CreateDataFile(String nameCsvFile, int coordinates){
        this.csvFile= new File(nameCsvFile);
        this.coordinates=coordinates;
        this.dataFileBlocksName="dataFileBlocks.txt";
        creation();
    }

    /**
     * This constructor can be used, if the file has already been created and the user want to modify it
     */
    public CreateDataFile(String dataFileBlocksName){
        this.dataFileBlocksName=dataFileBlocksName;
    }

    /**
     * The block0 contains all the information about all the other blocks
     * More specific, it has:
     * 1. The blockId, which is 2 bytes
     * 2. The remainingSize that shows the free space into this block. It contains 2 bytes.
     * 3. The totalBlocks in the dataFile. It contains 2 bytes
     * 4. The totalRecords in the dataFile. It is initialized to 0. It contains 2 bytes
     * 5. The sizeOfOneRecord, which is the size of the data in one slot in the other blocks. It is equal to (Double.Bytes*numberOfCoordinates + Int.Bytes + Long.Bytes). It contains 2 bytes
     * 6. numberOfCoordinates, which shows the number of coordinates that are used in this dataFile. It is 2 bytes
     * 7. totalRecordsOfTheLastBlock, which shows how many records are stored in the last block. It is 2 bytes
     *
     * So the first 13 bytes are being used for these information.
     *
     * The other 2 bytes are used in order to show the numberOfNames of the records
     * All the other bytes are used to save the string that have all the names with their code.
     *      All the names are like this: "(number);name\n" , where the name is a number. All the names are saved in the string
     */
    private void createBlock0(){
        byte[] block0= new byte[BLOCK_SIZE];
        byte[] temp;
        short blockId=0;
        short help;

        temp = ByteBuffer.allocate(2).putShort(blockId).array(); //slotOfBlockId
        System.arraycopy(temp,0,block0,0,2);

        temp = ByteBuffer.allocate(2).putShort((short) (BLOCK_SIZE-13-2) ).array(); //slotOfRemainingSize. It is minus 13 because 13 bytes are used already and minus 2 for the slot of numberOfNames
        System.arraycopy(temp,0,block0,2,2);

        temp = ByteBuffer.allocate(2).putShort((short) 1).array(); //slotOfTotalBlocks
        System.arraycopy(temp,0,block0,4,2);

        temp = ByteBuffer.allocate(2).putShort((short) 0).array(); //slotOfTotalRecords
        System.arraycopy(temp,0,block0,6,2);

        help= (short) (Double.BYTES *this.coordinates + Integer.BYTES + Long.BYTES);
        temp = ByteBuffer.allocate(2).putShort(help).array(); //slotOfSizeOfOneRecord
        System.arraycopy(temp,0,block0,8,2);

        byte numberOfCoordinates= (byte) this.coordinates;
        block0[10]= numberOfCoordinates;

        temp = ByteBuffer.allocate(2).putShort((short) 0).array(); //slotOfTotalRecordsOfTheLastBlock. It is initialized to 0
        System.arraycopy(temp,0,block0,11,2);


        temp = ByteBuffer.allocate(2).putShort((short) 0).array(); //slotOfNumberOfNames. It is initialized to 0, because there is not any record
        System.arraycopy(temp,0,block0,13,2);


        updateTheFile(block0);
    }

    /**
     * This function creates a newBlock and added it after the previous block. It also updates the information in the block0
     */
    private void createNewBlock(){
        byte[] block0;
        byte[] newBlock;
        byte[] temp;
        short blockId;

        block0=getBlock((short)0);
        blockId = ByteBuffer.wrap(block0, 4, 2).getShort(); //First we need to read the number of totalBlocks in order to make the right blockId

        newBlock= new byte[BLOCK_SIZE];
        temp = ByteBuffer.allocate(2).putShort(blockId).array(); //slotOfBlockId
        System.arraycopy(temp,0,newBlock,0,2);


        temp = ByteBuffer.allocate(2).putShort(++blockId).array(); //In order to update the number of the totalBlocks in block0
        System.arraycopy(temp,0,block0,4,2);


        temp = ByteBuffer.allocate(2).putShort((short) 0).array(); //slotOfTotalRecordsOfTheLastBlock. It is initialized to 0
        System.arraycopy(temp,0,block0,11,2);

        updateTheFile(block0);
        updateTheFile(newBlock);
    }

    /**
     * This function writes the block to the correct position in the file
     */
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

    private short totalSizeInBytesOfLastBlock(){
        byte[] block0= getBlock((short)0);
        short sizeOfOneRecord;
        short totalRecordsOfTheLastBlock;
        short temp;


        sizeOfOneRecord= ByteBuffer.wrap(block0, 8, 2).getShort();
        totalRecordsOfTheLastBlock= ByteBuffer.wrap(block0, 11, 2).getShort();

        temp= (short) (sizeOfOneRecord*totalRecordsOfTheLastBlock);
        temp+=2; //Because the first 2 bytes are used for the blockId


        return temp;
    }

    /**
     * This function checks if there is space in order to save the new data to the current block
     * It returns: -1, if there is no space in the current block
     *             the offset in the block to where to add the new element
     */
    private short seeIfHasSpaceToAddNewElements(){
        byte[] block0;
        int currentSizeOfLastBlock;
        short sizeOfOneRecord;

        block0= getBlock((short)0);

        sizeOfOneRecord= ByteBuffer.wrap(block0, 8, 2).getShort();

        currentSizeOfLastBlock= totalSizeInBytesOfLastBlock();

        if (currentSizeOfLastBlock + sizeOfOneRecord> BLOCK_SIZE){
            return -1;
        }

        return totalSizeInBytesOfLastBlock();
    }

    /**
     * returns the block in order the data be stored
     */
    private byte[] getTheBlockToAdd(){
        byte[] block0;
        short currentBlock;

        block0= getBlock((short)0);
        currentBlock = (short) (ByteBuffer.wrap(block0, 4, 2).getShort() -1); //Minus 1, because of 0-base-numbering

        return getBlock(currentBlock);
    }

    /**
     * This is the function which insert all the data from the csv to the dataFileBlocks.txt
     * First, it creates the block0, that contains the information of the file and after then, a new block
     * After, it opens the csv file and starts to read line by line the data.
     *      It checks if there is extra space in the current block to add the data. If not, it creates a new block
     * Then, the data is added to the block and in the end the file be updated
     */
    private void creation(){
        byte[] block0;
        byte[] temp;
        byte[] block;
        short offset;
        String tempString;
        int help;
        long longHelp;
        double helpDouble;
        short totalRecords;
        short totalRecordsOfTheLastBlock;
        short numberOfNames;
        short remainingSize;
        int sizeOfString;

        createBlock0(); //creates the block0 which contains all the information that will be needed
        createNewBlock(); // creates the first block to start add the data from the csv to the dataFileBlocks

        try{
            BufferedReader reader = new BufferedReader(new FileReader(csvFile.getName()));
            String line;
            while ((line = reader.readLine()) != null) {

                offset = seeIfHasSpaceToAddNewElements();
                if(offset == -1){
                    createNewBlock();
                    offset=2; // The first 2 bytes are used for the blockId
                }

                block0= getBlock((short) 0);
                block = getTheBlockToAdd();

                //for the nodeId
                help= line.indexOf(",");
                tempString= line.substring(0, help);
                line=line.substring(help+1);

                longHelp= Long.parseLong(tempString);
                temp = ByteBuffer.allocate(8).putLong(longHelp).array();
                System.arraycopy(temp,0,block,offset,8);
                offset+=8;

                for(int i=0;i<this.coordinates;i++){//add the coordinates in the dataFile
                    help= line.indexOf(",");
                    tempString= line.substring(0, help);
                    line=line.substring(help+1);

                    helpDouble= Double.parseDouble(tempString);
                    temp = ByteBuffer.allocate(8).putDouble(helpDouble).array();

                    System.arraycopy(temp,0,block,offset,8);
                    offset+=8;
                }

                remainingSize= ByteBuffer.wrap(block0, 2, 2).getShort();

                if(line.equals("") || remainingSize<line.length()){//add the name in the dataFile
                    temp = ByteBuffer.allocate(4).putInt(-1).array();
                    System.arraycopy(temp,0,block,offset,4);
                }
                else{
                    sizeOfString= BLOCK_SIZE-remainingSize-13-2;

                    temp= Arrays.copyOfRange(block0,15,15+sizeOfString);
                    try{
                        tempString= new String(temp,"ISO-8859-7");
                    }catch (UnsupportedEncodingException e){
                        System.out.println(e);
                    }

                    numberOfNames= ByteBuffer.wrap(block0,13,2).getShort();

//                    line= numberOfNames+";"+line+"\n";
                    line= line+"\n";
                    tempString+=line;
                    temp = tempString.getBytes("ISO-8859-7");


                    System.arraycopy(temp,0,block0,15,temp.length);

                    temp= ByteBuffer.allocate(2).putShort((short) (BLOCK_SIZE-13-2-temp.length) ).array();//update the remainingSize
                    System.arraycopy(temp,0,block0,2,2);

                    numberOfNames++;
                    temp= ByteBuffer.allocate(2).putShort(numberOfNames).array();//update the numberOfNames
                    System.arraycopy(temp,0,block0,13,2);

                    temp = ByteBuffer.allocate(4).putInt(numberOfNames-1).array();//put the integer that shows the position of the name
                    System.arraycopy(temp,0,block,offset,4);

                }

                totalRecords= ByteBuffer.wrap(block0,6,2).getShort();
                totalRecords++;
                temp = ByteBuffer.allocate(2).putShort(totalRecords).array();//update the totalRecords
                System.arraycopy(temp,0,block0,6,2);

                totalRecordsOfTheLastBlock = ByteBuffer.wrap(block0,11,2).getShort();
                totalRecordsOfTheLastBlock++;
                temp = ByteBuffer.allocate(2).putShort(totalRecordsOfTheLastBlock).array();//update the totalRecordsOfTheLastBlock
                System.arraycopy(temp,0,block0,11,2);

                updateTheFile(block0);
                updateTheFile(block);

            }

            reader.close();

        } catch (IOException e){
            System.out.println(e);
        }
    }

    /**
     * This function returns the block with id==blockId
     */
    private byte[] getBlock(short blockId){
        byte[] tempBlock = new byte[BLOCK_SIZE];
        int totalBlocks;
        try{
            RandomAccessFile file = new RandomAccessFile(this.dataFileBlocksName, "rw");

            file.read(tempBlock);//now the tempBlock is the block0

            totalBlocks=ByteBuffer.wrap(tempBlock,4,2).getShort();
            if(blockId<totalBlocks){
                file.seek(blockId*BLOCK_SIZE);
                file.read(tempBlock); //now the tempBlock is block with id blockId
            }

            file.close();
        }catch (IOException e){
            System.out.println(e);
        }

        return tempBlock;
    }
}
