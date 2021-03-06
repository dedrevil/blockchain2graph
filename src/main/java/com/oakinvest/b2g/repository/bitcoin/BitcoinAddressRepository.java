package com.oakinvest.b2g.repository.bitcoin;

import com.oakinvest.b2g.domain.bitcoin.BitcoinAddress;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * BitcoinAddress repository.
 * Created by straumat on 10/09/16.
 */
@Repository
public interface BitcoinAddressRepository extends Neo4jRepository<BitcoinAddress, Long> {

	/**
	 * Returns 1 if the address is already in the database.
	 *
	 * @param address address
	 * @return 1 if the address exists.
	 */
	@Query("MATCH (a:BitcoinAddress) USING INDEX a:BitcoinAddress(address) WHERE a.address = {0} return count(*) = 1")
	boolean exists(String address);

    /**
     * Find an address.
     *
     * @param address address
     * @return address
     */
    Optional<BitcoinAddress> findByAddress(String address);

    /**
     * Find a bitcoin address (with depth 0).
     *
     * @param address address
     * @return bitcoin address
     */
    @Query("MATCH (a:BitcoinAddress) USING INDEX a:BitcoinAddress(address) WHERE a.address = {0} return a")
    Optional<BitcoinAddress> findByAddressWithoutDepth(String address);

}
