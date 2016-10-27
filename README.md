# Foursquare-Attendance-Service

This repository contains the code that runs the SmartVenues web service. The original SmartVenues application of the University of Glasgow is available at (http://demos.terrier.org/SMART/venuesuggestion/). Before setting up the web service, please have a
 look at https://github.com/SmartSearch/Foursquare-Attendance-Crawler and https://github.com/SmartSearch/Foursquare-Attendance-Forecasting.

## Running

```
mvn exec:java  -Dterrier.etc=/path/to/terrier/etc -Dterrier.setup=/path/to/terrier/etc/terrier.properties -Dexec.args="/path/to/foursquare 6"
```

## Citing

If you use this code for a research purpose, please use the following citation:

Romain Deveaud, M-Dyaa Albakour, Jarana Manotumruksa, Craig Macdonald, and Iadh Ounis. *SmartVenues: Recommending Popular and Personalised Venues in a City.* In CIKM 2014, Shanghai, China. http://dl.acm.org/citation.cfm?id=2661855

Bibtex:
```
@inproceedings{Deveaud:2014:SRP:2661829.2661855,
 author = {Deveaud, Romain and Albakour, M-Dyaa and Manotumruksa, Jarana and Macdonald, Craig and Ounis, Iadh},
 title = {SmartVenues: Recommending Popular and Personalised Venues in a City},
 booktitle = {Proceedings of the 23rd ACM International Conference on Conference on Information and Knowledge Management},
 series = {CIKM '14},
 year = {2014},
 isbn = {978-1-4503-2598-1},
 location = {Shanghai, China},
 pages = {2078--2080},
 numpages = {3},
 url = {http://doi.acm.org/10.1145/2661829.2661855},
 doi = {10.1145/2661829.2661855},
 acmid = {2661855},
 publisher = {ACM},
 address = {New York, NY, USA},
 keywords = {attendance prediction, facebook, foursquare, location-based social network, time series forecasting, venue recommendation},
} 
```
