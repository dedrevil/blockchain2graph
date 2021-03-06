package com.oakinvest.b2g.repository.bitcoin;

import com.oakinvest.b2g.domain.bitcoin.BitcoinTransactionOutput;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * BitcoinTransactionOutput repository.
 * Created by straumat on 22/03/17.
 */
@Repository
public interface BitcoinTransactionOutputRepository extends Neo4jRepository<BitcoinTransactionOutput, Long> {

    /**
     * Find by txId and n.
     *
     * @param txId transaction id
     * @param n output
     * @return transaction output
     */
    Optional<BitcoinTransactionOutput> findByTxIdAndN(String txId, int n);

}
