```java
PageResultBuilder builder = new PageResultBuilder();
builder.setPageNum(Integer.max(Optional.ofNullable(ao.getPageNum()).orElse(1), 1));
builder.setPageSize(Integer.max(Optional.ofNullable(ao.getPageSize()).orElse(COMMON_PAGE_SIZE), COMMON_PAGE_SIZE));

```

