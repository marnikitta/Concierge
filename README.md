# Concierge (aka Yet another Zookeeper)

![Build Status](https://travis-ci.org/marnikitta/Concierge.svg?branch=master)

Concierge is a pet project created to obtain deeper understanding of 
distributed systems concepts

## Roadmap

1.  __[✔]__ Implement _eventually strong_ failure detector. Timeout-based
2.  __[✔]__ Implement Ω leader elector. Monarchy (highest non-suspected pid wins)
3.  __[✔]__ Implement Paxos consensus algorithm
4.  __[✔]__ Build atomic broadcast on top of the consensus layer
5.  __[✔]__ Implement KV-storage using state-machine replication
6.  __[✔]__ Add sessions, ephemeral nodes...
7.  __[✔]__ Wrap kv-storage with REST HTTP server
8.  __[✔]__ Generate simple REST client (Retrofit?)
9.  __[✔]__ Docker container
10. __[ ]__ Transform fail-stop model to fail-restore using log-deliver mechanism
11. __[ ]__ Replace kyro serialization with protobuf

## Running the tests

```bash
mvn test
```

## Deployment

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

## Built With

- [Maven](https://maven.apache.org/) - Dependency Management

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details
