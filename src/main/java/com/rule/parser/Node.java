package com.rule.parser;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.rule.constant.NodeKind;
import lombok.Data;

@Data
public class Node {
	private String id;
	private String name;
	private NodeKind kind;
	private String compId;
	//private JSONObject comp;
	private NodeData data;
	private List<NodeGroup> groups=new ArrayList<NodeGroup>();
}
