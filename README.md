# Elasticsearch Georegion Facet

This facet allows you to specifiy a geo_point typed field in a facet and then get the results grouped by country (or any other geo group if you specified it in the correct format).

One of the biggest problems is, that the current geo documentation of elasticsearch is not in a good shape (huhu, wording game for insiders in this context). The problem goes further as [spatial4j][https://github.com/spatial4j/spatial4j/] and [JTS][https://github.com/spatial4j/spatial4j/] documentation looks as bad.

## Installation

```
bin/plugin -install spinscale/elasticsearch-facet-georegion
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
bin/elasticsearch -f


```



```
curl -X PUT http://localhost:9200/myindex
curl -X PUT http://localhost:9200/myindex
create index
create mapping
```

```
create 12 cities via bulk
```

```
Query with a match_all query
```

## Think about if implementing your own facet is useful

This is just a proof of concept. You could easily implement all this, if you did not only store the location on indexing, but also the country - this would lead you to be able to execute a simple terms facet against your query.
Always think, if you really need another facet, or if you spent some more work into post- or preprocessing to solve your work more efficiently then doing this during your search. This facet will also need way more memory than a simple terms facet.

## TODO

This whole plugin is totally raw - I just wanted to check for myself, if I was able to build it and if I have understood how facetting works.

* The plugin could be more extensible. The current implementation can load all the countries of the world. However you might want to facet for other regions, for example the states in germany.
* Also the plugin should not read from a file, but rather from an index about its current locations.
* Build a useful FacetQueryBuilder
* Make it fast, the current algorithm, which checks, if a city is included in a country simply walks through all added countries - this could be vastly improved by pre-grouping the countries I guess.
* I am pretty sure, you could use already existing functionality of elasticsearch, like shapes that can already be stored and queried or filtered against - if I would have to get this production ready I would take a look, but this is intended to stay a simple afternoon hacking prototype.

## Bugs

* This implementation is not built for performance
* When loading the countries json file, canada and antarctica cannot be parsed correctly (see the error messages on startup) - not motivate to investigate

## Thanks

* I peaked at Igor Motov and his [facet scripting plugin][https://github.com/imotov/elasticsearch-facet-script] module
* The countries.geo.json file was from https://github.com/johan/world.geo.json and thus is licensed differently

## License

See LICENSE

