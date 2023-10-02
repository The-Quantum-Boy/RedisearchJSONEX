package com.sumit;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sumit.model.Order;
import com.sumit.repo.OrderRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.Resource;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.search.FieldName;
import redis.clients.jedis.search.IndexDefinition;
import redis.clients.jedis.search.IndexOptions;
import redis.clients.jedis.search.Schema;

import java.util.Arrays;

@SpringBootApplication
public class Redisearch1Application implements ApplicationRunner {

	@Autowired
	private UnifiedJedis jedis;

	@Autowired
	private OrderRepo orderRepo;

	@Value("classpath:data.json")
	Resource resourceFile;

	public static void main(String[] args) {
		SpringApplication.run(Redisearch1Application.class, args);
	}


	@Override
	public void run(ApplicationArguments args) throws Exception {
		// Delete all existing orders and the index
		orderRepo.deleteAll();
		try {
			jedis.ftDropIndex("order-idx");
		} catch (Exception e) {
			System.out.println("Index is not available ");
		}

		// Read the order data from the JSON file
		String data = new String(resourceFile.getInputStream().readAllBytes());

		// Deserialize the JSON data into an array of OrderData objects
		ObjectMapper objectMapper = new ObjectMapper()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		Order[] orders = objectMapper.readValue(data, Order[].class);

		// Save the orders to the database
		Arrays.stream(orders).forEach(orderRepo::save);

		// Create the RediSearch index
		Schema schema = new Schema()
				.addField(new Schema.Field(FieldName.of("$.commodityCode").as("commodityCode"), Schema.FieldType.TEXT));

		IndexDefinition indexDefinition = new IndexDefinition(IndexDefinition.Type.JSON)
				.setPrefixes(new String[]{"order:"});

		jedis.ftCreate("order-idx",
				IndexOptions.defaultOptions().setDefinition(indexDefinition), schema);
	}
}

