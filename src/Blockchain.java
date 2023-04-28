import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Blockchain {

    private final List<Block> blocks;
    private final String BLOCKCHAIN_DIR = "./src/blockchain/";

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

                    /*
                    // Create an input stream to read the block data
                    ByteArrayInputStream bis = new ByteArrayInputStream(blockData);
                    DataInputStream dis = new DataInputStream(bis);
                    ObjectInputStream ois = new ObjectInputStream(bis);

                    // Read the block data from the input stream and create a Block object
                    String hash = dis.readUTF();
                    long id = dis.readLong();
                    int nrTransacoes = dis.readInt();
                    List<Transacao> transacoes = new ArrayList<>();
                    for (int i = 0; i < nrTransacoes; i++) {
                        String transacaoString = dis.readUTF();
                        transacoes.add(Transacao.fromString(transacaoString));
                    }

                    //Signature serverSignature = Signature.getInstance("SHA256withRSA");
                    //serverSignature.initSign(serverPrivateKey);

                    Signature assinatura = null;
                    try {
                        assinatura = (Signature) ois.readObject();
                    } catch (IOException | ClassNotFoundException e) {
                        // The signature is not present in the block data
                    }

                    Block block = new Block(hash, id, nrTransacoes, transacoes, assinatura);
                    */
                    blocks.add(block);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    // Close the input stream
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }








                /*
                try (FileInputStream fis = new FileInputStream(blkFile);
                     BufferedInputStream bis = new BufferedInputStream(fis);
                     DataInputStream ois = new DataInputStream(bis)) {

                    // Read the block data from the file and deserialize it
                    byte[] blockData = new byte[ois.readInt()];
                    ois.readFully(blockData);
                    Block block = Block.fromByteArray(blockData, serverPrivateKey);

                    // Add the block to the blockchain
                    blocks.add(block);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                */
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
