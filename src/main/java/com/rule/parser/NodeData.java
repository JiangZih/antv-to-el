package com.rule.parser;

import com.alibaba.fastjson.JSONObject;

import lombok.Data;

@Data
public class NodeData {
	private JSONObject params;
	public static NodeData getInstance(JSONObject params) {
		NodeData nodeData = new NodeData();
		nodeData.setParams(params);
		return nodeData;
	}
}
