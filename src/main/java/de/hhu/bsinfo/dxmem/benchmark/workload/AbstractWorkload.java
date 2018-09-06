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

package de.hhu.bsinfo.dxmem.benchmark.workload;

import picocli.CommandLine;

import de.hhu.bsinfo.dxmem.benchmark.Benchmark;
import de.hhu.bsinfo.dxmem.benchmark.BenchmarkRunner;

/**
 * Base class for benchmark workloads
 *
 * @author Stefan Nothaas, stefan.nothaas@hhu.de, 31.08.2018
 */
public abstract class AbstractWorkload implements Runnable {
    @CommandLine.ParentCommand
    private Object m_parent;

    /**
     * Create the workload
     *
     * @return Benchmark benchmark workload with phases
     */
    public abstract Benchmark createWorkload();

    @Override
    public void run() {
        if (m_parent instanceof BenchmarkRunner) {
            BenchmarkRunner parent = (BenchmarkRunner) m_parent;

            parent.runBenchmark(createWorkload());
        } else {
            throw new IllegalStateException("Parent command does not implement BenchmarkRunner interface");
        }
    }
}
