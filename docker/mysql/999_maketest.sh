mysql -u root -proot --execute="CREATE DATABASE integrationtest"
mysql -u root -proot --database="integrationtest" < /docker-entrypoint-initdb.d/000_create_schema.sql