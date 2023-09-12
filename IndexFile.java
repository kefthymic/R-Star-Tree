import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class IndexFile {
    private final int BLOCK_SIZE= 32*1024;
    private static int dimensions;
    private int coordinates;
    private ReadDataFile readDataFile;
    private String indexFileName;

    public IndexFile(){
        this.readDataFile= new ReadDataFile();
        dimensions= readDataFile.getCoordinates();
        this.coordinates= readDataFile.getCoordinates();
        this.indexFileName="indexFile.txt";
        createBlock0();
    }

    /**
     * It is used in the Node Class
     *
     * @param indexFileName the name of the file with the R*-tree
     */
    public IndexFile(String indexFileName){
        this.readDataFile= new ReadDataFile();
        dimensions= readDataFile.getCoordinates();
        this.coordinates= readDataFile.getCoordinates();
        this.indexFileName= indexFileName;
    }

    /**
     * The block0 contains all the information about all the other blocks
     * More specific, it has:
     * 1. The blockId, which is 2 bytes
     * 2. The level that shows the level where this node is in the R*-tree. It contains 4 bytes.
     * 3. The howManyEntries that shows the number of entries this node has. It contains 4 bytes
     *
     * So the first 10 bytes are being used for these information.
     *
     * The other bytes are used in order to represent the entries.
     * More specific, it has:
     * 1. The blockOfTheRecordId is used to represent the entryOfLeaf and shows in which block the entry is stored in the dataFile. It is 2 bytes
     * 2. The slotOfTheRecordId is used to represent the entryOfLeaf and shows in which slot of the block the entry is stored in the dataFile. It is 4 bytes
     * 3. The childId is used to represent the nodeId of the node that this entry shows. If it is a entryOfLeaf it will have value of -1. It is 8 bytes
     * 4. for the above 2*coordinates*8 bytes will be stored an array of doubles that shoes the bounds of the boundingBox
     *      These are the information for one entry. In order to take the information of all the entries, the sizeOfOneEntry took into consideration
     */
    private void createBlock0(){

        byte[] block0= new byte[BLOCK_SIZE];
        byte[] temp;
        short blockId=0;
        int sizeOfOneEntry;
        int maxEntriesInOneNode;

        temp = ByteBuffer.allocate(2).putShort(blockId).array(); //slotOfBlockId
        System.arraycopy(temp,0,block0,0,2);

        temp = ByteBuffer.allocate(4).putInt( 1).array(); //slotOfTotalBlocks
        System.arraycopy(temp,0,block0,2,4);

        temp = ByteBuffer.allocate(4).putInt(-1).array(); //slotWhereIsTheRoot
        System.arraycopy(temp,0,block0,6,4);

        byte numberOfCoordinates= (byte) this.coordinates;
        block0[10]= numberOfCoordinates;

        sizeOfOneEntry = Short.BYTES + Integer.BYTES + Long.BYTES + 2*this.coordinates*Double.BYTES; //2 bytes for block, 4 bytes for slot, 8 bytes for child, 8 bytes for every element in the bounds[2*coordinates]
        temp = ByteBuffer.allocate(4).putInt(sizeOfOneEntry).array(); //slotSizeOfOneEntry
        System.arraycopy(temp,0,block0,11,4);

        maxEntriesInOneNode = (BLOCK_SIZE-10)/sizeOfOneEntry; //minus 10, because the first 10 bytes store information for the node
        temp = ByteBuffer.allocate(4).putInt(maxEntriesInOneNode).array(); //slotMaxEntriesInOneNode
        System.arraycopy(temp,0,block0,15,4);

        updateTheFile(block0);
    }

    /**
     *  It creates a new node in the file
     *
     * @param level is the level where the new node will be in the R*-tree
     * @return it returns the blockId that the node took
     */
    public short createNewNode(int level){
        byte[] block0;
        byte[] newBlock;
        byte[] temp;
        short blockId;

        block0=getBlock((short)0);
        blockId = (short) ByteBuffer.wrap(block0, 2, 4).getInt(); //First we need to read the number of totalBlocks in order to make the right blockId

        newBlock= new byte[BLOCK_SIZE];
        temp = ByteBuffer.allocate(2).putShort(blockId).array(); //slotOfBlockId
        System.arraycopy(temp,0,newBlock,0,2);


        temp = ByteBuffer.allocate(4).putInt(++blockId).array(); //In order to update the number of the totalBlocks in block0
        System.arraycopy(temp,0,block0,2,4);

        temp = ByteBuffer.allocate(4).putInt(level).array(); //slotOfLevel
        System.arraycopy(temp,0,newBlock,2,4);

        temp = ByteBuffer.allocate(4).putInt(0).array(); //slotOfHowManyEntries
        System.arraycopy(temp,0,newBlock,6,4);

        updateTheFile(block0);
        updateTheFile(newBlock);

        return (short) (blockId-1);
    }

    /**
     *  It creates a new node in the file. If any split will be done, then the node will need to delete all the entries and store some others
     *  For this reason, we use the blockId to modify it
     */
    public void createNewNode(short blockId, int level){
        byte[] block0;
        byte[] newBlock;
        byte[] temp;

        block0=getBlock((short)0);

        newBlock= new byte[BLOCK_SIZE];
        temp = ByteBuffer.allocate(2).putShort(blockId).array(); //slotOfBlockId
        System.arraycopy(temp,0,newBlock,0,2);

        temp = ByteBuffer.allocate(4).putInt(level).array(); //slotOfLevel
        System.arraycopy(temp,0,newBlock,2,4);

        temp = ByteBuffer.allocate(4).putInt(0).array(); //slotOfHowManyEntries
        System.arraycopy(temp,0,newBlock,6,4);

        updateTheFile(block0);
        updateTheFile(newBlock);
    }

    /**
     * This function adds a new entry in a node
     *
     * @param blockId the block where the new entry will be stored
     * @param entry the entry that want to add
     *
     * @return false, if the node is full and the addition could not be done
     *         true, otherwise
     */
    public boolean addNewEntryInNode(int blockId, Entry entry){
        byte[] block0= getBlock((short) 0);
        byte[] block= getBlock((short) blockId);
        byte[] temp;
        int offset;
        int sizeOfOneEntry;

        int maxEntriesInOneNode= ByteBuffer.wrap(block0, 15, 4).getInt();
        int howManyEntries= ByteBuffer.wrap(block,6,4).getInt();

        if(howManyEntries==maxEntriesInOneNode){
            return false;
        }


        sizeOfOneEntry= ByteBuffer.wrap(block0, 11, 4).getInt();


        offset= sizeOfOneEntry*howManyEntries +10; //in order to find the correct position to add the new entry


        howManyEntries++;
        temp = ByteBuffer.allocate(4).putInt(howManyEntries).array(); //update the slotOfHowManyEntries
        System.arraycopy(temp,0,block,6,4);

        long childId= entry.getChildId();
        BoundingBox boundingBox= entry.getBoundingBox();
        ArrayList<Double> arrayListOfBoundingBox= boundingBox.getBounds();

        short blockFromRecordId=-1;
        int slotOfRecordId=-1;

        if(childId==-1){
            EntryOfLeaf entryOfLeaf = (EntryOfLeaf) entry;
            RecordId recordId= entryOfLeaf.getRecordId();
            blockFromRecordId= recordId.getBlock();
            slotOfRecordId= recordId.getSlot();
        }

        temp = ByteBuffer.allocate(2).putShort(blockFromRecordId).array(); //slotOfBlockFromRecordId
        System.arraycopy(temp,0,block,offset,2);

        offset+=2;
        temp = ByteBuffer.allocate(4).putInt(slotOfRecordId).array(); //slotOfSlotOfRecordId
        System.arraycopy(temp,0,block,offset,4);


        offset+=4;
        temp = ByteBuffer.allocate(8).putLong(childId).array(); //slotOfChildId
        System.arraycopy(temp,0,block,offset,8);

        for(int i=0;i<2*this.coordinates;i++){
            offset+=8;
            temp = ByteBuffer.allocate(8).putDouble(arrayListOfBoundingBox.get(i)).array(); //slotsOfArrayListOfBoundingBox
            System.arraycopy(temp,0,block,offset,8);
        }

        updateTheFile(block);

        return true;
    }


    public void setTheRoot(int root){
        byte[] block0= getBlock((short) 0);
        byte[] temp;

        temp = ByteBuffer.allocate(4).putInt(root).array(); //slotOfWhereIsTheRoot
        System.arraycopy(temp,0,block0,6,4);

        updateTheFile(block0);
    }
    /**
     * This function is useful when someone want to read the file.
     * It gives the block, where the root is stored
     * The nodeId where the node is stored is in the block0
     */
    public Node getTheRoot(){
        byte[] block0= getBlock((short) 0);
        int whereIsTheRoot= ByteBuffer.wrap(block0, 6, 4).getInt();


        return getNodeFromTheFile(whereIsTheRoot);
    }

    public Node getNodeFromTheFile(int nodeId){
        byte[] nodeInBytes= getBlock((short) nodeId);
        byte[] block0= getBlock((short) 0);
        int sizeOfOneEntry= ByteBuffer.wrap(block0, 11, 4).getInt();
        int level;
        int howManyEntries;
        long childId;
        ArrayList<Double> arrayListForBoundingBox;
        ArrayList<Entry> allTheEntries;
        int tempOffsetOfEntries;
        int tempOffsetForBounds;
        double tempDouble;
        BoundingBox boundingBox;
        Entry entry;
        RecordId recordId;
        short blockForRecordId;
        int slotOfRecordId;

        level= ByteBuffer.wrap(nodeInBytes, 2, 4).getInt();
        howManyEntries= ByteBuffer.wrap(nodeInBytes, 6, 4).getInt();


        allTheEntries= new ArrayList<>();
        tempOffsetOfEntries= 10;
        for(int i=0;i<howManyEntries;i++){ //We do the same process for all the entries that are stored in the node

            childId= ByteBuffer.wrap(nodeInBytes, tempOffsetOfEntries+6, 8).getLong();

            arrayListForBoundingBox= new ArrayList<>();
            tempOffsetForBounds= tempOffsetOfEntries + 6 + 8;
            for(int j=0;j<2*this.coordinates;j++){
                tempDouble= ByteBuffer.wrap(nodeInBytes, tempOffsetForBounds, 8).getDouble();
                arrayListForBoundingBox.add(tempDouble);
                tempOffsetForBounds+=8;
            }

            boundingBox= new BoundingBox(arrayListForBoundingBox, this.coordinates);

            if(childId==-1){
                blockForRecordId= ByteBuffer.wrap(nodeInBytes, tempOffsetOfEntries, 2).getShort();
                slotOfRecordId= ByteBuffer.wrap(nodeInBytes, tempOffsetOfEntries+2, 4).getInt();
                recordId= new RecordId(blockForRecordId, slotOfRecordId);

                entry= new EntryOfLeaf(recordId, boundingBox, childId);
            }
            else{
                entry= new Entry(boundingBox, childId);
            }

            allTheEntries.add(entry);
            tempOffsetOfEntries+=sizeOfOneEntry;
        }

        Node node= new Node((short) nodeId, level, allTheEntries);


        return node;
    }

    /**
     * This function writes the block to the correct position in the file
     */
    private void updateTheFile(byte[] block){
        int targetBlockToWrite;
        try{
            RandomAccessFile file = new RandomAccessFile(this.indexFileName, "rw");

            targetBlockToWrite = ByteBuffer.wrap(block, 0, 2).getShort();
            file.seek((long) targetBlockToWrite * BLOCK_SIZE);
            file.write(block);

            file.close();
        }catch (IOException e){
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
            RandomAccessFile file = new RandomAccessFile(this.indexFileName, "rw");

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

    public int getMaxEntriesInNode(){
        byte[] block0= getBlock((short) 0);
        int maxEntriesInNode= ByteBuffer.wrap(block0, 15, 4).getInt();

        return maxEntriesInNode;
    }

    public short getTotalNodes(){
        byte[] block0= getBlock((short) 0);
        int totalNodes= ByteBuffer.wrap(block0, 2, 4).getInt();

        return (short) totalNodes;
    }

    public int getCoordinates() {
        int temp;
        byte[] block0 = getBlock((short) 0);

        temp = (int) block0[10];

        return temp;

    }

    public static int getDimensions() {
        return dimensions;
    }

}