# TVI App

## Important Endpoints
- `POST /clients` - creating a client
- `GET  /invoices/<clientId>/<start>/<end>.csv` - get an invoice in csv
- `POST /tariffs` - create a tariff
- `POST /sessions` - create a session

## How to run
- `sbt "project web" assembly`
- `docker-compose up`
- `open http://127.0.0.1:8080`
