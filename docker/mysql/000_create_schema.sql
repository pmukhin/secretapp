CREATE TABLE `Client` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `firstName` varchar(50) NOT NULL DEFAULT '',
  `lastName` varchar(50) NOT NULL DEFAULT '',
  `bod` date NOT NULL,
  `isDeleted` char(1) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

CREATE TABLE `Tariff` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `hourlyRate` double NOT NULL,
  `hourlyParkingRate` double DEFAULT NULL,
  `start` datetime NOT NULL,
  `isDeleted` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;

CREATE TABLE `Session` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `start` datetime NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE current_timestamp(),
  `end` datetime NOT NULL,
  `clientId` int(11) NOT NULL,
  `chargePointId` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `start_chargePointId_unique_idx` (`start`,`chargePointId`),
  KEY `clientId` (`end`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8