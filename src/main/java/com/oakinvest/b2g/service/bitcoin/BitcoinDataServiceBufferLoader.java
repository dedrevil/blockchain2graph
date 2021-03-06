package com.oakinvest.b2g.service.bitcoin;

import com.oakinvest.b2g.util.bitcoin.buffer.BitcoinDataServiceBuffer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Bitcoin data service buffer loader.
 */
@Component
public class BitcoinDataServiceBufferLoader {

    /**
     * Bitcoin data service.
     */
    private final BitcoinDataService bitcoinDataService;

    /**
     * Buffer.
     */
    private final BitcoinDataServiceBuffer buffer;

    /**
     * Constructor.
     *
     * @param newBitcoinDataService bitcoind data service
     * @param newBuffer             buffer
     */
    public BitcoinDataServiceBufferLoader(final BitcoinDataService newBitcoinDataService, final BitcoinDataServiceBuffer newBuffer) {
        this.bitcoinDataService = newBitcoinDataService;
        this.buffer = newBuffer;
    }

    /**
     * Load a block in the buffer in an asynchronous way.
     *
     * @param blockToLoad block to load
     */
    @Async
    @SuppressWarnings("checkstyle:designforextension")
    public void loadBlockInBuffer(final int blockToLoad) {
        // We get the required data.
        // Total block count in bitcoind.
        Optional<Integer> blockCount = bitcoinDataService.getBlockCount();

        // if the block to load in buffer exists in bitcoind.
        if (blockCount.isPresent() && blockToLoad <= blockCount.get()) {
            // We load the block requested.
            bitcoinDataService.addBlockInBuffer(blockToLoad);
        }

        // We remove the useless block in buffer.
        buffer.purge(blockToLoad - 1);
    }

}
