## 怎么查看es的索引模板

要查看 Elasticsearch 的索引模板，你可以使用 Elasticsearch 的 API 来获取索引模板的信息。以下是使用 curl 命令示例：

```
shell
curl -XGET http://localhost:9200/_template
```

上述命令将返回所有索引模板的信息，包括名称、模式匹配规则、映射设置等。

如果你只想查看特定的索引模板，可以在 URL 中指定该模板的名称。例如：

```
shell
curl -XGET http://localhost:9200/_template/my-index-template
```

上述命令将返回名为 "my-index-template" 的索引模板的信息。



## mapping中`dynamic` 为 `strict`

在 Elasticsearch 的索引映射（Index Mapping）中，`dynamic` 设置为 `strict` 表示禁止动态添加新字段。

当将 `dynamic` 设置为 `strict` 时，Elasticsearch 不会自动检测和添加未定义的字段到文档中。如果尝试插入包含未定义字段的文档，Elasticsearch 将拒绝该操作并引发错误。

这种设置对于需要确保索引的字段结构不会发生变化的情况非常有用。它可以防止由于误操作或不一致的数据输入导致索引中存在不一致或不规范的字段。

以下是一个示例，展示了如何在创建索引时设置 `dynamic` 为 `strict`：

```
json
PUT /my-index
{
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "field1": {
        "type": "text"
      },
      "field2": {
        "type": "integer"
      }
    }
  }
}
```

在上述示例中，我们创建了一个名为 `my-index` 的索引，并将 `dynamic` 设置为 `strict`。同时定义了两个字段 `field1` 和 `field2`，它们的类型分别为文本和整数。

通过将 `dynamic` 设置为 `strict`，Elasticsearch 将仅接受在映射中定义的字段，并且不会允许插入未定义的字段。

请注意，一旦索引被创建并且 `dynamic` 设置为 `strict`，就无法更改该设置。如果需要允许动态添加新字段，你需要重新创建索引或使用其他映射设置。



## 聚合查询代码

```java
	public void termsAgg(){
        String aggName = "status_bucket";
        NativeSearchQueryBuilder queryBuilder =  new NativeSearchQueryBuilder();
        queryBuilder.withPageable(PageRequest.of(0,1));
        TermsAggregationBuilder termsAgg = AggregationBuilders.terms(aggName).field("status");
        queryBuilder.addAggregation(termsAgg);
        Aggregations aggregations = restTemplate.query(queryBuilder.build(), SearchResponse::getAggregations);
        Terms terms = aggregations.get(aggName);
        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        HashMap<String,Long> statusRes = new HashMap<>();
        buckets.forEach(bucket -> {
            statusRes.put(bucket.getKeyAsString(),bucket.getDocCount());
        });
        System.out.println("---聚合结果---");
        System.out.println(statusRes);
    }

```

