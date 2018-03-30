# Concierge (aka Yet another Zookeeper)

![Build Status](https://travis-ci.org/marnikitta/Concierge.svg?branch=master)

Concierge is a pet project created to obtain deeper understanding of 
distributed systems concepts

#### Roadmap v1.0

1.  [x] Implement _eventually strong_ failure detector. Timeout-based
2.  [x] Implement Ω leader elector. Monarchy (highest non-suspected pid wins)
3.  [x] Implement Paxos consensus algorithm
4.  [x] Build atomic broadcast on top of the consensus layer
5.  [x] Implement KV-storage using state-machine replication
6.  [x] Add sessions, ephemeral nodes...
7.  [x] Wrap kv-storage with REST HTTP server
8.  [x] Generate simple REST client (Retrofit?)
9.  [x] Docker container
10. [ ] Transform fail-stop model to fail-restore using log-deliver mechanism
11. [ ] Replace kyro serialization with protobuf

#### Roadmap v1.1

1. [ ] Clean codebase (Fix tests, refactor something, use default ports)
2. [ ] Replace ping method with native sessions, e.g., TCP connection or HTTP sessions
3. [ ] Clean API
4. [ ] Make state persistent. a.k.a. _Transform fail-stop model to fail-restore using log-deliver mechanism_
5. [ ] Add table schema, Bigtable like
6. [ ] Add SQL support, Megastore like
7. [ ] Add ACID transactions, Spanner like

#### Engineering fun

1. [ ] Create own actors library w/ networking (UDP)
2. [ ] Replace Kryo with ad-hoc serialization
3. [ ] Implement SSTables local storage and replace LevelDB

#### Running the tests

```bash
mvn test
```

#### Deployment

```yaml
version: '3'

services:
  concierge-1:
    container_name: concierge-1
    image: marnikita/concierge
    ports:
      - 8080:8080
    environment:
      - HOST=concierge-1
      - CONCIERGES=["concierge-1:23456","concierge-2:23456","concierge-3:23456"]
      - ACTOR_PORT=23456
      - CLIENT_PORT=8080

  concierge-2:
    container_name: concierge-2
    image: marnikita/concierge
    ports:
      - 8081:8080
    environment:
      - HOST=concierge-2
      - CONCIERGES=["concierge-1:23456","concierge-2:23456","concierge-3:23456"]
      - ACTOR_PORT=23456
      - CLIENT_PORT=8080

  concierge-3:
    container_name: concierge-3
    image: marnikita/concierge
    ports:
      - 8082:8080
    environment:
      - HOST=concierge-3
      - CONCIERGES=["concierge-1:23456","concierge-2:23456","concierge-3:23456"]
      - ACTOR_PORT=23456
      - CLIENT_PORT=8080

```

#### Built With

- [Maven](https://maven.apache.org/) - Dependency Management

#### License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details
