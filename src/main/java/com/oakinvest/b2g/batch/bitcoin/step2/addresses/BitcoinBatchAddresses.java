package com.oakinvest.b2g.batch.bitcoin.step2.addresses;

import com.oakinvest.b2g.domain.bitcoin.BitcoinAddress;
import com.oakinvest.b2g.domain.bitcoin.BitcoinBlock;
import com.oakinvest.b2g.domain.bitcoin.BitcoinBlockState;
import com.oakinvest.b2g.dto.ext.bitcoin.bitcoind.BitcoindBlockData;
import com.oakinvest.b2g.util.bitcoin.batch.BitcoinBatchTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Bitcoin import addresses batch.
 * Created by straumat on 27/02/17.
 */
@Component
public class BitcoinBatchAddresses extends BitcoinBatchTemplate {

	/**
	 * Log prefix.
	 */
	private static final String PREFIX = "Addresses batch";

	/**
	 * Returns the log prefix to display in each log.
	 */
	@Override
	public final String getLogPrefix() {
		return PREFIX;
	}

	/**
	 * Return the block to treat.
	 *
	 * @return block to treat.
	 */
	@Override
	protected final Long getBlockToTreat() {
		BitcoinBlock blockToTreat = getBlockRepository().findFirstBlockByState(BitcoinBlockState.BLOCK_IMPORTED);
		if (blockToTreat != null) {
			return blockToTreat.getHeight();
		} else {
			return null;
		}
	}

	/**
	 * Treat block.
	 *
	 * @param blockHeight block number to treat.
	 */
	@Override
	@SuppressWarnings({ "checkstyle:designforextension", "checkstyle:emptyforiteratorpad" })
	protected final BitcoinBlock treatBlock(final long blockHeight) {
		BitcoindBlockData blockData = getBitcoindService().getBlockData(blockHeight);
		// ---------------------------------------------------------------------------------------------------------
		// If we have the data
		if (blockData != null) {
			// -----------------------------------------------------------------------------------------------------
			// We retrieve all the addresses.
			final List<String> addresses = Collections.synchronizedList(new ArrayList<String>());
			blockData.getTransactions().parallelStream()
					.forEach(grt -> grt.getVout()
							.stream()
							.filter(Objects::nonNull)
							.forEach(v -> v.getScriptPubKey()
									.getAddresses().stream()
									.filter(Objects::nonNull)
									.forEach(addresses::add)));

			// -----------------------------------------------------------------------------------------------------
			// We create all the addresses.
			addresses.stream()
					.distinct()
					// If the address doesn't exists
					.filter(address -> getAddressRepository().findByAddress(address) == null)
					// We save all the addresses.
					.forEach(address -> {
						BitcoinAddress a = new BitcoinAddress(address);
						getAddressRepository().save(a);
						addLog("- Address " + address + " created with id " + a.getId());
					});

			// ---------------------------------------------------------------------------------------------------------
			// We return the block.
			return getBlockRepository().findByHeight(blockHeight);
		} else {
			addError("No response from bitcoind for block n°" + getFormattedBlock(blockHeight));
			return null;
		}
	}

	/**
	 * Return the state to set to the block that has been treated.
	 *
	 * @return state to set of the block that has been treated.
	 */
	@Override
	protected final BitcoinBlockState getNewStateOfTreatedBlock() {
		return BitcoinBlockState.ADDRESSES_IMPORTED;
	}

}
