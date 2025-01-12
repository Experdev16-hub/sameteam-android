package com.example.sameteam.databinding;

import androidx.databinding.MergedDataBinderMapper;

public class DataBinderMapperImpl extends MergedDataBinderMapper {
  DataBinderMapperImpl() {
    addMapper(new com.example.sameteam.DataBinderMapperImpl());
  }
}
