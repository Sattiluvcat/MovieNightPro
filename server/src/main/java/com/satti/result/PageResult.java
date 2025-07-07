package com.satti.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {
        private long total; // Total number of records
        private List<T> records; // Current page data collection
        private int page; // Current page number
        private int size; // Page size
}