import java.util.ArrayList;
import java.util.List;

public class Blockchain {

    private final List<Block> blocks;

    public Blockchain() {
        this.blocks = new ArrayList<>();
        this.blocks.add(new Block(1));
    }

    public void addBlock(Block block) {
        Block previousBlock = blocks.get(blocks.size() - 1);
        block.setHash(previousBlock.getHash());
        blocks.add(block);
    }

    public boolean isChainValid() {
        for (int i = 1; i < blocks.size(); i++) {
            Block currentBlock = blocks.get(i);
            Block previousBlock = blocks.get(i - 1);

            if (!currentBlock.getHash().equals(currentBlock.calculateHash())) {
                System.out.println("Block " + i + " has been tampered with.");
                return false;
            }

            if (!currentBlock.getHash().equals(previousBlock.getHash())) {
                System.out.println("Block " + i + " has an invalid previous hash.");
                return false;
            }
        }
        return true;
    }

    public void addTransacao(Transacao transacao) {
        this.blocks.get(blocks.size() - 1).addTransacao(transacao);
    }

    public boolean isLastBlockFull() {
        return getLastBlock().getNrTransacoes() == 5;
    }

    public Block getLastBlock() {
        return this.blocks.get(blocks.size() - 1);
    }
}
