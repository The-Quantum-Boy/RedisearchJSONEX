package com.sumit.repo;

import com.google.gson.Gson;
import com.sumit.model.Order;
import com.sumit.model.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
@Repository
public class OrderRepo {

    @Autowired
    private JedisPooled jedis;

    public static final Integer PAGE_SIZE = 5;

    public Order save(Order order) {
        Gson gson = new Gson();
        String key = "order:" + order.getInternalOrdNo();
        jedis.jsonSet(key, gson.toJson(order));
        jedis.sadd("order", key);
        return order;
    }

    public void deleteAll() {
        Set<String> keys = jedis.smembers("order");
        if (!keys.isEmpty()) {
            keys.stream().forEach(jedis::jsonDel);
        }
    }

    public List<Order> search(String commodityCode) {

        Long totalResult=0l;

        StringBuilder queryBuilder = new StringBuilder();

        if(commodityCode!=null && !commodityCode.isEmpty()){
            queryBuilder.append("@commodityCode:"+commodityCode);
        }



        String queryCriteria = queryBuilder.toString();

        Query query=null;

        if (queryCriteria.isEmpty()){
            query = new Query();
        }else{
            query = new Query(queryCriteria);
        }


        SearchResult searchResult = jedis.ftSearch("order-idx",query);
        totalResult = searchResult.getTotalResults();

        List<Order> orderList = searchResult.getDocuments()
                .stream()
                .map(this::converDocumentToOrder)
                .collect(Collectors.toList());

        return orderList;
    }

    private Order converDocumentToOrder(Document document){

        Gson gson = new Gson();

        String jsonDoc = document
                .getProperties()
                .iterator()
                .next()
                .getValue()
                .toString();

        return gson.fromJson(jsonDoc, Order.class);
    }
}

