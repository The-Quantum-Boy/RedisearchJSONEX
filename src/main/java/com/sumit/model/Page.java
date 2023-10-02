package com.sumit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;


@Data
@Builder
@AllArgsConstructor
public class Page {

    private List<Order> orders;

    private Integer totalPage;

    private Integer currentPage;

    private Long  total;

}
