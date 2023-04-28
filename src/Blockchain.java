import java.io.*;
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Blockchain class. Represents a blockchain of blocks.
 * @author Rodrigo Cabrito 54455
 * @author João Costa 54482
 * @author João Fraga 44837
 */

public class Blockchain {

    private final String BLOCKCHAIN_DIR = "./blockchain/";
    private final List<Block> blocks;


    public Blockchain() {
        this.blocks = new ArrayList<>();
        this.blocks.add(new Block(1));
    }

    /**
     * Updates the .blk file with the information of the block.
     * @param block given block
     * @throws IOException if an error occurs writing to the .blk file
     */
    public void updateBlockFile(Block block) throws IOException {
        // Write the block data to a new .blk file
        File blkFile = new File(BLOCKCHAIN_DIR, "block_" + block.getId() + ".blk");
        DataOutputStream outputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(blkFile)));
        byte[] blockData = block.toByteArray();
        outputStream.writeInt(blockData.length);
        outputStream.write(blockData);
        outputStream.close();
    }

    /**
     * Verifies if the blockchain is valid.
     * @return {@code true} if the blockchain is valid, {@code false} otherwise
     */
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

    /**
     * Populates the blockchain with the blocks stored in the blockchain directory, specifically in the .blk files.
     * @param serverPrivateKey given server private key
     * @throws NoSuchAlgorithmException if an error occurs while parsing the file to a block.
     * @throws InvalidKeyException if an error occurs while parsing the file to a block.
     * @throws FileNotFoundException if the directory does not exist.
     */
    public void loadBlocks(PrivateKey serverPrivateKey) throws NoSuchAlgorithmException, InvalidKeyException, FileNotFoundException {
        // Load all the .blk files from the blocks directory
        File blkDir = new File(BLOCKCHAIN_DIR);
        File[] blkFiles = blkDir.listFiles();
        if (blkFiles != null) {

            // Read each .blk file and create a Block object from it
            for (File blkFile : blkFiles) {
                if (blkFile.getName().equals(blkFiles[0].getName())) {
                    blocks.remove(0); // remove first block
                }

                // Create an input stream to read from the .blk file
                DataInputStream inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(blkFile)));

                try {
                    // Read the length of the block data
                    int length = inputStream.readInt();

                    // Create a byte array to hold the block data
                    byte[] blockData = new byte[length];

                    // Read the block data from the input stream
                    inputStream.readFully(blockData);

                    Block block = Block.fromByteArray(blockData, serverPrivateKey);
                    if (block.getSignature() != null) {
                        Signature signature = block.getSignature();
                        signature.update(block.toByteArray());
                        block.setSignatureBytes(signature.sign());
                    }

                    blocks.add(block);

                } catch (IOException | SignatureException e) {
                    throw new RuntimeException(e);
                } finally {
                    // Close the input stream
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }
    }

    /**
     * Adds a transaction to the blockchain.
     * @param transacao given transaction
     * @param signature given server's signature.
     * @throws IOException if an error occurs while writing to the .blk file
     * @throws SignatureException if an error occurs while signing the last block of the blockchain.
     */
    public void addTransacao(Transacao transacao, Signature signature) throws IOException, SignatureException {
        Block lastBlock = getLastBlock();
        lastBlock.addTransacao(transacao);
        updateBlockFile(getLastBlock());

        if (isLastBlockFull()) {
            signature.update(lastBlock.toByteArray());
            lastBlock.closeBlock(signature.sign());
            updateBlockFile(getLastBlock());
            this.blocks.add(new Block(lastBlock.getId() + 1));
            lastBlock = getLastBlock();
            Block previousLastBlock = blocks.get(blocks.size() - 2);
            lastBlock.setHash(previousLastBlock.calculateHash());
            System.out.println("o bloco com id " + previousLastBlock.getId() + " tem hash: " + previousLastBlock.calculateHash());
            System.out.println("o bloco com id " + lastBlock.getId() + " tem hash: " + lastBlock.getHash());
        }
    }

    /**
     * Checks if the last block of the blockchain is full.
     * @return {@code true} if the last block of the blockchain is full, {@code false} otherwise
     */
    public boolean isLastBlockFull() {
        return getLastBlock().getNrTransacoes() == 5;
    }

    /**
     * Returns the last block of the blockchain.
     * @return the last block of the blockchain
     */
    public Block getLastBlock() {
        return this.blocks.get(blocks.size() - 1);
    }

    public List<Block> getBlocks() {
        return this.blocks;
    }
}
