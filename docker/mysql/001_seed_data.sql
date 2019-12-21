LOAD DATA INFILE '/data/Client.csv'
IGNORE INTO TABLE `Client`  FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\n';

LOAD DATA INFILE '/data/Tariff.csv'
IGNORE INTO TABLE `Tariff`   FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\n'
(`id`, hourlyRate, @hourlyParkingRate, `start`, `isDeleted`)
SET hourlyParkingRate = nullif(@hourlyParkingRate, '');

LOAD DATA INFILE '/data/Session.csv'
IGNORE INTO TABLE `Session` FIELDS TERMINATED BY ',' ENCLOSED BY '"' LINES TERMINATED BY '\n';