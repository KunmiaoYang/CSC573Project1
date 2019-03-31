DROP TABLE IF EXISTS client;
CREATE TABLE `client` (
  `ip` varchar(20) NOT NULL,
  `port` int(11) NOT NULL,
  `active` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`ip`,`port`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS RFC;
CREATE TABLE `rfc` (
  `number` int(11) NOT NULL,
  `title` varchar(256) DEFAULT NULL,
  `ip` varchar(20) NOT NULL,
  `port` int(11) NOT NULL,
  PRIMARY KEY (`number`,`ip`,`port`),
  KEY `ip` (`ip`,`port`),
  CONSTRAINT `rfc_ibfk_1` FOREIGN KEY (`ip`, `port`) REFERENCES `client` (`ip`, `port`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;