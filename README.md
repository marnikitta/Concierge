# Concierge (aka Yet another Zookeeper)

![Build Status](https://travis-ci.org/marnikitta/Concierge.svg?branch=master)

Concierge is a pet project created to obtain deeper understanding of 
distributed systems concepts

#### Roadmap

1. __[✔]__ Implement _eventually strong_ failure detector. Timeout-based
2. __[✔]__ Implement Ω leader elector. Monarchy (highest non-suspected pid wins)
3. __[✔]__ Implement Paxos consensus algorithm
4. __[ ]__ Build atomic broadcast on top of the consensus layer
5. __[ ]__ Deliver ledger to the newly elected leader
6. __[ ]__ Implement KV-storage using state-machine replication
7. __[ ]__ Add sessions, ephemeral nodes...
8. __[ ]__ Make leader election primitive aware of the ledger length
