/*
 * Copyright (C) 2018 Heinrich-Heine-Universitaet Duesseldorf, Institute of Computer Science,
 * Department Operating Systems
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package de.hhu.bsinfo.dxmem.operations;

import de.hhu.bsinfo.dxmem.DXMem;
import de.hhu.bsinfo.dxmem.core.CIDTableChunkEntry;
import de.hhu.bsinfo.dxmem.core.Context;
import de.hhu.bsinfo.dxmem.data.AbstractChunk;
import de.hhu.bsinfo.dxutils.stats.StatisticsManager;
import de.hhu.bsinfo.dxutils.stats.Value;

/**
 * Allocate memory for multiple already reserved CIDs (reserved using the Reserve operation). This can also be
 * used by the recovery to write recovered (non local chunks).
 *
 * @author Stefan Nothaas, stefan.nothaas@hhu.de, 21.06.2018
 */
public class CreateReservedMulti {
    private static final Value SOP_CREATE_RESERVE = new Value(DXMem.class, "CreateReserve");

    static {
        StatisticsManager.get().registerOperation(DXMem.class, SOP_CREATE_RESERVE);
    }

    private final Context m_context;

    /**
     * Constructor
     *
     * @param p_context
     *         Context with core components
     */
    public CreateReservedMulti(final Context p_context) {
        m_context = p_context;
    }

    // DO NOT pass arbitrary IDs as the first parameter. use the reserve operation to generate
    // chunk IDs and reserve them
    // assigning arbitrary values will definitely break something

    /**
     * Allocate memory for a reserved CID. DO NOT pass arbitrary CIDs to this function. Always use the Reserve operation
     * to get CIDs which can be used with this operation. Using arbitrary CIDs will definitely break the subsystem.
     * Exception: Recovery of non local chunks
     *
     * @param p_cids
     *         Array of reserved (or recovered) CIDs to allocate memory for
     * @param p_addresses
     *         Optional (can be null): Array to return the raw addresses of the allocate chunks
     * @param p_sizes
     *         Sizes to allocate chunks for
     * @param p_sizesOffset
     *         Start offset in size array
     * @param p_sizesLength
     *         Number of elements to consider of size array
     */
    public void createReserved(final long[] p_cids, final long[] p_addresses, final int[] p_sizes,
            final int p_sizesOffset, final int p_sizesLength) {
        assert p_cids != null;
        // p_addresses is optional
        assert p_sizes != null;
        assert p_sizesOffset >= 0;
        assert p_sizesLength >= 0;

        // can't use thread local pool here
        CIDTableChunkEntry[] entries = new CIDTableChunkEntry[p_sizesLength];

        m_context.getDefragmenter().acquireApplicationThreadLock();

        m_context.getHeap().malloc(entries, p_sizes, p_sizesOffset, p_sizesLength);

        for (int i = 0; i < p_sizesLength; i++) {
            m_context.getCIDTable().insert(p_cids[i], entries[i]);
        }

        if (p_addresses != null) {
            for (int i = 0; i < p_sizesLength; i++) {
                p_addresses[i] = entries[i].getAddress();
            }
        }

        m_context.getDefragmenter().releaseApplicationThreadLock();

        SOP_CREATE_RESERVE.inc();
    }

    /**
     * Allocate memory for a reserved chunk. DO NOT pass arbitrary chunks with CIDs to this function.
     * Always use the Reserve operation to get CIDs which can be used with this operation.
     * Using arbitrary CIDs will definitely break the subsystem.
     * Exception: Recovery of non local chunks
     *
     * @param p_ds
     *         Array of chunks with reserved CIDs set
     * @param p_addresses
     *         Optional (can be null): Array to return the raw addresses of the allocate chunks
     */
    public void createReserved(final AbstractChunk[] p_ds, final long[] p_addresses) {
        assert p_ds != null;
        // p_addresses is optional

        // can't use thread local pool here
        CIDTableChunkEntry[] entries = new CIDTableChunkEntry[p_ds.length];
        int[] sizes = new int[p_ds.length];

        for (int i = 0; i < sizes.length; i++) {
            sizes[i] = p_ds[i].sizeofObject();
        }

        m_context.getDefragmenter().acquireApplicationThreadLock();

        m_context.getHeap().malloc(entries, sizes, 0, sizes.length);

        for (int i = 0; i < sizes.length; i++) {
            m_context.getCIDTable().insert(p_ds[i].getID(), entries[i]);
        }

        if (p_addresses != null) {
            for (int i = 0; i < sizes.length; i++) {
                p_addresses[i] = entries[i].getAddress();
            }
        }

        m_context.getDefragmenter().releaseApplicationThreadLock();

        SOP_CREATE_RESERVE.inc();
    }
}
