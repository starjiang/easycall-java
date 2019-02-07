/*
SQLyog Ultimate v12.09 (64 bit)
MySQL - 5.7.21 : Database - configcenter
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`configcenter` /*!40100 DEFAULT CHARACTER SET utf8mb4 */;

USE `configcenter`;

/*Table structure for table `data` */

DROP TABLE IF EXISTS `data`;

CREATE TABLE `data` (
  `name` varchar(250) NOT NULL,
  `data` text NOT NULL,
  `version` bigint(20) NOT NULL DEFAULT '0',
  `owner` varchar(100) NOT NULL DEFAULT '',
  `createTime` timestamp NOT NULL DEFAULT '2019-01-01 00:00:00',
  `updateTime` timestamp NOT NULL DEFAULT '2019-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/*Data for the table `data` */

insert  into `data`(`name`,`data`,`version`,`owner`,`createTime`,`updateTime`) values ('profile','service.zk=127.0.0.1:2181',7,'','2019-01-01 00:00:00','2019-02-07 19:31:47'),('profile1','sdsdsdds',1,'','2019-01-31 18:30:51','2019-01-31 18:31:11');

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
