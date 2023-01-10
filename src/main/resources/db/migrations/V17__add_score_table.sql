CREATE TABLE scores
(
    `Event_ID` int(11) NOT NULL,
    `Score` MEDIUMBLOB NOT NULL,
    PRIMARY KEY (`Event_ID`),
    FOREIGN KEY (`Event_ID`) REFERENCES events (`Event_ID`)
);