<div align="center">

<img src="https://readme-typing-svg.demolab.com?font=JetBrains+Mono&weight=700&size=25&duration=2800&pause=900&color=7C83FD&center=true&vCenter=true&width=850&lines=LSMTreeDB;A+Log-Structured+Key-Value+Storage+Engine;Built+from+first+principles+in+Java" alt="LSMTreeDB animated title" />

<br/>

<p>
<strong>A learning-driven storage engine exploring how modern databases
turn writes into durable, searchable data.</strong>
</p>
<p>
<img src="https://img.shields.io/badge/Language-Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"/>
<img src="https://img.shields.io/badge/Build-Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" alt="Maven"/>
<img src="https://img.shields.io/badge/Tests-JUnit%205-25A162?style=for-the-badge&logo=junit5&logoColor=white" alt="JUnit 5"/>
<img src="https://img.shields.io/badge/Architecture-LSM--Tree-6C63FF?style=for-the-badge" alt="LSM Tree"/>
<img src="https://img.shields.io/badge/Status-In%20Development-CA8A04?style=for-the-badge" alt="In development"/>
</p>
<p>
<em>Write efficiently. Persist safely. Read intelligently.</em>
</p>

</div>

------------------------------------------------------------------------

## What is LSMTreeDB?

**LSMTreeDB** is a Java-based key-value storage engine being built from
the ground up around the **Log-Structured Merge-Tree (LSM-Tree)**
design.

The project is an exploration of database internals: how a storage
engine accepts writes, makes them durable, organizes data on disk,
recovers after a crash, and controls the cost of reads as data grows.

> **Project status:** This repository is under active development. The
> architecture and roadmap below describe the intended implementation.
> Features are marked as complete only after they are implemented and
> tested.

## Why build a storage engine?

Databases hide a remarkable amount of engineering behind a simple
`put(key, value)` call. LSMTreeDB makes those mechanics visible:

- **Write path:** append operations to a log and update an in-memory
  structure.
- **Durability:** recover acknowledged data after a process restart.
- **Disk layout:** flush sorted data into immutable files.
- **Read path:** reconcile data across memory and disk while respecting
  newer values and deletions.
- **Maintenance:** compact files and use probabilistic filters to avoid
  unnecessary disk reads.
- **Measurement:** benchmark workloads instead of making unverified
  performance claims.

## Architecture at a glance

The following diagram represents the target write path. It will evolve
as implementation decisions are validated.

``` mermaid
flowchart TD
    A["Client: put(key, value)"] --> B["Write-Ahead Log (WAL)"]
    B --> C["Memtable"]
    C --> D{"Memtable threshold reached?"}
    D -- "No" --> E["Continue accepting writes"]
    D -- "Yes" --> F["Flush sorted entries"]
    F --> G["Immutable SSTable"]
    G --> H["Disk storage"]
    H --> I["Background compaction"]
    I --> J["Merged SSTables"]
    J --> H
    E --> A

    classDef client fill:#172554,stroke:#60A5FA,color:#EFF6FF
    classDef memory fill:#064E3B,stroke:#34D399,color:#ECFDF5
    classDef disk fill:#312E81,stroke:#A78BFA,color:#F5F3FF
    classDef process fill:#713F12,stroke:#FBBF24,color:#FFFBEB
    class A client
    class B,C memory
    class F,G,H,J disk
    class I process
```

### Component responsibilities

| Component        | Responsibility                                                                 |
|------------------|--------------------------------------------------------------------------------|
| **WAL**          | Records mutations so the engine can replay them during recovery.               |
| **Memtable**     | Holds the latest in-memory view of keys before flushing.                       |
| **SSTable**      | Stores sorted, immutable key-value data on disk.                               |
| **Bloom filter** | May indicate that a key is absent from an SSTable, avoiding some disk lookups. |
| **Compaction**   | Merges SSTables, resolves obsolete versions, and reclaims space.               |

## How a write is intended to work

``` mermaid
sequenceDiagram
    autonumber
    participant App as Application
    participant WAL as Write-Ahead Log
    participant Mem as Memtable
    participant Disk as SSTable Storage

    App->>WAL: append mutation
    WAL-->>App: durable acknowledgement*
    App->>Mem: apply mutation
    Note over Mem: Keep latest in-memory state
    opt Memtable reaches configured threshold
        Mem->>Disk: flush sorted snapshot
        Disk-->>Mem: SSTable created
    end
```

`*` The exact acknowledgement and durability guarantee depend on the WAL
implementation, write batching, and synchronization policy. These
semantics will be documented and tested rather than assumed.

## How reads are intended to work

``` mermaid
flowchart TD
    Q["get(key)"] --> M{"Found in Memtable?"}
    M -- Yes --> R["Return newest value"]
    M -- No --> B["Check candidate SSTables"]
    B --> BF{"Bloom filter says possibly present?"}
    BF -- No --> N["Skip this SSTable"]
    BF -- Yes --> S["Search SSTable index / data"]
    N --> NEXT["Check next candidate"]
    S --> FOUND{"Key found?"}
    FOUND -- Yes --> V["Compare versions / sequence numbers"]
    FOUND -- No --> NEXT
    V --> OUT["Return newest valid value"]
    NEXT --> DONE["No remaining candidates"]
    DONE --> NULL["Return not found"]
```

A production-quality implementation must also define tombstones, version
ordering, overlapping files, and the rules for choosing the newest
value. Those are correctness requirements, not optional polish.

## Planned repository structure

The structure below is the intended direction. Package and class names
may change as the implementation becomes concrete.

``` text
LSMTreeDB/
├── pom.xml
├── README.md
├── .gitignore
├── docs/
│   ├── architecture.md
│   ├── storage-format.md
│   └── benchmark-methodology.md
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/sarthak/lsmtree/
│   │           ├── api/
│   │           │   └── KeyValueStore.java
│   │           ├── memtable/
│   │           │   └── MemTable.java
│   │           ├── wal/
│   │           │   └── WriteAheadLog.java
│   │           ├── sstable/
│   │           │   ├── SSTable.java
│   │           │   └── SSTableReader.java
│   │           ├── bloom/
│   │           │   └── BloomFilter.java
│   │           ├── compaction/
│   │           │   └── Compactor.java
│   │           └── recovery/
│   │               └── RecoveryManager.java
│   └── test/
│       └── java/
│           └── com/sarthak/lsmtree/
│               ├── memtable/
│               ├── wal/
│               ├── sstable/
│               └── recovery/
└── data/                  # Runtime data; should not be committed
```

## Development roadmap

| Phase | Engineering milestone                  | Status     |
|-------|----------------------------------------|------------|
| 01    | In-memory key-value API (`put`, `get`) | Foundation |
| 02    | WAL record format and append path      | Planned    |
| 03    | WAL replay and restart recovery        | Planned    |
| 04    | Memtable flush and SSTable format      | Planned    |
| 05    | SSTable lookup and version resolution  | Planned    |
| 06    | Bloom filters                          | Planned    |
| 07    | Compaction and tombstone handling      | Planned    |
| 08    | Correctness, crash and edge-case tests | Planned    |
| 09    | Reproducible performance benchmarks    | Planned    |
| 10    | Documentation and release              | Planned    |

<details>
<summary>
<strong>Engineering checklist</strong>
</summary>

- [ ] Define key/value encoding and on-disk record format
- [ ] Define WAL framing, checksums, and torn-write handling
- [ ] Specify when a write is considered durable
- [ ] Implement replay and idempotent recovery
- [ ] Define SSTable metadata and indexing
- [ ] Handle updates, deletes, and tombstones correctly
- [ ] Test compaction against overlapping key ranges
- [ ] Benchmark with repeatable workloads and report environment details

</details>

## Correctness before performance

A storage engine is only useful if it returns the right value after
ordinary operations, restarts, and failures. The test strategy will
therefore cover:

- **Key semantics:** insert, overwrite, missing keys, and deletion.
- **Persistence:** close and reopen without losing acknowledged writes.
- **Recovery:** replay valid log records and handle incomplete trailing
  records safely.
- **Ordering:** resolve multiple versions of the same key correctly.
- **Compaction:** preserve live values and remove obsolete data only
  when safe.
- **Filters:** Bloom-filter false positives are acceptable; false
  negatives are not.

## Benchmarking philosophy

Performance numbers will be published only after the benchmark harness
exists and the workload is repeatable.

Planned measurements:

| Metric                  | What it tells us                                  |
|-------------------------|---------------------------------------------------|
| Throughput (ops/sec)    | Operations completed per second                   |
| p50 / p95 / p99 latency | Typical and tail operation latency                |
| Write amplification     | Extra bytes written relative to user data         |
| Read amplification      | Number of storage locations examined per read     |
| Space amplification     | Physical storage used relative to live data       |
| Recovery time           | Time needed to restore state from persistent data |

Benchmarks will document the machine, JVM version, dataset size,
key/value sizes, read/write mix, warm-up, and measurement duration.
Results will not be compared across different setups as if they were
equivalent.

## Technology stack

- **Language:** Java
- **Build:** Apache Maven
- **Testing:** JUnit 5
- **Core concepts:** LSM-Trees, data structures, file I/O, persistence,
  recovery, compaction, probabilistic data structures
- **Tooling:** Git and command-line development tools

## Design references

This project is an educational implementation intended to make
storage-engine trade-offs understandable. Useful background reading:

- [LSM-Tree overview and storage-engine
  concepts](https://system-design-in-depth.vedgupta.in/topic/lsm-tree-storage-engine/)
- [Apache Cassandra storage engine
  documentation](https://cassandra.apache.org/doc/latest/cassandra/architecture/storage_engine.html)
- [LevelDB implementation
  notes](https://github.com/google/leveldb/blob/main/doc/impl.md)

## Contributing and feedback

This is an individual learning project. Suggestions, bug reports, and
discussions about correctness or storage design are welcome. If you spot
an issue, please include the operation sequence, expected result, actual
result, and reproduction steps.

------------------------------------------------------------------------

<div align="center">

**LSMTreeDB — understanding the machinery behind durable storage, one
component at a time.**

<sub>Designed and developed by
<a href="https://github.com/Sarthaktanpure">Sarthak Tanpure</a></sub>

</div>
