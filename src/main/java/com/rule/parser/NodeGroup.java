package com.rule.parser;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class NodeGroup {
	private List<Node> nodes;
	public static NodeGroup getInstance() {
		NodeGroup flow = new NodeGroup();
		flow.setNodes(new ArrayList<Node>());
		return flow;
	}
}
