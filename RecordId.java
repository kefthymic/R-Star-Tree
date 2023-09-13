public class RecordId {
    private short block;
    private int slot;

    public RecordId(short block, int slot){
        this.block=block;
        this.slot=slot;
    }

    public int getSlot() {
        return slot;
    }

    public short getBlock() {
        return block;
    }


    @Override
    public String toString() {
        return "Block: " + block + ", Slot: " + slot;
    }
}
