package com.rule.bean;


import java.util.LinkedHashMap;

import com.alibaba.fastjson.JSONObject;

import lombok.Data;

@Data
public class RuleNode {
	private String id;
	private String name;
	private JSONObject params;
}
