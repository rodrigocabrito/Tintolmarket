import java.io.*;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Blockchain {

    private final List<Block> blocks;
    private final String BLOCKCHAIN_DIR = "./blockchain/";

    public Blockchain() {
        this.blocks = new ArrayList<>();
        this.blocks.add(new Block(1));
    }

    public void addBlock(Block block) {
        Block previousBlock = blocks.get(blocks.size() - 1);
        block.setHash(previousBlock.calculateHash());
        blocks.add(block);
    }

    public void updateBlockFile(Block block) throws IOException {
        // Write the block data to a new .blk file
        File blkFile = new File(BLOCKCHAIN_DIR, "block_" + block.getId() + ".blk");
        DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(blkFile)));
        byte[] blockData = block.toByteArray();
        outputStream.writeInt(blockData.length);
        outputStream.write(blockData);
        outputStream.close();
    }

    public boolean isChainValid() {
        if (blocks.size() != 1) {
            for (int i = 1; i < blocks.size(); i++) {
                Block currentBlock = blocks.get(i);
                Block previousBlock = blocks.get(i - 1);

                if (!currentBlock.getHash().equals(previousBlock.calculateHash())) {
                    System.out.println("Bloco com id " + i + " tem hash errado.");
                    return false;
                }
            }
        } else {
            return getLastBlock().getHash().equals(Arrays.toString(new byte[32]));
        }
        return true;
    }

    public void loadBlocks() {
        // Load all the .blk files from the blocks directory
        File blkDir = new File(BLOCKCHAIN_DIR);
        File[] blkFiles = blkDir.listFiles((dir, name) -> name.endsWith(".blk"));
        if (blkFiles != null) {
            // Read each .blk file and create a Block object from it
            for (File blkFile : blkFiles) {
                try {
                    RandomAccessFile file = new RandomAccessFile(blkFile, "r");

                    // Read the block data from the file
                    int blockDataLength = file.readInt();
                    byte[] blockData = new byte[blockDataLength];
                    file.readFully(blockData);

                    // Create a Block object from the data
                    Block block = Block.fromByteArray(blockData);

                    // Add the block to the blockchain
                    blocks.add(block);

                    file.close();
                } catch (IOException e) {
                    // Handle the exception
                }
            }
        }
    }

    public void addTransacao(Transacao transacao, Signature signature) throws IOException {
        Block lastBlock = getLastBlock();
        lastBlock.addTransacao(transacao);
        updateBlockFile(getLastBlock());

        if (isLastBlockFull()) {
            lastBlock.closeBlock(signature);
            updateBlockFile(getLastBlock());
            this.blocks.add(new Block(lastBlock.getId() + 1));
            lastBlock = getLastBlock();
            Block previousLastBlock = blocks.get(blocks.size() - 2);
            lastBlock.setHash(previousLastBlock.calculateHash());
        }
    }

    public boolean isLastBlockFull() {
        return getLastBlock().getNrTransacoes() == 5;
    }

    public Block getLastBlock() {
        return this.blocks.get(blocks.size() - 1);
    }

    public List<Block> getBlocks() {
        return this.blocks;
    }
}
