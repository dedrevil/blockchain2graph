package com.oakinvest.b2g.util.bitcoin.batch;

import com.oakinvest.b2g.domain.bitcoin.BitcoinBlock;
import com.oakinvest.b2g.repository.bitcoin.BitcoinAddressRepository;
import com.oakinvest.b2g.repository.bitcoin.BitcoinBlockRepository;
import com.oakinvest.b2g.repository.bitcoin.BitcoinRepositories;
import com.oakinvest.b2g.repository.bitcoin.BitcoinTransactionOutputRepository;
import com.oakinvest.b2g.service.StatusService;
import com.oakinvest.b2g.service.bitcoin.BitcoinDataService;
import com.oakinvest.b2g.service.bitcoin.BitcoinDataServiceBufferLoader;
import com.oakinvest.b2g.util.bitcoin.mapper.BitcoindToDomainMapper;
import org.mapstruct.factory.Mappers;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Optional;

/**
 * Bitcoin import batch - abstract model.
 * Created by straumat on 27/02/17.
 */
public abstract class BitcoinBatchTemplate {

    /**
     * How many milli seconds in one second.
     */
    private static final float MILLISECONDS_IN_SECONDS = 1000F;

    /**
     * Log separator.
     */
    private static final String LOG_SEPARATOR = "===================================";

    /**
     * Pause to make when there is no block to process (1 second).
     */
    private static final int PAUSE_WHEN_NO_BLOCK_TO_PROCESS = 1000;

    /**
     * Mapper.
     */
    private final BitcoindToDomainMapper mapper = Mappers.getMapper(BitcoindToDomainMapper.class);

    /**
     * Bitcoin block repository.
     */
    private final BitcoinBlockRepository blockRepository;

    /**
     * Bitcoin address repository.
     */
    private final BitcoinAddressRepository addressRepository;

    /**
     * Bitcoin transaction output repository.
     */
    private final BitcoinTransactionOutputRepository transactionOutputRepository;

    /**
     * Bitcoin data service.
     */
    private final BitcoinDataService bitcoinDataService;

    /**
     * Bitcoind data service buffer loader.
     */
    private final BitcoinDataServiceBufferLoader bitcoinDataServiceBufferLoader;

    /**
     * Status service.
     */
    private final StatusService status;

    /**
     * Session factory.
     */
    private final SessionFactory sessionFactory;

    /**
     * Neo4j session.
     */
    private Session session;

    /**
     * time of the start of the batch.
     */
    private long batchStartTime;

    /**
     * Constructor.
     *
     * @param newBitcoinRepositories            bitcoin repositories
     * @param newBitcoinDataService             bitcoin data service
     * @param newBitcoinDataServiceBufferLoader bitcoind data service buffer loader
     * @param newStatusService                  status service
     * @param newSessionFactory                 session factory
     */
    protected BitcoinBatchTemplate(final BitcoinRepositories newBitcoinRepositories, final BitcoinDataService newBitcoinDataService, final BitcoinDataServiceBufferLoader newBitcoinDataServiceBufferLoader, final StatusService newStatusService, final SessionFactory newSessionFactory) {
        this.addressRepository = newBitcoinRepositories.getBitcoinAddressRepository();
        this.blockRepository = newBitcoinRepositories.getBitcoinBlockRepository();
        this.transactionOutputRepository = newBitcoinRepositories.getBitcoinTransactionOutputRepository();
        this.bitcoinDataService = newBitcoinDataService;
        this.bitcoinDataServiceBufferLoader = newBitcoinDataServiceBufferLoader;
        this.status = newStatusService;
        this.sessionFactory = newSessionFactory;
    }

    /**
     * Initialize sessions.
     */
    @PostConstruct
    public final void loadSession() {
        session = sessionFactory.openSession();
    }

    /**
     * Returns the elapsed time of the batch.
     *
     * @return elapsed time of the batch.
     */
    private float getElapsedTime() {
        return (System.currentTimeMillis() - batchStartTime) / MILLISECONDS_IN_SECONDS;
    }

    /**
     * Execute the batch.
     */
    @Transactional
    @Scheduled(fixedDelay = 1)
    @SuppressWarnings("checkstyle:designforextension")
    public void execute() {
        batchStartTime = System.currentTimeMillis();
        addLog(LOG_SEPARATOR);
        try {
            // We retrieve the block to process.
            Optional<Integer> blockHeightToProcess = getBlockHeightToProcess();

            // If there is a block to process.
            if (blockHeightToProcess.isPresent()) {
                // Process the block.
                addLog("Starting to process block " + getFormattedBlockHeight(blockHeightToProcess.get()));
                Optional<BitcoinBlock> blockToProcess = processBlock(blockHeightToProcess.get());

                // If the process ended well.
                blockToProcess.ifPresent((BitcoinBlock bitcoinBlock) -> {
                    // Before saving it, we start to load the next block to load in the buffer.
                    bitcoinDataServiceBufferLoader.loadBlockInBuffer(blockHeightToProcess.get() + 1);

                    // If the block has been well processed, we change the state and we save it.
                    addLog("Saving block data");
                    getBlockRepository().save(bitcoinBlock);
                    addLog("Block " + bitcoinBlock.getFormattedHeight() + " processed in " + getElapsedTime() + " secs");
                    getStatus().setImportedBlockCount(bitcoinBlock.getHeight());
                });
            } else {
                // If there is nothing to process.
                addLog("No block to process");
                Thread.sleep(PAUSE_WHEN_NO_BLOCK_TO_PROCESS);
            }
        } catch (Exception e) {
            addError("An error occurred while processing block : " + e.getMessage(), e);
        } finally {
            getSession().clear();
        }
    }

    /**
     * Getter session.
     *
     * @return session
     */
    private Session getSession() {
        return session;
    }

    /**
     * Return the block to process.
     *
     * @return block to process.
     */
    protected abstract Optional<Integer> getBlockHeightToProcess();

    /**
     * Treat block.
     *
     * @param blockHeight block height to process.
     * @return the block processed
     */
    protected abstract Optional<BitcoinBlock> processBlock(int blockHeight);

    /**
     * Returns the block height in a formatted way.
     *
     * @param blockHeight block height
     * @return formatted block height
     */
    protected final String getFormattedBlockHeight(final int blockHeight) {
        return String.format("%09d", blockHeight);
    }

    /**
     * Add a logger to the status and the logs.
     *
     * @param message message
     */
    protected final void addLog(final String message) {
        status.addLog(message);
    }

    /**
     * Add an error to the status and the logs.
     *
     * @param message message
     */
    protected final void addError(final String message) {
        status.addError(message, null);
    }

    /**
     * Add an error to the status and the logs.
     *
     * @param message message
     * @param e       exception raised.
     */
    private void addError(final String message, final Exception e) {
        status.addError(message, e);
    }

    /**
     * Getter mapper.
     *
     * @return mapper
     */
    protected final BitcoindToDomainMapper getMapper() {
        return mapper;
    }

    /**
     * Getter blockRepository.
     *
     * @return blockRepository
     */
    protected final BitcoinBlockRepository getBlockRepository() {
        return blockRepository;
    }

    /**
     * Getter addressRepository.
     *
     * @return addressRepository
     */
    protected final BitcoinAddressRepository getAddressRepository() {
        return addressRepository;
    }

    /**
     * Getter.
     *
     * @return transactionOutputRepository
     */
    protected final BitcoinTransactionOutputRepository getTransactionOutputRepository() {
        return transactionOutputRepository;
    }

    /**
     * Getter status.
     *
     * @return status
     */
    protected final StatusService getStatus() {
        return status;
    }

    /**
     * Getter bitcoin data service.
     *
     * @return bitcoin data service
     */
    protected final BitcoinDataService getBitcoinDataService() {
        return bitcoinDataService;
    }

}