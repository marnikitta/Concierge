# Concierge (aka Yet another Zookeeper)

![Build Status](https://travis-ci.org/marnikitta/Concierge.svg?branch=master)

Concierge is a pet project created to obtain deeper understanding of 
distributed systems concepts

#### Roadmap

1.  __[✔]__ Implement _eventually strong_ failure detector. Timeout-based
2.  __[✔]__ Implement Ω leader elector. Monarchy (highest non-suspected pid wins)
3.  __[✔]__ Implement Paxos consensus algorithm
4.  __[✔]__ Build atomic broadcast on top of the consensus layer
5.  __[✔]__ Implement KV-storage using state-machine replication
6.  __[✔]__ Add sessions, ephemeral nodes...
7.  __[ ]__ Wrap kv-storage with REST HTTP server
8.  __[ ]__ Generate simple REST client (Retrofit?)
9.  __[ ]__ Docker container
10. __[ ]__ Basic REPL client as proof of concept
11. __[ ]__ Transform fail-stop model to fail-restore using log-deliver mechanism
12. __[ ]__ Replace kyro serialization with protobuf
