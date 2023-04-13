package com.rule.parser;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.rule.bean.RuleNode;
import com.rule.constant.NodeKind;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class RuleParser {

    private LinkedHashMap<String, List<Node>> nodeNextMap = new LinkedHashMap<>();
    private LinkedHashMap<String, List<Node>> nodePreMap = new LinkedHashMap<>();
    private LinkedHashMap<String, Integer> flowToNodeCounter = new LinkedHashMap<>();
    private LinkedHashMap<String, NodeGroup> nodeGroupMap = new LinkedHashMap<>();
    private Flow flow = Flow.getInstance();
    private List<String> cmpDataList = new ArrayList<>();

    /**
     * 解析dag到Flow对象
     * @param config
     * @return
     */
    public Flow parseFlow(String config) {
        JSONObject configJson = JSONObject.parseObject(config);
        JSONArray cells = configJson.getJSONArray("cells");
        // 解析edge和node
        List<JSONObject> edges = new ArrayList<>();
        LinkedHashMap<String, Node> nodeMap = new LinkedHashMap<>();
        Node startNode = null;
        for (int i = 0; i < cells.size(); i++) {
            JSONObject cell = cells.getJSONObject(i);
            String id = cell.getString("id");
            String shape = cell.getString("shape");
            if (shape.equals("start")) {
                startNode = this.createNode(cell);
                nodeMap.put(id, startNode);
            } else if (shape.equals("edge")) {
                edges.add(cell);
            } else {
                nodeMap.put(id, this.createNode(cell));
            }
        }

        // 遍历edge,获取某个节点的前后续节点
        for (JSONObject edge : edges) {
            String source = edge.getJSONObject("source").getString("cell");
            String target = edge.getJSONObject("target").getString("cell");
            if (!nodeNextMap.containsKey(source)) {
                nodeNextMap.put(source, new ArrayList<>());
            }
            nodeNextMap.get(source).add(nodeMap.get(target));

            if (!nodePreMap.containsKey(target)) {
                nodePreMap.put(target, new ArrayList<>());
            }
            nodePreMap.get(target).add(nodeMap.get(source));
        }

        // 创建流程实例
        if (startNode != null)
            this.initNode(startNode, null);
        return flow;
    }
    
    /**
     * 根据Flow生成EL
     * @return
     * @throws Exception
     */
    public String genEL() throws Exception {
        StringBuilder bld = new StringBuilder("");
        String el = this.genThenEL(flow.getNodes());
        for (String cmpData : cmpDataList) {
            bld.append(cmpData);
            bld.append("\n");
        }
        bld.append(el);
        return bld.toString();
    }

    private RuleNode createRuleNode(Node node) {
        RuleNode ruleNode = new RuleNode();
        ruleNode.setId(node.getId());
        ruleNode.setName(node.getName());
        ruleNode.setParams(node.getData().getParams());
        return ruleNode;
    }

    private String genNodeEL(Node node) throws Exception {
        StringBuilder bld = new StringBuilder("");
        if (node.getData() == null || node.getData().getParams() == null) {
            bld.append(node.getCompId());
        } else {
            String cpmDataName = "cmpData" + (cmpDataList.size() + 1);
            RuleNode ruleNode = this.createRuleNode(node);
            // 规则引擎对换行符不支持
            String cpmDataStr = Base64.getEncoder().encodeToString(JSONObject.toJSONString(ruleNode).getBytes("UTF-8"));
            cmpDataList.add(cpmDataName + " = '" + cpmDataStr + "';");
            if (NodeKind.IFNODE == node.getKind()) {
                bld.append("IF(" + node.getCompId() + ".tag(\"" + node.getId() + "\")" + ".data(" + cpmDataName + ")");
                JSONObject params = node.getData().getParams();
                String trueNode = params.getJSONObject("trueNode").getString("value");
                String falseNode = params.getJSONObject("falseNode").getString("value");
                List<NodeGroup> trueNodeGroup = node.getGroups().stream().filter(group ->
                                group.getNodes().stream().filter(groupNode ->
                                        groupNode.getId().equals(trueNode)).collect(Collectors.toList()).size() > 0)
                        .collect(Collectors.toList());
                List<NodeGroup> falseNodeGroup = node.getGroups().stream().filter(group ->
                                group.getNodes().stream().filter(groupNode ->
                                        groupNode.getId().equals(falseNode)).collect(Collectors.toList()).size() > 0)
                        .collect(Collectors.toList());
                if (trueNodeGroup.size() > 0) {
                    bld.append(",");
                    for (NodeGroup g : trueNodeGroup) {
                        if (node.getGroups().indexOf(g) > 0) {
                            bld.append(",");
                        }
                        bld.append(this.genThenEL(g.getNodes()));
                    }
                }
                if (falseNodeGroup.size() > 0) {
                    for (NodeGroup g : falseNodeGroup) {
                        if (node.getGroups().indexOf(g) > 0) {
                            bld.append(",");
                        }
                        bld.append(this.genThenEL(g.getNodes()));
                    }
                }
                bld.append(")");
            } else {
                bld.append(node.getCompId() + ".tag(\"" + node.getId() + "\")" + ".data(" + cpmDataName + ")");
                if (node.getGroups().size() > 0) {
                    bld.append(",WHEN(");
                    for (NodeGroup g : node.getGroups()) {
                        if (node.getGroups().indexOf(g) > 0) {
                            bld.append(",");
                        }
                        bld.append(this.genThenEL(g.getNodes()));
                    }
                    bld.append(")");
                }
            }
        }
        return bld.toString();
    }

    private String genThenEL(List<Node> nodes) throws Exception {
        StringBuilder bld = new StringBuilder("");
        bld.append("THEN(");
        for (Node gNode : nodes) {
            String gNodeEL = this.genNodeEL(gNode);
            if (nodes.indexOf(gNode) > 0)
                bld.append(",");
            bld.append(gNodeEL);
        }
        bld.append(")");
        return bld.toString();
    }

   

    private Node createNode(JSONObject cell) {
        JSONObject data = cell.getJSONObject("data");
        String shape = cell.getString("shape");
        Node node = new Node();
        node.setId(cell.getString("id"));
        node.setCompId(data.getString("compId"));
        node.setName(data.getString("name"));
        node.setKind(NodeKind.getIns(shape));
        node.setData(NodeData.getInstance(data.getJSONObject("params")));
        return node;
    }

    private void initNode(Node node, NodeGroup parentGroup) {
        List<Node> nextNodes = nodeNextMap.get(node.getId());
        List<Node> preNodes = nodePreMap.get(node.getId());
        // 处理当前节点
        if (parentGroup == null) {
            flow.getNodes().add(node);
        } else {
            // 前驱节点为1
            if (preNodes.size() == 1)
                parentGroup.getNodes().add(node);
            else
            // 前驱节点为多个时
            {
                if (!flowToNodeCounter.containsKey(node.getId()))
                    flowToNodeCounter.put(node.getId(), 0);
                flowToNodeCounter.put(node.getId(), flowToNodeCounter.get(node.getId()).intValue() + 1);
                // 流向当前节点次数不足时，返回
                if (flowToNodeCounter.get(node.getId()).intValue() < preNodes.size()) {
                    return;
                }
                // 最后一次流向当前节点时，判断节点归属分组
                else {
                    NodeGroup realNodeGroup = getNodeGroup(node);
                    if (realNodeGroup == null)
                        flow.getNodes().add(node);
                    else
                        realNodeGroup.getNodes().add(node);
                }
            }
        }
        // 处理后续节点
        if (nextNodes == null)
            return;
        if (nextNodes.size() == 1)
            this.initNode(nextNodes.get(0), parentGroup);
        else {
            this.nodeGroupMap.put(node.getId(), parentGroup);
            for (Node nextNode : nextNodes) {
                NodeGroup group = NodeGroup.getInstance();
                node.getGroups().add(group);
                this.initNode(nextNode, group);
            }
        }
    }

    // 获得某节点逆向流向的第一个汇合点
    private NodeGroup getNodeGroup(Node node) {
        NodeGroup group = null;
        List<List<String>> flowLines = new ArrayList<>();
        List<Node> preNodes = nodePreMap.get(node.getId());
        if (preNodes == null || preNodes.size() == 0)
            return null;
        for (Node preNode : preNodes) {
            flowLines.add(this.getPreFlowLine(preNode));
        }
        String crossNodeId = this.getCrossNodeId(flowLines);
        group = this.nodeGroupMap.get(crossNodeId);
        return group;
    }

    private List<String> getPreFlowLine(Node node) {
        List<String> flowLine = new ArrayList<String>();
        flowLine.add(node.getId());
        List<Node> preNodes = this.nodePreMap.get(node.getId());
        if (preNodes == null || preNodes.size() == 0)
            return flowLine;
        for (Node preNode : preNodes) {
            flowLine.addAll(this.getPreFlowLine(preNode));
        }
        return flowLine;
    }

    private String getCrossNodeId(List<List<String>> flowLines) {
        String result = null;
        for (String item : flowLines.get(0)) {
            boolean bHasAll = true;
            for (int i = 1; i < flowLines.size(); i++) {
                bHasAll = bHasAll && flowLines.get(i).contains(item);
            }
            if (bHasAll) {
                result = item;
                break;
            }
        }
        return result;
    }
}
