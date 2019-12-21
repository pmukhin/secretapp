CREATE TABLE `Client` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `firstName` varchar(50) NOT NULL DEFAULT '',
  `lastName` varchar(50) NOT NULL DEFAULT '',
  `dob` date NOT NULL,
  `isDeleted` char(1) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `isDeleted_idx` (`isDeleted`) -- for list view --
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `Tariff` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `hourlyRate` double NOT NULL,
  `hourlyParkingRate` double DEFAULT NULL,
  `start` datetime NOT NULL,
  `isDeleted` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `isDeleted_start_idx` (`isDeleted`, `start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `Session` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `start` datetime NOT NULL,
  `end` datetime NOT NULL,
  `clientId` int(11) NOT NULL,
  `chargePointId` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `start_chargePointId_unique_idx` (`start`,`chargePointId`),
  KEY `clientId_idx` (`clientId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8