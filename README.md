# TVI App

## Important Endpoints
- `POST /clients` - creating a client
- `GET  /invoices/<clientId>/<start>/<end>.csv` - get an invoice in csv
- `POST /tariffs` - create a tariff
- `POST /sessions` - create a session

## Modules explanation
- `domain` - where abstract application logic is located along with models
- `doobie-be` - doobie implementation of data sources
- `csv2s` - a tiny library for producing csv
- `web` - web application as
- `it` - integration tests

## How to run
- `sbt "project web" assembly` to build a jar
- `docker-compose up --build` to build and run docker images
- `open http://127.0.0.1:8080` to open a browser

## Important points
- Integration specs are dependant on order execution (for simplicity)
- Creating a session does not happen synchronously, instead a new session data is 
placed into a queue and then executed by a worker
- In this example app worker is just a stream concurrently reading from a queue along with other workers
- In a real world app worker could be a kafka consumer, or a node in akka cluster
- Generation of csv invoices also could be built this way: only empty invoice record in db is created, 
and then a message is sent into a queue. User might use an identifier returned during invoice creation,
and get `425 Too Early` while invoice is not processed by a worker yet
- MySQL/MariaDB is taken for simplicity 

## How to run integration tests
- stop your docker environment by `docker-compose down`
- run your docker environment with new env var 
    `TVI_MYSQL_NAME=integrationtest docker-compose up --build`
- run tests `sbt "project it" clean test`

## Example requests
Example requests can be found in `it` module in `src/test/resources/testdata`