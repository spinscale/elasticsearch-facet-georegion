# Elasticsearch Georegion Facet

This facet allows you to specifiy a geo_point typed field in a facet and then get the results grouped by country (or any other geo group if you specified it in the correct format).

One of the biggest problems is, that the current geo documentation of elasticsearch is not in a good shape (huhu, wording game for insiders in this context). The problem goes further as [spatial4j](https://github.com/spatial4j/spatial4j/) and [JTS](https://github.com/spatial4j/spatial4j/) documentation looks as bad.

## Installation

```
git clone https://github.com/spinscale/elasticsearch-facet-georegion
cd elasticsearch-facet-georegion
mvn package -DskipTests=true
cd /path/to/elasticsearch-installation/
bin/plugin -install elasticsearch-facet-georegion -url file:///path/to/elasticsearch-facet-georegion/target/releases/elasticsearch-facet-georegion-1.0-SNAPSHOT.zip
```

## Usage

If you want to use it in java, check out the tests included.

If you want to use it from the command line, first download the countries file from here

```
cd config/
wget --no-check-certificate https://github.com/johan/world.geo.json/raw/master/countries.geo.json
```

Then edit the elasticsearch.yml file to include

```
facet.georegion.file: config/countries.geo.json
```

Start elasticsearch and check if the plugin is loaded like this

```
# bin/elasticsearch -f
[2012-12-28 15:25:49,788][INFO ][node                     ] [Rebel] {0.20.2}[12294]: initializing ...
[2012-12-28 15:25:49,800][INFO ][plugins                  ] [Rebel] loaded [facet-georegion], sites []
...
[2012-12-28 15:25:52,060][ERROR][plugin.service.georegion ] [Rebel] Could not add country Antarctica to countries with message: found non-noded intersection between LINESTRING ( 295.705894 -66.837004, 295.118307 -67.150474 ) and LINESTRING ( 480.871 -67.189696, 180.0 -66.92709850108163 ) [ (295.34830425954766, -67.02777346212152, NaN) ]
[2012-12-28 15:25:52,339][ERROR][plugin.service.georegion ] [Rebel] Could not add country Canada to countries with message: Self-intersection at or near point (-132.710009, 54.040009, NaN)
[2012-12-28 15:25:52,818][INFO ][plugin.service.georegion ] [Rebel] Loaded 177 elements into region countries
```

Now create an index with a geo_point mapping

```
curl -X PUT http://localhost:9200/myindex
curl -X PUT http://localhost:9200/myindex/mytype/_mapping -d '{
  "mytype" : {
    "properties" : {
      "loc" : { "type" : "geo_point", "store" : "yes" }
    }
  }
}'
```

Now add some cities into your new shiny index

```
curl -X POST http://localhost:9200/_bulk -d '
{ "index" : { "_index" : "myindex", "_type" : "mytype", "_id" : "1" } }
{ "name" : "Paris", "loc" : [48.856666666667,2.3516666666667] }
{ "index" : { "_index" : "myindex", "_type" : "mytype", "_id" : "2" } }
{ "name" : "Berlin", "loc" : [52.518611111111, 13.40805555555] }
{ "index" : { "_index" : "myindex", "_type" : "mytype", "_id" : "3" } }
{ "name" : "Munich", "loc" : [48.136944444444, 11.575277777778] }
{ "index" : { "_index" : "myindex", "_type" : "mytype", "_id" : "4" } }
{ "name" : "Cologne", "loc" : [50.938055555556, 6.9569444444444] }
{ "index" : { "_index" : "myindex", "_type" : "mytype", "_id" : "5" } }
{ "name" : "Rome", "loc" : [41.883333333333, 12.483333333333] }
{ "index" : { "_index" : "myindex", "_type" : "mytype", "_id" : "6" } }
{ "name" : "Milano", "loc" : [45.4625, 9.1863888888889] }
{ "index" : { "_index" : "myindex", "_type" : "mytype", "_id" : "7" } }
{ "name" : "Stockholm", "loc" : [59.325, 18.05] }
{ "index" : { "_index" : "myindex", "_type" : "mytype", "_id" : "8" } }
{ "name" : "Amsterdam", "loc" : [52.370197222222, 4.8904444444444] }
{ "index" : { "_index" : "myindex", "_type" : "mytype", "_id" : "9" } }
{ "name" : "Kopenhagen", "loc" : [55.675706111111, 12.578745] }
{ "index" : { "_index" : "myindex", "_type" : "mytype", "_id" : "10" } }
{ "name" : "Zurich", "loc" : [47.3686498, 8.5391825] }
{ "index" : { "_index" : "myindex", "_type" : "mytype", "_id" : "11" } }
{ "name" : "Montreal", "loc" : [45.5086699, -73.5539925] }
{ "index" : { "_index" : "myindex", "_type" : "mytype", "_id" : "12" } }
{ "name" : "Trondheim" }
'
```

Query with a match_all query and add the facet
```
# curl -X POST 'http://localhost:9200/myindex/mytype/_search?pretty=1&size=0' -d '{
  "query" : { "match_all" : {} },
  "facets" : {    "countries" : {
      "georegion" : {
        "field" : "loc",
        "region" : "countries"      }    }
  }
}'

{
  "took" : 19,
  "timed_out" : false,
  "_shards" : {
    "total" : 5,
    "successful" : 5,
    "failed" : 0
  },
  "hits" : {
    "total" : 12,
    "max_score" : 1.0,
    "hits" : [ ]
  },
  "facets" : {
    "countries" : {
      "_type" : "georegion",
      "counts" : {
        "France" : "1",
        "_missing" : "1",
        "_unknown" : "1",
        "Denmark" : "1",
        "Netherlands" : "1",
        "Germany" : "3",
        "Switzerland" : "1",
        "Italy" : "2",
        "Sweden" : "1"
      }
    }
  }
}
```

In case you are wondering about `_unknown` and `_missing` - the latter one refers to the city of Trondheim as no location had been supplied on indexing, while the former one represents the city of Montreal. This is because, canada was not parsed correctly on startup (see the error messages) and thus Montreal is not included in any country.

## Think about if implementing your own facet is useful

This is just a proof of concept. You could easily implement all this, if you did not only store the location on indexing, but also the country - this would lead you to be able to execute a simple terms facet against your query.
Always think, if you really need another facet, or if you spent some more work into post- or preprocessing to solve your work more efficiently then doing this during your search. This facet will also need way more memory than a simple terms facet.

## TODO

This whole plugin is totally raw - I just wanted to check for myself, if I was able to build it and if I have understood how facetting works.

* No size limitation or something like that, results are not sorted as well
* The plugin could be more extensible. The current implementation can load all the countries of the world. However you might want to facet for other regions, for example the states in germany.
* Also the plugin should not read from a file, but rather from an index about its current locations.
* Build a useful FacetQueryBuilder
* Make it fast, the current algorithm, which checks, if a city is included in a country simply walks through all added countries - this could be vastly improved by pre-grouping the countries I guess.
* I am pretty sure, you could use already existing functionality of elasticsearch, like shapes that can already be stored and queried or filtered against - if I would have to get this production ready I would take a look, but this is intended to stay a simple afternoon hacking prototype.

## Bugs

* This implementation is not built for performance
* When loading the countries json file, canada and antarctica cannot be parsed correctly (see the error messages on startup) - not motivate to investigate

## Thanks

* I peaked at Igor Motov and his [facet scripting plugin](https://github.com/imotov/elasticsearch-facet-script) module
* The countries.geo.json file was from https://github.com/johan/world.geo.json and thus is licensed differently

## License

See LICENSE

