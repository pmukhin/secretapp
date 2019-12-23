# TVI App

## Important Endpoints
- `POST /clients` - creating a client
- `GET  /invoices/<clientId>/<start>/<end>.csv` - get an invoice in csv
- `POST /tariffs` - create a tariff
- `POST /sessions` - create a session

## Modules explanation

## How to run
- `sbt "project web" assembly`
- `docker-compose up`
- `open http://127.0.0.1:8080`

## How to run integration tests
- stop your docker environment by `docker-compose down`
- run your docker environment with new env var 
    `TVI_MYSQL_NAME=integrationtest docker-compose up --build`
- run tests `sbt "project it" clean test`

## Example requests
Example requests can be found in `it` module in `src/test/resources/testdata`