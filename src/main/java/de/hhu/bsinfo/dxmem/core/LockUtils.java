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

package de.hhu.bsinfo.dxmem.core;

public final class LockUtils {

    public enum LockStatus {
        OK,
        INVALID,
        TIMEOUT
    }

    /**
     * Private constructor for utility class
     */
    private LockUtils() {

    }

    // this call might also re-read the entry if acquiring the lock did not succeed on the first try
    // if the lock was acquired successfully, the (re-read) entry is guaranteed to be valid
    // retry timeout -1 = infinite, 0 = one shot, > 0 timeout in ms
    public static LockStatus acquireReadLock(final CIDTable p_cidTable, final CIDTableChunkEntry p_entry,
            final int p_retryTimeoutMs) {
        long startTime = 0;

        if (p_retryTimeoutMs > 0) {
            startTime = System.nanoTime();
        }

        while (true) {
            // entry turned invalid, e.g. chunk was deleted
            if (!p_entry.isValid()) {
                return LockStatus.INVALID;
            }

            if (!p_entry.isWriteLockAcquired()) {
                if (p_entry.acquireReadLock()) {
                    // read lock acquired, try to persist state
                    if (p_cidTable.entryAtomicUpdate(p_entry)) {
                        return LockStatus.OK;
                    }
                }
                // else: no locks available because too many readers active, we have to wait
            }
            // else: write lock acquired, don't enter until released

            if (p_retryTimeoutMs >= 0) {
                if (p_retryTimeoutMs == 0 || System.nanoTime() - startTime >= p_retryTimeoutMs * 1000 * 1000) {
                    return LockStatus.TIMEOUT;
                }
            }

            Thread.yield();
            p_cidTable.entryReread(p_entry);
        }
    }

    public static void releaseReadLock(final CIDTable p_cidTable, final CIDTableChunkEntry p_entry) {
        while (true) {
            // invalid state, chunk was deleted but a lock was still acquired
            assert p_entry.isValid();

            p_entry.releaseReadLock();

            // read lock released, try to persist state
            if (p_cidTable.entryAtomicUpdate(p_entry)) {
                break;
            }

            Thread.yield();
            p_cidTable.entryReread(p_entry);
        }
    }

    // this call might also re-read the entry if acquiring the lock did not succeed on the first try
    // if the lock was acquired successfully, the (re-read) entry is guaranteed to be valid
    // retry timeout -1 = infinite, 0 = one shot, > 0 timeout in ms
    public static LockStatus acquireWriteLock(final CIDTable p_cidTable, final CIDTableChunkEntry p_entry,
            final int p_retryTimeoutMs) {
        long startTime = 0;

        if (p_retryTimeoutMs > 0) {
            startTime = System.nanoTime();
        }

        while (true) {
            // entry turned invalid, e.g. chunk was deleted
            if (!p_entry.isValid()) {
                return LockStatus.INVALID;
            }

            if (p_entry.acquireWriteLock()) {
                // try to persist state
                if (p_cidTable.entryAtomicUpdate(p_entry)) {
                    // now, wait for all readers to exit the section

                    while (p_entry.areReadLocksAcquired()) {
                        Thread.yield();
                        p_cidTable.entryReread(p_entry);
                    }

                    // write locked with no readers in section
                    return LockStatus.OK;
                }
            }
            // else: write lock already acquired

            if (p_retryTimeoutMs >= 0) {
                if (p_retryTimeoutMs == 0 || System.nanoTime() - startTime >= p_retryTimeoutMs * 1000 * 1000) {
                    return LockStatus.TIMEOUT;
                }
            }

            Thread.yield();
            p_cidTable.entryReread(p_entry);
        }
    }

    public static void releaseWriteLock(final CIDTable p_cidTable, final CIDTableChunkEntry p_entry) {
        while (true) {
            // invalid state, chunk was deleted but a lock was still acquired
            assert p_entry.isValid();

            p_entry.releaseWriteLock();

            // write lock released, try to persist state
            if (p_cidTable.entryAtomicUpdate(p_entry)) {
                break;
            }

            Thread.yield();
            p_cidTable.entryReread(p_entry);
        }
    }
}